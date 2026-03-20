package com.crew.evalpipeline.suggestion.service;

import com.crew.evalpipeline.config.AppProperties;
import com.crew.evalpipeline.evaluation.entity.EvaluationEntity;
import com.crew.evalpipeline.evaluation.entity.EvaluationIssueEntity;
import com.crew.evalpipeline.evaluation.repository.EvaluationRepository;
import com.crew.evalpipeline.shared.DomainEnums.EvaluationIssueType;
import com.crew.evalpipeline.shared.DomainEnums.SuggestionScope;
import com.crew.evalpipeline.shared.DomainEnums.SuggestionStatus;
import com.crew.evalpipeline.suggestion.entity.ImprovementSuggestionEntity;
import com.crew.evalpipeline.suggestion.repository.ImprovementSuggestionRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SuggestionService {

    private final ImprovementSuggestionRepository improvementSuggestionRepository;
    private final EvaluationRepository evaluationRepository;
    private final AppProperties appProperties;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public SuggestionService(
            ImprovementSuggestionRepository improvementSuggestionRepository,
            EvaluationRepository evaluationRepository,
            AppProperties appProperties,
            ObjectMapper objectMapper,
            Clock clock
    ) {
        this.improvementSuggestionRepository = improvementSuggestionRepository;
        this.evaluationRepository = evaluationRepository;
        this.appProperties = appProperties;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Transactional
    public List<ImprovementSuggestionEntity> generateSuggestions(EvaluationEntity evaluation) {
        List<ImprovementSuggestionEntity> generated = new ArrayList<>();
        String agentVersion = evaluation.getConversation().getAgentVersion();
        String conversationId = evaluation.getConversation().getConversationId();

        for (EvaluationIssueEntity issue : evaluation.getIssues()) {
            if (issue.getIssueType() == EvaluationIssueType.TOOL_PARAMETERS) {
                String issueCategory = issue.getDetails() != null && issue.getDetails().has("issueCategory")
                        ? issue.getDetails().get("issueCategory").asText("") : "";
                String toolName = issue.getDetails() != null && issue.getDetails().has("toolName")
                        ? issue.getDetails().get("toolName").asText("unknown") : "unknown";

                if ("format".equals(issueCategory)) {
                    createSuggestionIfThresholdMet(
                            generated,
                            SuggestionScope.PROMPT,
                            "PROMPT_PATTERN",
                            agentVersion + ":" + toolName,
                            agentVersion,
                            "Add explicit parameter-format instructions for " + toolName,
                            "Repeated parameter format failures suggest the agent prompt is not constraining output enough.",
                            "Reduce invalid tool arguments and runtime failures.",
                            conversationId,
                            EvaluationIssueType.TOOL_PARAMETERS
                    );
                }

                if ("hallucinated".equals(issueCategory) || "missing".equals(issueCategory)) {
                    createSuggestionIfThresholdMet(
                            generated,
                            SuggestionScope.TOOL,
                            "TOOL_NAME",
                            toolName,
                            agentVersion,
                            "Clarify schema and validation for " + toolName,
                            "Observed missing or hallucinated parameters indicate the tool definition needs stronger validation and descriptions.",
                            "Increase parameter extraction accuracy.",
                            conversationId,
                            EvaluationIssueType.TOOL_PARAMETERS
                    );
                }
            }

            if (issue.getIssueType() == EvaluationIssueType.CALIBRATION || issue.getIssueType() == EvaluationIssueType.ANNOTATION_DISAGREEMENT) {
                createSuggestionIfThresholdMet(
                        generated,
                        SuggestionScope.EVALUATOR,
                        "EVALUATOR",
                        "evaluation_pipeline",
                        agentVersion,
                        "Recalibrate evaluator rubrics against human labels",
                        "Automated scores are diverging from human feedback or human labels disagree heavily.",
                        "Improve evaluator precision and review routing.",
                        conversationId,
                        issue.getIssueType()
                );
            }
        }
        return generated;
    }

    @Transactional(readOnly = true)
    public List<ImprovementSuggestionEntity> listSuggestions(String scope, String targetType, String agentVersion, String status) {
        return improvementSuggestionRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt")).stream()
                .filter(suggestion -> scope == null || suggestion.getScope().name().equalsIgnoreCase(scope))
                .filter(suggestion -> targetType == null || suggestion.getTargetType().equalsIgnoreCase(targetType))
                .filter(suggestion -> agentVersion == null || suggestion.getAgentVersion().equalsIgnoreCase(agentVersion))
                .filter(suggestion -> status == null || suggestion.getStatus().name().equalsIgnoreCase(status))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ImprovementSuggestionEntity> relevantSuggestions(String conversationId, String agentVersion) {
        return listSuggestions(null, null, agentVersion, SuggestionStatus.OPEN.name()).stream()
                .filter(suggestion -> extractEvidenceConversationIds(suggestion.getEvidenceConversationIds()).contains(conversationId))
                .toList();
    }

    private void createSuggestionIfThresholdMet(
            List<ImprovementSuggestionEntity> generated,
            SuggestionScope scope,
            String targetType,
            String targetKey,
            String agentVersion,
            String suggestionText,
            String rationale,
            String expectedImpact,
            String conversationId,
            EvaluationIssueType issueType
    ) {
        int frequency = countMatchingIssueFrequency(agentVersion, targetKey, issueType);
        if (frequency < appProperties.getSuggestion().getMinimumPatternFrequency()) {
            return;
        }

        ImprovementSuggestionEntity suggestion = improvementSuggestionRepository
                .findByScopeAndTargetTypeAndTargetKeyAndAgentVersionAndStatus(scope, targetType, targetKey, agentVersion, SuggestionStatus.OPEN)
                .orElseGet(() -> {
                    ImprovementSuggestionEntity entity = new ImprovementSuggestionEntity();
                    entity.setSuggestionId(UUID.randomUUID().toString());
                    entity.setScope(scope);
                    entity.setTargetType(targetType);
                    entity.setTargetKey(targetKey);
                    entity.setAgentVersion(agentVersion);
                    entity.setSuggestion(suggestionText);
                    entity.setRationale(rationale);
                    entity.setExpectedImpact(expectedImpact);
                    entity.setStatus(SuggestionStatus.OPEN);
                    entity.setEvidenceConversationIds(objectMapper.createArrayNode());
                    return entity;
                });

        Set<String> evidence = new LinkedHashSet<>(extractEvidenceConversationIds(suggestion.getEvidenceConversationIds()));
        evidence.add(conversationId);
        suggestion.setEvidenceConversationIds(toArrayNode(evidence.stream().limit(5).toList()));
        suggestion.setFrequency(Math.max(frequency, evidence.size()));
        suggestion.setConfidence(Math.min(1.0, 0.5 + (0.1 * suggestion.getFrequency())));
        suggestion.setUpdatedAt(Instant.now(clock));
        generated.add(improvementSuggestionRepository.save(suggestion));
    }

    private int countMatchingIssueFrequency(String agentVersion, String targetKey, EvaluationIssueType issueType) {
        return (int) evaluationRepository.findAll().stream()
                .filter(evaluation -> evaluation.getConversation().getAgentVersion().equals(agentVersion))
                .flatMap(evaluation -> evaluation.getIssues().stream())
                .filter(issue -> issue.getIssueType() == issueType)
                .filter(issue -> {
                    if (issue.getDetails() == null || !issue.getDetails().has("toolName")) {
                        return Objects.equals(targetKey, "evaluation_pipeline") || targetKey.contains(":");
                    }
                    String toolName = issue.getDetails().get("toolName").asText("").toLowerCase(Locale.ROOT);
                    return targetKey.toLowerCase(Locale.ROOT).contains(toolName);
                })
                .count();
    }

    private List<String> extractEvidenceConversationIds(JsonNode node) {
        List<String> values = new ArrayList<>();
        if (node != null && node.isArray()) {
            node.forEach(item -> values.add(item.asText()));
        }
        return values;
    }

    private ArrayNode toArrayNode(List<String> values) {
        ArrayNode arrayNode = objectMapper.createArrayNode();
        values.forEach(arrayNode::add);
        return arrayNode;
    }
}
