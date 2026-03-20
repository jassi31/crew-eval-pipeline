package com.crew.evalpipeline.conversation.entity;

import com.crew.evalpipeline.shared.AuditableEntity;
import com.crew.evalpipeline.shared.DomainEnums.TurnRole;
import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "turns")
public class TurnEntity extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    private ConversationEntity conversation;

    @Column(nullable = false)
    private Integer turnIndex;

    @Column(nullable = false)
    private String turnId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TurnRole role;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    private Instant timestamp;

    @OneToMany(mappedBy = "turn", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ToolCallEntity> toolCalls = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public ConversationEntity getConversation() {
        return conversation;
    }

    public void setConversation(ConversationEntity conversation) {
        this.conversation = conversation;
    }

    public Integer getTurnIndex() {
        return turnIndex;
    }

    public void setTurnIndex(Integer turnIndex) {
        this.turnIndex = turnIndex;
    }

    public String getTurnId() {
        return turnId;
    }

    public void setTurnId(String turnId) {
        this.turnId = turnId;
    }

    public TurnRole getRole() {
        return role;
    }

    public void setRole(TurnRole role) {
        this.role = role;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public List<ToolCallEntity> getToolCalls() {
        return toolCalls;
    }

    public void addToolCall(ToolCallEntity toolCall) {
        toolCalls.add(toolCall);
        toolCall.setTurn(this);
    }
}
