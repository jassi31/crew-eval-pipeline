package com.crew.evalpipeline.api.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class MetaDtos {

    private MetaDtos() {
    }

    public record CalibrationReportResponse(
            Double scoreCorrelation,
            Double issuePrecision,
            Double issueRecall,
            Double coverage,
            List<String> blindSpots,
            Instant generatedAt
    ) {
    }

    public record AgreementByTypeResponse(
            String annotationType,
            Double agreement,
            Map<String, Double> labelWeights,
            Integer annotationCount
    ) {
    }

    public record AgreementReportResponse(
            Double overallAgreement,
            List<AgreementByTypeResponse> byType,
            Instant generatedAt
    ) {
    }
}
