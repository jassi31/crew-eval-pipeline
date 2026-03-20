package com.crew.evalpipeline.feedback;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.crew.evalpipeline.feedback.entity.AnnotationEntity;
import com.crew.evalpipeline.feedback.entity.AnnotatorProfileEntity;
import com.crew.evalpipeline.feedback.entity.FeedbackEntity;
import com.crew.evalpipeline.feedback.repository.AnnotatorProfileRepository;
import com.crew.evalpipeline.feedback.service.FeedbackConsensus;
import com.crew.evalpipeline.feedback.service.FeedbackConsensusService;
import com.crew.evalpipeline.shared.DomainEnums.AnnotationType;
import com.crew.evalpipeline.shared.DomainEnums.OpsQuality;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FeedbackConsensusServiceTest {

    @Mock
    private AnnotatorProfileRepository annotatorProfileRepository;

    @InjectMocks
    private FeedbackConsensusService feedbackConsensusService;

    @Test
    void shouldComputeWeightedAgreementAndHumanScore() {
        FeedbackEntity feedback = new FeedbackEntity();
        feedback.setUserRating(4);
        feedback.setOpsQuality(OpsQuality.GOOD);

        AnnotationEntity first = annotation("ann-1", "correct", 1.0);
        AnnotationEntity second = annotation("ann-2", "incorrect", 1.0);
        feedback.addAnnotation(first);
        feedback.addAnnotation(second);

        when(annotatorProfileRepository.findById("ann-1")).thenReturn(Optional.of(profile("ann-1", 1.0)));
        when(annotatorProfileRepository.findById("ann-2")).thenReturn(Optional.of(profile("ann-2", 1.0)));

        FeedbackConsensus consensus = feedbackConsensusService.summarize(feedback);

        assertThat(consensus.normalizedUserRating()).isEqualTo(0.8);
        assertThat(consensus.opsScore()).isEqualTo(0.8);
        assertThat(consensus.overallAgreement()).isEqualTo(0.5);
        assertThat(consensus.overallHumanScore()).isNotNull();
        assertThat(consensus.agreementByType()).containsKey(AnnotationType.TOOL_ACCURACY);
    }

    private AnnotationEntity annotation(String annotatorId, String label, double confidence) {
        AnnotationEntity annotation = new AnnotationEntity();
        annotation.setType(AnnotationType.TOOL_ACCURACY);
        annotation.setAnnotatorId(annotatorId);
        annotation.setLabel(label);
        annotation.setConfidence(confidence);
        return annotation;
    }

    private AnnotatorProfileEntity profile(String annotatorId, double weight) {
        AnnotatorProfileEntity profile = new AnnotatorProfileEntity();
        profile.setAnnotatorId(annotatorId);
        profile.setWeight(weight);
        profile.setActive(true);
        return profile;
    }
}
