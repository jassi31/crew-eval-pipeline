package com.crew.evalpipeline.feedback.entity;

import com.crew.evalpipeline.shared.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "annotator_profiles")
public class AnnotatorProfileEntity extends AuditableEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private String annotatorId;

    @Column(nullable = false)
    private Double weight = 1.0;

    @Column(nullable = false)
    private Boolean active = Boolean.TRUE;

    public String getAnnotatorId() {
        return annotatorId;
    }

    public void setAnnotatorId(String annotatorId) {
        this.annotatorId = annotatorId;
    }

    public Double getWeight() {
        return weight;
    }

    public void setWeight(Double weight) {
        this.weight = weight;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}
