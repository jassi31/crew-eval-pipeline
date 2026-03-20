package com.crew.evalpipeline.feedback.service;

import com.crew.evalpipeline.feedback.entity.AnnotationEntity;
import com.crew.evalpipeline.feedback.entity.AnnotatorProfileEntity;
import com.crew.evalpipeline.feedback.entity.FeedbackEntity;
import com.crew.evalpipeline.feedback.repository.AnnotatorProfileRepository;
import com.crew.evalpipeline.shared.DomainEnums.AnnotationType;
import com.crew.evalpipeline.shared.DomainEnums.OpsQuality;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class FeedbackConsensusService {

    private final AnnotatorProfileRepository annotatorProfileRepository;

    public FeedbackConsensusService(AnnotatorProfileRepository annotatorProfileRepository) {
        this.annotatorProfileRepository = annotatorProfileRepository;
    }

    public FeedbackConsensus summarize(FeedbackEntity feedback) {
        if (feedback == null) {
            return new FeedbackConsensus(null, null, null, null, 1.0, Collections.emptyMap(), Collections.emptyMap());
        }

        Double userScore = feedback.getUserRating() == null ? null : feedback.getUserRating() / 5.0;
        Double opsScore = mapOpsQuality(feedback.getOpsQuality());
        Map<AnnotationType, Double> agreementByType = new EnumMap<>(AnnotationType.class);
        Map<AnnotationType, Map<String, Double>> labelWeightsByType = new EnumMap<>(AnnotationType.class);

        feedback.getAnnotations().stream()
                .collect(java.util.stream.Collectors.groupingBy(AnnotationEntity::getType))
                .forEach((type, annotations) -> {
                    Map<String, Double> weightedLabels = new HashMap<>();
                    double totalWeight = 0.0;
                    for (AnnotationEntity annotation : annotations) {
                        double weight = resolveAnnotatorWeight(annotation.getAnnotatorId()) * Optional.ofNullable(annotation.getConfidence()).orElse(1.0);
                        weightedLabels.merge(annotation.getLabel().toLowerCase(), weight, Double::sum);
                        totalWeight += weight;
                    }

                    double topWeight = weightedLabels.values().stream().max(Comparator.naturalOrder()).orElse(0.0);
                    double agreement = totalWeight == 0 ? 1.0 : topWeight / totalWeight;
                    agreementByType.put(type, agreement);
                    labelWeightsByType.put(type, weightedLabels);
                });

        Double annotationConsensusScore = computeAnnotationConsensusScore(labelWeightsByType);
        Double overallHumanScore = averageNonNull(userScore, opsScore, annotationConsensusScore);
        double overallAgreement = agreementByType.values().stream().mapToDouble(Double::doubleValue).average().orElse(1.0);

        return new FeedbackConsensus(
                userScore,
                opsScore,
                annotationConsensusScore,
                overallHumanScore,
                overallAgreement,
                agreementByType,
                labelWeightsByType
        );
    }

    private double resolveAnnotatorWeight(String annotatorId) {
        return annotatorProfileRepository.findById(annotatorId)
                .filter(AnnotatorProfileEntity::getActive)
                .map(AnnotatorProfileEntity::getWeight)
                .orElse(1.0);
    }

    private Double mapOpsQuality(OpsQuality opsQuality) {
        if (opsQuality == null) {
            return null;
        }
        return switch (opsQuality) {
            case POOR -> 0.2;
            case AVERAGE -> 0.5;
            case GOOD -> 0.8;
            case EXCELLENT -> 1.0;
        };
    }

    private Double computeAnnotationConsensusScore(Map<AnnotationType, Map<String, Double>> labelWeightsByType) {
        List<Double> scores = labelWeightsByType.values().stream()
                .map(this::labelWeightsToScore)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();
        if (scores.isEmpty()) {
            return null;
        }
        return scores.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
    }

    private Optional<Double> labelWeightsToScore(Map<String, Double> labelWeights) {
        if (labelWeights.isEmpty()) {
            return Optional.empty();
        }
        String winningLabel = labelWeights.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
        if (winningLabel == null) {
            return Optional.empty();
        }
        if (winningLabel.contains("correct") || winningLabel.contains("good") || winningLabel.contains("helpful")) {
            return Optional.of(1.0);
        }
        if (winningLabel.contains("partial") || winningLabel.contains("average")) {
            return Optional.of(0.5);
        }
        if (winningLabel.contains("incorrect") || winningLabel.contains("poor") || winningLabel.contains("bad")) {
            return Optional.of(0.0);
        }
        return Optional.of(0.5);
    }

    private Double averageNonNull(Double... values) {
        List<Double> present = java.util.Arrays.stream(values).filter(java.util.Objects::nonNull).toList();
        if (present.isEmpty()) {
            return null;
        }
        return present.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
    }
}
