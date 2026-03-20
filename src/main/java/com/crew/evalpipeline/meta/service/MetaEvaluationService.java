package com.crew.evalpipeline.meta.service;

import com.crew.evalpipeline.api.dto.MetaDtos.AgreementByTypeResponse;
import com.crew.evalpipeline.api.dto.MetaDtos.AgreementReportResponse;
import com.crew.evalpipeline.api.dto.MetaDtos.CalibrationReportResponse;
import com.crew.evalpipeline.conversation.entity.ConversationEntity;
import com.crew.evalpipeline.conversation.repository.ConversationRepository;
import com.crew.evalpipeline.evaluation.entity.EvaluationEntity;
import com.crew.evalpipeline.evaluation.entity.EvaluationIssueEntity;
import com.crew.evalpipeline.evaluation.repository.EvaluationRepository;
import com.crew.evalpipeline.feedback.entity.AnnotationEntity;
import com.crew.evalpipeline.feedback.service.FeedbackConsensus;
import com.crew.evalpipeline.feedback.service.FeedbackConsensusService;
import com.crew.evalpipeline.meta.entity.CalibrationSnapshotEntity;
import com.crew.evalpipeline.meta.repository.CalibrationSnapshotRepository;
import com.crew.evalpipeline.shared.DomainEnums.AnnotationType;
import com.crew.evalpipeline.shared.DomainEnums.EvaluationIssueType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MetaEvaluationService {

    private final EvaluationRepository evaluationRepository;
    private final CalibrationSnapshotRepository calibrationSnapshotRepository;
    private final ConversationRepository conversationRepository;
    private final FeedbackConsensusService feedbackConsensusService;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public MetaEvaluationService(
            EvaluationRepository evaluationRepository,
            CalibrationSnapshotRepository calibrationSnapshotRepository,
            ConversationRepository conversationRepository,
            FeedbackConsensusService feedbackConsensusService,
            ObjectMapper objectMapper,
            Clock clock
    ) {
        this.evaluationRepository = evaluationRepository;
        this.calibrationSnapshotRepository = calibrationSnapshotRepository;
        this.conversationRepository = conversationRepository;
        this.feedbackConsensusService = feedbackConsensusService;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Transactional
    public CalibrationSnapshotEntity refreshCalibrationSnapshot() {
        List<EvaluationEntity> evaluations = evaluationRepository.findAll();
        List<Double> automatedScores = new ArrayList<>();
        List<Double> humanScores = new ArrayList<>();
        Set<String> blindSpots = new LinkedHashSet<>();
        int tp = 0;
        int fp = 0;
        int fn = 0;

        for (EvaluationEntity evaluation : evaluations) {
            FeedbackConsensus consensus = feedbackConsensusService.summarize(evaluation.getConversation().getFeedback());
            if (consensus.overallHumanScore() != null) {
                automatedScores.add(evaluation.getOverallScore());
                humanScores.add(consensus.overallHumanScore());
            }

            for (AnnotationEntity annotation : evaluation.getConversation().getFeedback() == null
                    ? List.<AnnotationEntity>of()
                    : evaluation.getConversation().getFeedback().getAnnotations()) {
                boolean expectedIssue = isNegativeLabel(annotation.getLabel());
                boolean predictedIssue = hasIssueForAnnotationType(evaluation.getIssues(), annotation.getType(), evaluation);
                if (expectedIssue && predictedIssue) {
                    tp++;
                } else if (!expectedIssue && predictedIssue) {
                    fp++;
                } else if (expectedIssue) {
                    fn++;
                    blindSpots.add(annotation.getType().name());
                }
            }
        }

        double precision = tp + fp == 0 ? 1.0 : tp / (double) (tp + fp);
        double recall = tp + fn == 0 ? 1.0 : tp / (double) (tp + fn);
        double coverage = evaluations.isEmpty() ? 1.0 : humanScores.size() / (double) evaluations.size();
        double scoreCorrelation = pearsonCorrelation(automatedScores, humanScores);

        CalibrationSnapshotEntity snapshot = new CalibrationSnapshotEntity();
        snapshot.setScoreCorrelation(scoreCorrelation);
        snapshot.setIssuePrecision(precision);
        snapshot.setIssueRecall(recall);
        snapshot.setCoverage(coverage);
        snapshot.setBlindSpots(toArrayNode(new ArrayList<>(blindSpots)));
        snapshot.setCreatedAt(Instant.now(clock));
        snapshot.setUpdatedAt(Instant.now(clock));
        return calibrationSnapshotRepository.save(snapshot);
    }

    @Transactional(readOnly = true)
    public CalibrationReportResponse getCalibrationReport() {
        CalibrationSnapshotEntity snapshot = calibrationSnapshotRepository.findTopByOrderByCreatedAtDesc()
                .orElseGet(this::refreshCalibrationSnapshot);
        List<String> blindSpots = new ArrayList<>();
        snapshot.getBlindSpots().forEach(node -> blindSpots.add(node.asText()));
        return new CalibrationReportResponse(
                snapshot.getScoreCorrelation(),
                snapshot.getIssuePrecision(),
                snapshot.getIssueRecall(),
                snapshot.getCoverage(),
                blindSpots,
                snapshot.getCreatedAt()
        );
    }

    @Transactional(readOnly = true)
    public AgreementReportResponse getAgreementReport() {
        Map<AnnotationType, List<Double>> agreements = new EnumMap<>(AnnotationType.class);
        Map<AnnotationType, Map<String, Double>> labelWeights = new EnumMap<>(AnnotationType.class);

        for (ConversationEntity conversation : conversationRepository.findAll()) {
            FeedbackConsensus consensus = feedbackConsensusService.summarize(
                    conversationRepository.findById(conversation.getConversationId()).orElse(conversation).getFeedback());
            consensus.agreementByType().forEach((type, agreement) ->
                    agreements.computeIfAbsent(type, ignored -> new ArrayList<>()).add(agreement));

            consensus.labelWeightsByType().forEach((type, labels) -> {
                Map<String, Double> aggregate = labelWeights.computeIfAbsent(type, ignored -> new HashMap<>());
                labels.forEach((label, weight) -> aggregate.merge(label, weight, Double::sum));
            });
        }

        List<AgreementByTypeResponse> byType = agreements.entrySet().stream()
                .map(entry -> new AgreementByTypeResponse(
                        entry.getKey().name(),
                        entry.getValue().stream().mapToDouble(Double::doubleValue).average().orElse(1.0),
                        labelWeights.getOrDefault(entry.getKey(), Map.of()),
                        labelWeights.getOrDefault(entry.getKey(), Map.of()).values().stream().mapToInt(value -> 1).sum()
                ))
                .toList();

        double overallAgreement = byType.stream().mapToDouble(AgreementByTypeResponse::agreement).average().orElse(1.0);
        return new AgreementReportResponse(overallAgreement, byType, Instant.now(clock));
    }

    private boolean hasIssueForAnnotationType(List<EvaluationIssueEntity> issues, AnnotationType type, EvaluationEntity evaluation) {
        return switch (type) {
            case TOOL_ACCURACY -> issues.stream().anyMatch(issue ->
                    issue.getIssueType() == EvaluationIssueType.TOOL_SELECTION
                            || issue.getIssueType() == EvaluationIssueType.TOOL_PARAMETERS
                            || issue.getIssueType() == EvaluationIssueType.TOOL_EXECUTION);
            case COHERENCE -> issues.stream().anyMatch(issue -> issue.getIssueType() == EvaluationIssueType.COHERENCE);
            case RESPONSE_HELPFULNESS, FACTUALITY -> evaluation.getOverallScore() < 0.5 || !issues.isEmpty();
            case OTHER -> !issues.isEmpty();
        };
    }

    private boolean isNegativeLabel(String label) {
        String normalized = label == null ? "" : label.toLowerCase();
        return normalized.contains("incorrect")
                || normalized.contains("poor")
                || normalized.contains("bad")
                || normalized.contains("fail");
    }

    private ArrayNode toArrayNode(List<String> values) {
        ArrayNode arrayNode = objectMapper.createArrayNode();
        values.forEach(arrayNode::add);
        return arrayNode;
    }

    private double pearsonCorrelation(List<Double> xs, List<Double> ys) {
        if (xs.size() < 2 || ys.size() < 2 || xs.size() != ys.size()) {
            return 1.0;
        }
        double meanX = xs.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double meanY = ys.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double numerator = 0.0;
        double denominatorX = 0.0;
        double denominatorY = 0.0;
        for (int index = 0; index < xs.size(); index++) {
            double deltaX = xs.get(index) - meanX;
            double deltaY = ys.get(index) - meanY;
            numerator += deltaX * deltaY;
            denominatorX += deltaX * deltaX;
            denominatorY += deltaY * deltaY;
        }
        if (denominatorX == 0 || denominatorY == 0) {
            return 1.0;
        }
        return numerator / Math.sqrt(denominatorX * denominatorY);
    }
}
