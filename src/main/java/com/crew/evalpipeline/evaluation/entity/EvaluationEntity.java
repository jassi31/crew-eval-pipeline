package com.crew.evalpipeline.evaluation.entity;

import com.crew.evalpipeline.conversation.entity.ConversationEntity;
import com.crew.evalpipeline.shared.AuditableEntity;
import com.crew.evalpipeline.shared.DomainEnums.ReviewDecision;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "evaluations")
public class EvaluationEntity extends AuditableEntity {

    @Id
    @Column(name = "evaluation_id", nullable = false, updatable = false)
    private String evaluationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    private ConversationEntity conversation;

    @Column(nullable = false)
    private String evaluatorVersion;

    @Column(nullable = false)
    private Double overallScore;

    @Column(nullable = false)
    private Double responseQualityScore;

    @Column(nullable = false)
    private Double toolAccuracyScore;

    @Column(nullable = false)
    private Double coherenceScore;

    @Column(nullable = false)
    private Double heuristicScore;

    @Column(nullable = false)
    private Double overallConfidence;

    @Column(nullable = false)
    private Double responseQualityConfidence;

    @Column(nullable = false)
    private Double toolAccuracyConfidence;

    @Column(nullable = false)
    private Double coherenceConfidence;

    @Column(nullable = false)
    private Double heuristicConfidence;

    private Double evaluatorHumanDivergence;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReviewDecision reviewDecision;

    @Column(nullable = false)
    private Boolean needsHumanReview;

    @OneToMany(mappedBy = "evaluation", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EvaluationIssueEntity> issues = new ArrayList<>();

    public String getEvaluationId() {
        return evaluationId;
    }

    public void setEvaluationId(String evaluationId) {
        this.evaluationId = evaluationId;
    }

    public ConversationEntity getConversation() {
        return conversation;
    }

    public void setConversation(ConversationEntity conversation) {
        this.conversation = conversation;
    }

    public String getEvaluatorVersion() {
        return evaluatorVersion;
    }

    public void setEvaluatorVersion(String evaluatorVersion) {
        this.evaluatorVersion = evaluatorVersion;
    }

    public Double getOverallScore() {
        return overallScore;
    }

    public void setOverallScore(Double overallScore) {
        this.overallScore = overallScore;
    }

    public Double getResponseQualityScore() {
        return responseQualityScore;
    }

    public void setResponseQualityScore(Double responseQualityScore) {
        this.responseQualityScore = responseQualityScore;
    }

    public Double getToolAccuracyScore() {
        return toolAccuracyScore;
    }

    public void setToolAccuracyScore(Double toolAccuracyScore) {
        this.toolAccuracyScore = toolAccuracyScore;
    }

    public Double getCoherenceScore() {
        return coherenceScore;
    }

    public void setCoherenceScore(Double coherenceScore) {
        this.coherenceScore = coherenceScore;
    }

    public Double getHeuristicScore() {
        return heuristicScore;
    }

    public void setHeuristicScore(Double heuristicScore) {
        this.heuristicScore = heuristicScore;
    }

    public Double getOverallConfidence() {
        return overallConfidence;
    }

    public void setOverallConfidence(Double overallConfidence) {
        this.overallConfidence = overallConfidence;
    }

    public Double getResponseQualityConfidence() {
        return responseQualityConfidence;
    }

    public void setResponseQualityConfidence(Double responseQualityConfidence) {
        this.responseQualityConfidence = responseQualityConfidence;
    }

    public Double getToolAccuracyConfidence() {
        return toolAccuracyConfidence;
    }

    public void setToolAccuracyConfidence(Double toolAccuracyConfidence) {
        this.toolAccuracyConfidence = toolAccuracyConfidence;
    }

    public Double getCoherenceConfidence() {
        return coherenceConfidence;
    }

    public void setCoherenceConfidence(Double coherenceConfidence) {
        this.coherenceConfidence = coherenceConfidence;
    }

    public Double getHeuristicConfidence() {
        return heuristicConfidence;
    }

    public void setHeuristicConfidence(Double heuristicConfidence) {
        this.heuristicConfidence = heuristicConfidence;
    }

    public Double getEvaluatorHumanDivergence() {
        return evaluatorHumanDivergence;
    }

    public void setEvaluatorHumanDivergence(Double evaluatorHumanDivergence) {
        this.evaluatorHumanDivergence = evaluatorHumanDivergence;
    }

    public ReviewDecision getReviewDecision() {
        return reviewDecision;
    }

    public void setReviewDecision(ReviewDecision reviewDecision) {
        this.reviewDecision = reviewDecision;
    }

    public Boolean getNeedsHumanReview() {
        return needsHumanReview;
    }

    public void setNeedsHumanReview(Boolean needsHumanReview) {
        this.needsHumanReview = needsHumanReview;
    }

    public List<EvaluationIssueEntity> getIssues() {
        return issues;
    }

    public void addIssue(EvaluationIssueEntity issue) {
        issues.add(issue);
        issue.setEvaluation(this);
    }
}
