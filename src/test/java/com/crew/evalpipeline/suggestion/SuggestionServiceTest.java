package com.crew.evalpipeline.suggestion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.crew.evalpipeline.config.AppProperties;
import com.crew.evalpipeline.conversation.entity.ConversationEntity;
import com.crew.evalpipeline.evaluation.entity.EvaluationEntity;
import com.crew.evalpipeline.evaluation.entity.EvaluationIssueEntity;
import com.crew.evalpipeline.evaluation.repository.EvaluationRepository;
import com.crew.evalpipeline.shared.DomainEnums.EvaluationIssueType;
import com.crew.evalpipeline.shared.DomainEnums.IssueSeverity;
import com.crew.evalpipeline.shared.DomainEnums.SuggestionScope;
import com.crew.evalpipeline.suggestion.entity.ImprovementSuggestionEntity;
import com.crew.evalpipeline.suggestion.repository.ImprovementSuggestionRepository;
import com.crew.evalpipeline.suggestion.service.SuggestionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SuggestionServiceTest {

    @Mock
    private ImprovementSuggestionRepository improvementSuggestionRepository;

    @Mock
    private EvaluationRepository evaluationRepository;

    @Test
    void shouldGeneratePromptSuggestionAfterRepeatedFormatFailures() {
        AppProperties properties = new AppProperties();
        properties.getSuggestion().setMinimumPatternFrequency(2);
        SuggestionService suggestionService = new SuggestionService(
                improvementSuggestionRepository,
                evaluationRepository,
                properties,
                new ObjectMapper(),
                Clock.fixed(Instant.parse("2026-03-20T00:00:00Z"), ZoneOffset.UTC)
        );

        EvaluationEntity current = evaluation("conv-a", "v1");
        current.addIssue(formatIssue("flight_search"));
        EvaluationEntity prior = evaluation("conv-b", "v1");
        prior.addIssue(formatIssue("flight_search"));

        when(evaluationRepository.findAll()).thenReturn(List.of(current, prior));
        when(improvementSuggestionRepository.findByScopeAndTargetTypeAndTargetKeyAndAgentVersionAndStatus(
                SuggestionScope.PROMPT,
                "PROMPT_PATTERN",
                "v1:flight_search",
                "v1",
                com.crew.evalpipeline.shared.DomainEnums.SuggestionStatus.OPEN
        )).thenReturn(Optional.empty());
        when(improvementSuggestionRepository.save(any(ImprovementSuggestionEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        List<ImprovementSuggestionEntity> suggestions = suggestionService.generateSuggestions(current);

        assertThat(suggestions).hasSize(1);
        assertThat(suggestions.get(0).getScope()).isEqualTo(SuggestionScope.PROMPT);
        assertThat(suggestions.get(0).getFrequency()).isGreaterThanOrEqualTo(2);
    }

    private EvaluationEntity evaluation(String conversationId, String agentVersion) {
        ConversationEntity conversation = new ConversationEntity();
        conversation.setConversationId(conversationId);
        conversation.setAgentVersion(agentVersion);

        EvaluationEntity evaluation = new EvaluationEntity();
        evaluation.setEvaluationId("eval-" + conversationId);
        evaluation.setConversation(conversation);
        evaluation.setOverallScore(0.5);
        evaluation.setOverallConfidence(0.6);
        evaluation.setResponseQualityScore(0.5);
        evaluation.setToolAccuracyScore(0.5);
        evaluation.setCoherenceScore(0.5);
        evaluation.setHeuristicScore(0.5);
        evaluation.setResponseQualityConfidence(0.5);
        evaluation.setToolAccuracyConfidence(0.5);
        evaluation.setCoherenceConfidence(0.5);
        evaluation.setHeuristicConfidence(0.5);
        evaluation.setNeedsHumanReview(false);
        evaluation.setReviewDecision(com.crew.evalpipeline.shared.DomainEnums.ReviewDecision.AUTO_ACCEPTED);
        evaluation.setEvaluatorVersion("v1");
        return evaluation;
    }

    private EvaluationIssueEntity formatIssue(String toolName) {
        EvaluationIssueEntity issue = new EvaluationIssueEntity();
        issue.setIssueType(EvaluationIssueType.TOOL_PARAMETERS);
        issue.setSeverity(IssueSeverity.WARNING);
        issue.setDescription("Parameter format validation failed for date_range");
        issue.setDetails(JsonNodeFactory.instance.objectNode()
                .put("toolName", toolName)
                .put("issueCategory", "format"));
        return issue;
    }
}
