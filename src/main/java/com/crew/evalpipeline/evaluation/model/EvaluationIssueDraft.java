package com.crew.evalpipeline.evaluation.model;

import com.crew.evalpipeline.shared.DomainEnums.EvaluationIssueType;
import com.crew.evalpipeline.shared.DomainEnums.IssueSeverity;
import com.fasterxml.jackson.databind.JsonNode;

public record EvaluationIssueDraft(
        EvaluationIssueType type,
        IssueSeverity severity,
        String description,
        JsonNode details
) {
}
