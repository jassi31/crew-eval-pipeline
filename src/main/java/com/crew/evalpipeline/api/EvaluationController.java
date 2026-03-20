package com.crew.evalpipeline.api;

import com.crew.evalpipeline.api.dto.EvaluationDtos.EvaluationListItemResponse;
import com.crew.evalpipeline.api.dto.EvaluationDtos.EvaluationResponse;
import com.crew.evalpipeline.api.dto.EvaluationDtos.ManualRunResponse;
import com.crew.evalpipeline.evaluation.entity.EvaluationJobEntity;
import com.crew.evalpipeline.evaluation.service.EvaluationJobService;
import com.crew.evalpipeline.evaluation.service.EvaluationService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/evaluations")
public class EvaluationController {

    private final EvaluationService evaluationService;
    private final EvaluationJobService evaluationJobService;

    public EvaluationController(EvaluationService evaluationService, EvaluationJobService evaluationJobService) {
        this.evaluationService = evaluationService;
        this.evaluationJobService = evaluationJobService;
    }

    @PostMapping("/{conversationId}/run")
    public ResponseEntity<ManualRunResponse> runEvaluation(@PathVariable String conversationId) {
        EvaluationJobEntity job = evaluationJobService.enqueueByConversationId(conversationId, "MANUAL_RERUN");
        return ResponseEntity.accepted().body(new ManualRunResponse(conversationId, job.getJobId(), job.getStatus().name()));
    }

    @GetMapping("/{conversationId}")
    public EvaluationResponse getLatestEvaluation(@PathVariable String conversationId) {
        return evaluationService.getLatestEvaluation(conversationId);
    }

    @GetMapping
    public List<EvaluationListItemResponse> searchEvaluations(
            @RequestParam(required = false) String agentVersion,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String issueType,
            @RequestParam(required = false) Double minScore,
            @RequestParam(required = false) Boolean needsHumanReview
    ) {
        return evaluationService.search(agentVersion, status, issueType, minScore, needsHumanReview);
    }
}
