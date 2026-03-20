package com.crew.evalpipeline.evaluation.service;

import com.crew.evalpipeline.api.dto.EvaluationDtos.EvaluationListItemResponse;
import com.crew.evalpipeline.api.dto.EvaluationDtos.EvaluationResponse;
import com.crew.evalpipeline.api.dto.EvaluationDtos.IssueResponse;
import com.crew.evalpipeline.api.dto.EvaluationDtos.ScoreBreakdownResponse;
import com.crew.evalpipeline.api.dto.EvaluationDtos.SuggestionResponse;
import com.crew.evalpipeline.api.error.ResourceNotFoundException;
import com.crew.evalpipeline.config.AppProperties;
import com.crew.evalpipeline.conversation.entity.ConversationEntity;
import com.crew.evalpipeline.conversation.repository.ConversationRepository;
import com.crew.evalpipeline.evaluation.entity.EvaluationEntity;
import com.crew.evalpipeline.evaluation.entity.EvaluationIssueEntity;
import com.crew.evalpipeline.evaluation.model.EvaluationComponentResult;
import com.crew.evalpipeline.evaluation.model.EvaluationContext;
import com.crew.evalpipeline.evaluation.model.EvaluationIssueDraft;
import com.crew.evalpipeline.evaluation.repository.EvaluationRepository;
import com.crew.evalpipeline.feedback.service.FeedbackConsensus;
import com.crew.evalpipeline.feedback.service.FeedbackConsensusService;
import com.crew.evalpipeline.meta.service.MetaEvaluationService;
import com.crew.evalpipeline.shared.DomainEnums.EvaluationIssueType;
import com.crew.evalpipeline.shared.DomainEnums.EvaluatorType;
import com.crew.evalpipeline.shared.DomainEnums.IssueSeverity;
import com.crew.evalpipeline.shared.DomainEnums.ReviewDecision;
import com.crew.evalpipeline.suggestion.entity.ImprovementSuggestionEntity;
import com.crew.evalpipeline.suggestion.service.SuggestionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EvaluationService {

    private final ConversationRepository conversationRepository;
    private final EvaluationRepository evaluationRepository;
    private final FeedbackConsensusService feedbackConsensusService;
    private final ToolRegistryService toolRegistryService;
    private final HeuristicEvaluator heuristicEvaluator;
    private final ToolCallEvaluator toolCallEvaluator;
    private final CoherenceEvaluator coherenceEvaluator;
    private final LlmJudgeEvaluator llmJudgeEvaluator;
    private final SuggestionService suggestionService;
    private final MetaEvaluationService metaEvaluationService;
    private final AppProperties appProperties;
    private final ObjectMapper objectMapper;

    public EvaluationService(
            ConversationRepository conversationRepository,
            EvaluationRepository evaluationRepository,
            FeedbackConsensusService feedbackConsensusService,
            ToolRegistryService toolRegistryService,
            HeuristicEvaluator heuristicEvaluator,
            ToolCallEvaluator toolCallEvaluator,
            CoherenceEvaluator coherenceEvaluator,
            LlmJudgeEvaluator llmJudgeEvaluator,
            SuggestionService suggestionService,
            MetaEvaluationService metaEvaluationService,
            AppProperties appProperties,
            ObjectMapper objectMapper
    ) {
        this.conversationRepository = conversationRepository;
        this.evaluationRepository = evaluationRepository;
        this.feedbackConsensusService = feedbackConsensusService;
        this.toolRegistryService = toolRegistryService;
        this.heuristicEvaluator = heuristicEvaluator;
        this.toolCallEvaluator = toolCallEvaluator;
        this.coherenceEvaluator = coherenceEvaluator;
        this.llmJudgeEvaluator = llmJudgeEvaluator;
        this.suggestionService = suggestionService;
        this.metaEvaluationService = metaEvaluationService;
        this.appProperties = appProperties;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public EvaluationEntity evaluateConversation(String conversationId) {
        ConversationEntity conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found: " + conversationId));
        FeedbackConsensus feedbackConsensus = feedbackConsensusService.summarize(conversation.getFeedback());
        EvaluationContext context = new EvaluationContext(conversation, feedbackConsensus, toolRegistryService.getRegistry());

        List<EvaluationComponentResult> componentResults = List.of(
                llmJudgeEvaluator.evaluate(context),
                toolCallEvaluator.evaluate(context),
                coherenceEvaluator.evaluate(context),
                heuristicEvaluator.evaluate(context)
        );
        Map<EvaluatorType, EvaluationComponentResult> byType = new EnumMap<>(EvaluatorType.class);
        componentResults.forEach(result -> byType.put(result.evaluatorType(), result));

        WeightedScores weightedScores = calculateWeightedScores(byType);
        Double divergence = feedbackConsensus.overallHumanScore() == null
                ? null
                : Math.abs(weightedScores.overallScore() - feedbackConsensus.overallHumanScore());

        List<EvaluationIssueDraft> drafts = new ArrayList<>();
        componentResults.forEach(result -> drafts.addAll(result.issues()));
        if (feedbackConsensus.overallAgreement() != null
                && feedbackConsensus.overallAgreement() < appProperties.getEvaluation().getAnnotationAgreementThreshold()) {
            ObjectNode details = JsonNodeFactory.instance.objectNode();
            details.put("agreement", feedbackConsensus.overallAgreement());
            drafts.add(new EvaluationIssueDraft(
                    EvaluationIssueType.ANNOTATION_DISAGREEMENT,
                    IssueSeverity.WARNING,
                    "Human annotators disagree on the label outcome",
                    details
            ));
        }
        if (divergence != null && divergence > appProperties.getEvaluation().getDivergenceThreshold()) {
            ObjectNode details = JsonNodeFactory.instance.objectNode();
            details.put("divergence", divergence);
            details.put("humanScore", feedbackConsensus.overallHumanScore());
            details.put("automatedScore", weightedScores.overallScore());
            drafts.add(new EvaluationIssueDraft(
                    EvaluationIssueType.CALIBRATION,
                    IssueSeverity.WARNING,
                    "Automated evaluation diverges from human feedback",
                    details
            ));
        }

        boolean needsHumanReview = weightedScores.overallConfidence() < appProperties.getEvaluation().getLowConfidenceThreshold()
                || feedbackConsensus.overallAgreement() < appProperties.getEvaluation().getAnnotationAgreementThreshold()
                || (divergence != null && divergence > appProperties.getEvaluation().getDivergenceThreshold());

        EvaluationEntity evaluation = new EvaluationEntity();
        evaluation.setEvaluationId(UUID.randomUUID().toString());
        evaluation.setConversation(conversation);
        evaluation.setEvaluatorVersion(appProperties.getEvaluation().getEvaluatorVersion());
        evaluation.setOverallScore(weightedScores.overallScore());
        evaluation.setResponseQualityScore(weightedScores.responseQualityScore());
        evaluation.setToolAccuracyScore(weightedScores.toolAccuracyScore());
        evaluation.setCoherenceScore(weightedScores.coherenceScore());
        evaluation.setHeuristicScore(weightedScores.heuristicScore());
        evaluation.setOverallConfidence(weightedScores.overallConfidence());
        evaluation.setResponseQualityConfidence(weightedScores.responseQualityConfidence());
        evaluation.setToolAccuracyConfidence(weightedScores.toolAccuracyConfidence());
        evaluation.setCoherenceConfidence(weightedScores.coherenceConfidence());
        evaluation.setHeuristicConfidence(weightedScores.heuristicConfidence());
        evaluation.setEvaluatorHumanDivergence(divergence);
        evaluation.setNeedsHumanReview(needsHumanReview);
        evaluation.setReviewDecision(needsHumanReview ? ReviewDecision.HUMAN_REVIEW : ReviewDecision.AUTO_ACCEPTED);

        for (EvaluationIssueDraft draft : drafts) {
            EvaluationIssueEntity issue = new EvaluationIssueEntity();
            issue.setIssueType(draft.type());
            issue.setSeverity(draft.severity());
            issue.setDescription(draft.description());
            issue.setDetails(draft.details());
            evaluation.addIssue(issue);
        }

        EvaluationEntity saved = evaluationRepository.save(evaluation);
        suggestionService.generateSuggestions(saved);
        metaEvaluationService.refreshCalibrationSnapshot();
        return saved;
    }

    @Transactional(readOnly = true)
    public EvaluationResponse getLatestEvaluation(String conversationId) {
        EvaluationEntity evaluation = evaluationRepository.findTopByConversationConversationIdOrderByCreatedAtDesc(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Evaluation not found for conversation: " + conversationId));
        return toResponse(evaluation);
    }

    @Transactional(readOnly = true)
    public List<EvaluationListItemResponse> search(
            String agentVersion,
            String status,
            String issueType,
            Double minScore,
            Boolean needsHumanReview
    ) {
        return evaluationRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt")).stream()
                .filter(evaluation -> agentVersion == null || evaluation.getConversation().getAgentVersion().equalsIgnoreCase(agentVersion))
                .filter(evaluation -> status == null || evaluation.getConversation().getStatus().name().equalsIgnoreCase(status))
                .filter(evaluation -> issueType == null || evaluation.getIssues().stream()
                        .anyMatch(issue -> issue.getIssueType().name().equalsIgnoreCase(issueType)))
                .filter(evaluation -> minScore == null || evaluation.getOverallScore() >= minScore)
                .filter(evaluation -> needsHumanReview == null || evaluation.getNeedsHumanReview().equals(needsHumanReview))
                .map(evaluation -> new EvaluationListItemResponse(
                        evaluation.getEvaluationId(),
                        evaluation.getConversation().getConversationId(),
                        evaluation.getConversation().getAgentVersion(),
                        evaluation.getOverallScore(),
                        evaluation.getOverallConfidence(),
                        evaluation.getReviewDecision(),
                        evaluation.getNeedsHumanReview(),
                        evaluation.getCreatedAt()))
                .toList();
    }

    private EvaluationResponse toResponse(EvaluationEntity evaluation) {
        Map<String, Double> componentConfidences = new HashMap<>();
        componentConfidences.put("responseQuality", evaluation.getResponseQualityConfidence());
        componentConfidences.put("toolAccuracy", evaluation.getToolAccuracyConfidence());
        componentConfidences.put("coherence", evaluation.getCoherenceConfidence());
        componentConfidences.put("heuristics", evaluation.getHeuristicConfidence());

        List<SuggestionResponse> suggestions = suggestionService
                .relevantSuggestions(evaluation.getConversation().getConversationId(), evaluation.getConversation().getAgentVersion())
                .stream()
                .map(this::toSuggestionResponse)
                .toList();

        return new EvaluationResponse(
                evaluation.getEvaluationId(),
                evaluation.getConversation().getConversationId(),
                new ScoreBreakdownResponse(
                        evaluation.getOverallScore(),
                        evaluation.getResponseQualityScore(),
                        evaluation.getToolAccuracyScore(),
                        evaluation.getCoherenceScore(),
                        evaluation.getHeuristicScore()
                ),
                componentConfidences,
                evaluation.getReviewDecision(),
                evaluation.getNeedsHumanReview(),
                evaluation.getEvaluatorHumanDivergence(),
                evaluation.getIssues().stream()
                        .map(issue -> new IssueResponse(issue.getIssueType(), issue.getSeverity(), issue.getDescription(), issue.getDetails()))
                        .toList(),
                suggestions,
                evaluation.getCreatedAt()
        );
    }

    private SuggestionResponse toSuggestionResponse(ImprovementSuggestionEntity suggestion) {
        List<String> evidenceConversationIds = new ArrayList<>();
        suggestion.getEvidenceConversationIds().forEach(node -> evidenceConversationIds.add(node.asText()));
        return new SuggestionResponse(
                suggestion.getSuggestionId(),
                suggestion.getScope(),
                suggestion.getTargetType(),
                suggestion.getTargetKey(),
                suggestion.getSuggestion(),
                suggestion.getRationale(),
                suggestion.getConfidence(),
                suggestion.getFrequency(),
                suggestion.getExpectedImpact(),
                evidenceConversationIds,
                suggestion.getStatus(),
                suggestion.getCreatedAt()
        );
    }

    private WeightedScores calculateWeightedScores(Map<EvaluatorType, EvaluationComponentResult> byType) {
        Map<EvaluatorType, Double> weights = Map.of(
                EvaluatorType.RESPONSE_QUALITY, 0.35,
                EvaluatorType.TOOL_ACCURACY, 0.30,
                EvaluatorType.COHERENCE, 0.20,
                EvaluatorType.HEURISTICS, 0.15
        );

        double totalWeight = 0.0;
        double weightedScoreSum = 0.0;
        double confidenceSum = 0.0;
        int confidenceCount = 0;

        for (Map.Entry<EvaluatorType, Double> weight : weights.entrySet()) {
            EvaluationComponentResult result = byType.get(weight.getKey());
            if (result != null && result.applicable()) {
                totalWeight += weight.getValue();
                weightedScoreSum += result.score() * weight.getValue();
                confidenceSum += result.confidence();
                confidenceCount++;
            }
        }

        EvaluationComponentResult responseQuality = byType.getOrDefault(EvaluatorType.RESPONSE_QUALITY,
                new EvaluationComponentResult(EvaluatorType.RESPONSE_QUALITY, false, 0.0, 0.0, List.of(), Map.of()));
        EvaluationComponentResult toolAccuracy = byType.getOrDefault(EvaluatorType.TOOL_ACCURACY,
                new EvaluationComponentResult(EvaluatorType.TOOL_ACCURACY, false, 0.0, 0.0, List.of(), Map.of()));
        EvaluationComponentResult coherence = byType.getOrDefault(EvaluatorType.COHERENCE,
                new EvaluationComponentResult(EvaluatorType.COHERENCE, false, 0.0, 0.0, List.of(), Map.of()));
        EvaluationComponentResult heuristics = byType.getOrDefault(EvaluatorType.HEURISTICS,
                new EvaluationComponentResult(EvaluatorType.HEURISTICS, false, 0.0, 0.0, List.of(), Map.of()));

        return new WeightedScores(
                totalWeight == 0 ? 0.0 : weightedScoreSum / totalWeight,
                responseQuality.score(),
                toolAccuracy.score(),
                coherence.score(),
                heuristics.score(),
                confidenceCount == 0 ? 0.0 : confidenceSum / confidenceCount,
                responseQuality.confidence(),
                toolAccuracy.confidence(),
                coherence.confidence(),
                heuristics.confidence()
        );
    }

    private record WeightedScores(
            double overallScore,
            double responseQualityScore,
            double toolAccuracyScore,
            double coherenceScore,
            double heuristicScore,
            double overallConfidence,
            double responseQualityConfidence,
            double toolAccuracyConfidence,
            double coherenceConfidence,
            double heuristicConfidence
    ) {
    }
}
