package com.crew.evalpipeline.conversation.entity;

import com.crew.evalpipeline.shared.AuditableEntity;
import com.crew.evalpipeline.shared.DomainEnums.ToolExecutionStatus;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "tool_calls")
public class ToolCallEntity extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "turn_id", nullable = false)
    private TurnEntity turn;

    @Column(nullable = false)
    private String toolName;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private JsonNode parameters;

    @JdbcTypeCode(SqlTypes.JSON)
    private JsonNode result;

    private Long latencyMs;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ToolExecutionStatus executionStatus = ToolExecutionStatus.UNKNOWN;

    public Long getId() {
        return id;
    }

    public TurnEntity getTurn() {
        return turn;
    }

    public void setTurn(TurnEntity turn) {
        this.turn = turn;
    }

    public String getToolName() {
        return toolName;
    }

    public void setToolName(String toolName) {
        this.toolName = toolName;
    }

    public JsonNode getParameters() {
        return parameters;
    }

    public void setParameters(JsonNode parameters) {
        this.parameters = parameters;
    }

    public JsonNode getResult() {
        return result;
    }

    public void setResult(JsonNode result) {
        this.result = result;
    }

    public Long getLatencyMs() {
        return latencyMs;
    }

    public void setLatencyMs(Long latencyMs) {
        this.latencyMs = latencyMs;
    }

    public ToolExecutionStatus getExecutionStatus() {
        return executionStatus;
    }

    public void setExecutionStatus(ToolExecutionStatus executionStatus) {
        this.executionStatus = executionStatus;
    }
}
