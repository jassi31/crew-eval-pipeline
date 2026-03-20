package com.crew.evalpipeline.suggestion.entity;

import com.crew.evalpipeline.shared.AuditableEntity;
import com.crew.evalpipeline.shared.DomainEnums.SuggestionScope;
import com.crew.evalpipeline.shared.DomainEnums.SuggestionStatus;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "improvement_suggestions")
public class ImprovementSuggestionEntity extends AuditableEntity {

    @Id
    @Column(name = "suggestion_id", nullable = false, updatable = false)
    private String suggestionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SuggestionScope scope;

    @Column(nullable = false)
    private String targetType;

    @Column(nullable = false)
    private String targetKey;

    @Column(nullable = false)
    private String agentVersion;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String suggestion;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String rationale;

    @Column(nullable = false)
    private Double confidence;

    @Column(nullable = false)
    private Integer frequency;

    @Column(nullable = false)
    private String expectedImpact;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "evidence_conversation_ids", nullable = false)
    private JsonNode evidenceConversationIds;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SuggestionStatus status = SuggestionStatus.OPEN;

    public String getSuggestionId() {
        return suggestionId;
    }

    public void setSuggestionId(String suggestionId) {
        this.suggestionId = suggestionId;
    }

    public SuggestionScope getScope() {
        return scope;
    }

    public void setScope(SuggestionScope scope) {
        this.scope = scope;
    }

    public String getTargetType() {
        return targetType;
    }

    public void setTargetType(String targetType) {
        this.targetType = targetType;
    }

    public String getTargetKey() {
        return targetKey;
    }

    public void setTargetKey(String targetKey) {
        this.targetKey = targetKey;
    }

    public String getAgentVersion() {
        return agentVersion;
    }

    public void setAgentVersion(String agentVersion) {
        this.agentVersion = agentVersion;
    }

    public String getSuggestion() {
        return suggestion;
    }

    public void setSuggestion(String suggestion) {
        this.suggestion = suggestion;
    }

    public String getRationale() {
        return rationale;
    }

    public void setRationale(String rationale) {
        this.rationale = rationale;
    }

    public Double getConfidence() {
        return confidence;
    }

    public void setConfidence(Double confidence) {
        this.confidence = confidence;
    }

    public Integer getFrequency() {
        return frequency;
    }

    public void setFrequency(Integer frequency) {
        this.frequency = frequency;
    }

    public String getExpectedImpact() {
        return expectedImpact;
    }

    public void setExpectedImpact(String expectedImpact) {
        this.expectedImpact = expectedImpact;
    }

    public JsonNode getEvidenceConversationIds() {
        return evidenceConversationIds;
    }

    public void setEvidenceConversationIds(JsonNode evidenceConversationIds) {
        this.evidenceConversationIds = evidenceConversationIds;
    }

    public SuggestionStatus getStatus() {
        return status;
    }

    public void setStatus(SuggestionStatus status) {
        this.status = status;
    }
}
