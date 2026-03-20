package com.crew.evalpipeline.feedback.entity;

import com.crew.evalpipeline.conversation.entity.ConversationEntity;
import com.crew.evalpipeline.shared.AuditableEntity;
import com.crew.evalpipeline.shared.DomainEnums.OpsQuality;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "feedback")
public class FeedbackEntity extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false, unique = true)
    private ConversationEntity conversation;

    private Integer userRating;

    @Enumerated(EnumType.STRING)
    private OpsQuality opsQuality;

    @Column(columnDefinition = "TEXT")
    private String opsNotes;

    @OneToMany(mappedBy = "feedback", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AnnotationEntity> annotations = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public ConversationEntity getConversation() {
        return conversation;
    }

    public void setConversation(ConversationEntity conversation) {
        this.conversation = conversation;
    }

    public Integer getUserRating() {
        return userRating;
    }

    public void setUserRating(Integer userRating) {
        this.userRating = userRating;
    }

    public OpsQuality getOpsQuality() {
        return opsQuality;
    }

    public void setOpsQuality(OpsQuality opsQuality) {
        this.opsQuality = opsQuality;
    }

    public String getOpsNotes() {
        return opsNotes;
    }

    public void setOpsNotes(String opsNotes) {
        this.opsNotes = opsNotes;
    }

    public List<AnnotationEntity> getAnnotations() {
        return annotations;
    }

    public void addAnnotation(AnnotationEntity annotation) {
        annotations.add(annotation);
        annotation.setFeedback(this);
    }

    public void clearAnnotations() {
        annotations.clear();
    }
}
