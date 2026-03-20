package com.crew.evalpipeline.api;

import com.crew.evalpipeline.api.dto.MetaDtos.AgreementReportResponse;
import com.crew.evalpipeline.api.dto.MetaDtos.CalibrationReportResponse;
import com.crew.evalpipeline.meta.service.MetaEvaluationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/meta")
public class MetaController {

    private final MetaEvaluationService metaEvaluationService;

    public MetaController(MetaEvaluationService metaEvaluationService) {
        this.metaEvaluationService = metaEvaluationService;
    }

    @GetMapping("/calibration")
    public CalibrationReportResponse getCalibration() {
        return metaEvaluationService.getCalibrationReport();
    }

    @GetMapping("/agreements")
    public AgreementReportResponse getAgreements() {
        return metaEvaluationService.getAgreementReport();
    }
}
