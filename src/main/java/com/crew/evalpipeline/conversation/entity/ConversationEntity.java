package com.crew.evalpipeline.conversation.entity;

import com.crew.evalpipeline.evaluation.entity.EvaluationEntity;
import com.crew.evalpipeline.evaluation.entity.EvaluationJobEntity;
import com.crew.evalpipeline.feedback.entity.FeedbackEntity;
import com.crew.evalpipeline.shared.AuditableEntity;
import com.crew.evalpipeline.shared.DomainEnums.ConversationStatus;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "conversations")
public class ConversationEntity extends AuditableEntity {

    @Id
    @Column(name = "conversation_id", nullable = false, updatable = false)
    private String conversationId;

    @Column(nullable = false)
    private String agentVersion;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_payload", nullable = false)
    private JsonNode rawPayload;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "expected_outcome")
    private JsonNode expectedOutcome;

    private String source;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ConversationStatus status = ConversationStatus.QUEUED;

    @Column(nullable = false)
    private Long totalLatencyMs = 0L;

    @Column(nullable = false)
    private Boolean missionCompleted = Boolean.FALSE;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "conversation_tags", joinColumns = @JoinColumn(name = "conversation_id"))
    @Column(name = "tag")
    private List<String> tags = new ArrayList<>();

    @OneToMany(mappedBy = "conversation", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TurnEntity> turns = new ArrayList<>();

    @OneToOne(mappedBy = "conversation", cascade = CascadeType.ALL, orphanRemoval = true)
    private FeedbackEntity feedback;

    @OneToMany(mappedBy = "conversation", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EvaluationJobEntity> jobs = new ArrayList<>();

    @OneToMany(mappedBy = "conversation", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EvaluationEntity> evaluations = new ArrayList<>();

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public String getAgentVersion() {
        return agentVersion;
    }

    public void setAgentVersion(String agentVersion) {
        this.agentVersion = agentVersion;
    }

    public JsonNode getRawPayload() {
        return rawPayload;
    }

    public void setRawPayload(JsonNode rawPayload) {
        this.rawPayload = rawPayload;
    }

    public JsonNode getExpectedOutcome() {
        return expectedOutcome;
    }

    public void setExpectedOutcome(JsonNode expectedOutcome) {
        this.expectedOutcome = expectedOutcome;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public ConversationStatus getStatus() {
        return status;
    }

    public void setStatus(ConversationStatus status) {
        this.status = status;
    }

    public Long getTotalLatencyMs() {
        return totalLatencyMs;
    }

    public void setTotalLatencyMs(Long totalLatencyMs) {
        this.totalLatencyMs = totalLatencyMs;
    }

    public Boolean getMissionCompleted() {
        return missionCompleted;
    }

    public void setMissionCompleted(Boolean missionCompleted) {
        this.missionCompleted = missionCompleted;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public List<TurnEntity> getTurns() {
        return turns;
    }

    public void setTurns(List<TurnEntity> turns) {
        this.turns = turns;
    }

    public void addTurn(TurnEntity turn) {
        turns.add(turn);
        turn.setConversation(this);
    }

    public FeedbackEntity getFeedback() {
        return feedback;
    }

    public void setFeedback(FeedbackEntity feedback) {
        this.feedback = feedback;
        if (feedback != null) {
            feedback.setConversation(this);
        }
    }

    public List<EvaluationJobEntity> getJobs() {
        return jobs;
    }

    public List<EvaluationEntity> getEvaluations() {
        return evaluations;
    }
}
