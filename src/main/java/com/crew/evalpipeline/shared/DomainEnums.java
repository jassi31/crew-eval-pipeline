package com.crew.evalpipeline.shared;

public final class DomainEnums {

    private DomainEnums() {
    }

    public enum ConversationStatus {
        QUEUED,
        PROCESSING,
        EVALUATED,
        FAILED
    }

    public enum EvaluationJobStatus {
        QUEUED,
        IN_PROGRESS,
        COMPLETED,
        FAILED
    }

    public enum EvaluationIssueType {
        LATENCY,
        TOOL_SELECTION,
        TOOL_PARAMETERS,
        TOOL_EXECUTION,
        COHERENCE,
        FORMAT,
        CALIBRATION,
        ANNOTATION_DISAGREEMENT,
        BLIND_SPOT
    }

    public enum IssueSeverity {
        INFO,
        WARNING,
        ERROR
    }

    public enum ReviewDecision {
        AUTO_ACCEPTED,
        HUMAN_REVIEW
    }

    public enum SuggestionScope {
        PROMPT,
        TOOL,
        EVALUATOR
    }

    public enum SuggestionStatus {
        OPEN,
        ACCEPTED,
        DISMISSED
    }

    public enum OpsQuality {
        POOR,
        AVERAGE,
        GOOD,
        EXCELLENT
    }

    public enum ToolExecutionStatus {
        SUCCESS,
        FAILURE,
        PARTIAL,
        UNKNOWN
    }

    public enum TurnRole {
        USER,
        ASSISTANT,
        SYSTEM,
        TOOL
    }

    public enum AnnotationType {
        TOOL_ACCURACY,
        RESPONSE_HELPFULNESS,
        COHERENCE,
        FACTUALITY,
        OTHER
    }

    public enum EvaluatorType {
        RESPONSE_QUALITY,
        TOOL_ACCURACY,
        COHERENCE,
        HEURISTICS
    }
}
