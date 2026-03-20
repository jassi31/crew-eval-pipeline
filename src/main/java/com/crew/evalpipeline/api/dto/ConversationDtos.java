package com.crew.evalpipeline.api.dto;

import com.crew.evalpipeline.shared.DomainEnums.AnnotationType;
import com.crew.evalpipeline.shared.DomainEnums.ConversationStatus;
import com.crew.evalpipeline.shared.DomainEnums.OpsQuality;
import com.crew.evalpipeline.shared.DomainEnums.ToolExecutionStatus;
import com.crew.evalpipeline.shared.DomainEnums.TurnRole;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;

public final class ConversationDtos {

    private ConversationDtos() {
    }

    public record ConversationIngestRequest(
            @NotBlank String conversationId,
            @NotBlank String agentVersion,
            @NotEmpty List<@Valid TurnRequest> turns,
            @Valid FeedbackRequest feedback,
            @Valid MetadataRequest metadata,
            String source,
            List<String> tags,
            JsonNode expectedOutcome
    ) {
    }

    public record TurnRequest(
            @NotNull Integer turnId,
            @NotNull TurnRole role,
            @NotBlank String content,
            Instant timestamp,
            List<@Valid ToolCallRequest> toolCalls
    ) {
    }

    public record ToolCallRequest(
            @NotBlank String toolName,
            @NotNull JsonNode parameters,
            JsonNode result,
            Long latencyMs,
            ToolExecutionStatus executionStatus
    ) {
    }

    public record FeedbackRequest(
            @Min(1) @Max(5) Integer userRating,
            @Valid OpsReviewRequest opsReview,
            List<@Valid AnnotationRequest> annotations
    ) {
    }

    public record FeedbackUpsertRequest(
            @Min(1) @Max(5) Integer userRating,
            @Valid OpsReviewRequest opsReview,
            List<@Valid AnnotationRequest> annotations
    ) {
    }

    public record OpsReviewRequest(
            OpsQuality quality,
            String notes
    ) {
    }

    public record AnnotationRequest(
            @NotNull AnnotationType type,
            @NotBlank String label,
            @NotBlank String annotatorId,
            @Min(0) @Max(1) Double confidence,
            JsonNode groundTruth
    ) {
    }

    public record MetadataRequest(
            Long totalLatencyMs,
            Boolean missionCompleted
    ) {
    }

    public record ConversationAcceptedResponse(
            String conversationId,
            String jobId,
            ConversationStatus status
    ) {
    }

    public record BatchIngestionResponse(
            String ingestionJobId,
            int acceptedCount,
            List<String> conversationIds
    ) {
    }

    public record ConversationResponse(
            String conversationId,
            String agentVersion,
            ConversationStatus status,
            String source,
            List<String> tags,
            JsonNode expectedOutcome,
            MetadataResponse metadata,
            FeedbackResponse feedback,
            List<TurnResponse> turns,
            EvaluationSummaryResponse latestEvaluation
    ) {
    }

    public record TurnResponse(
            String turnId,
            int turnIndex,
            TurnRole role,
            String content,
            Instant timestamp,
            List<ToolCallResponse> toolCalls
    ) {
    }

    public record ToolCallResponse(
            String toolName,
            JsonNode parameters,
            JsonNode result,
            Long latencyMs,
            ToolExecutionStatus executionStatus
    ) {
    }

    public record FeedbackResponse(
            Integer userRating,
            OpsReviewResponse opsReview,
            List<AnnotationResponse> annotations
    ) {
    }

    public record OpsReviewResponse(
            OpsQuality quality,
            String notes
    ) {
    }

    public record AnnotationResponse(
            AnnotationType type,
            String label,
            String annotatorId,
            Double confidence,
            JsonNode groundTruth
    ) {
    }

    public record MetadataResponse(
            Long totalLatencyMs,
            Boolean missionCompleted
    ) {
    }

    public record EvaluationSummaryResponse(
            String evaluationId,
            Double overallScore,
            Double overallConfidence,
            Boolean needsHumanReview
    ) {
    }
}
