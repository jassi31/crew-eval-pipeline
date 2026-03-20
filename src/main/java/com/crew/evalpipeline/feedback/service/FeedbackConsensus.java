package com.crew.evalpipeline.feedback.service;

import com.crew.evalpipeline.shared.DomainEnums.AnnotationType;
import java.util.Map;

public record FeedbackConsensus(
        Double normalizedUserRating,
        Double opsScore,
        Double annotationConsensusScore,
        Double overallHumanScore,
        Double overallAgreement,
        Map<AnnotationType, Double> agreementByType,
        Map<AnnotationType, Map<String, Double>> labelWeightsByType
) {
}
