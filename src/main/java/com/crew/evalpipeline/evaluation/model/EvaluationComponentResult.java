package com.crew.evalpipeline.evaluation.model;

import com.crew.evalpipeline.shared.DomainEnums.EvaluatorType;
import java.util.List;
import java.util.Map;

public record EvaluationComponentResult(
        EvaluatorType evaluatorType,
        boolean applicable,
        double score,
        double confidence,
        List<EvaluationIssueDraft> issues,
        Map<String, Double> metrics
) {
}
