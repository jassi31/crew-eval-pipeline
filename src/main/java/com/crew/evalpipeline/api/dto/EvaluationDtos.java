package com.crew.evalpipeline.api.dto;

import com.crew.evalpipeline.shared.DomainEnums.EvaluationIssueType;
import com.crew.evalpipeline.shared.DomainEnums.IssueSeverity;
import com.crew.evalpipeline.shared.DomainEnums.ReviewDecision;
import com.crew.evalpipeline.shared.DomainEnums.SuggestionScope;
import com.crew.evalpipeline.shared.DomainEnums.SuggestionStatus;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class EvaluationDtos {

    private EvaluationDtos() {
    }

    public record ManualRunResponse(
            String conversationId,
            String jobId,
            String status
    ) {
    }

    public record EvaluationResponse(
            String evaluationId,
            String conversationId,
            ScoreBreakdownResponse scores,
            Map<String, Double> componentConfidences,
            ReviewDecision reviewDecision,
            Boolean needsHumanReview,
            Double evaluatorHumanDivergence,
            List<IssueResponse> issuesDetected,
            List<SuggestionResponse> improvementSuggestions,
            Instant createdAt
    ) {
    }

    public record ScoreBreakdownResponse(
            Double overall,
            Double responseQuality,
            Double toolAccuracy,
            Double coherence,
            Double heuristics
    ) {
    }

    public record IssueResponse(
            EvaluationIssueType type,
            IssueSeverity severity,
            String description,
            JsonNode details
    ) {
    }

    public record SuggestionResponse(
            String suggestionId,
            SuggestionScope type,
            String targetType,
            String target,
            String suggestion,
            String rationale,
            Double confidence,
            Integer frequency,
            String expectedImpact,
            List<String> evidenceConversationIds,
            SuggestionStatus status,
            Instant createdAt
    ) {
    }

    public record EvaluationListItemResponse(
            String evaluationId,
            String conversationId,
            String agentVersion,
            Double overallScore,
            Double overallConfidence,
            ReviewDecision reviewDecision,
            Boolean needsHumanReview,
            Instant createdAt
    ) {
    }
}
