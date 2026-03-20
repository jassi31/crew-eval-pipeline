package com.crew.evalpipeline.meta.entity;

import com.crew.evalpipeline.shared.AuditableEntity;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "calibration_snapshots")
public class CalibrationSnapshotEntity extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Double scoreCorrelation;

    @Column(nullable = false)
    private Double issuePrecision;

    @Column(nullable = false)
    private Double issueRecall;

    @Column(nullable = false)
    private Double coverage;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private JsonNode blindSpots;

    public Long getId() {
        return id;
    }

    public Double getScoreCorrelation() {
        return scoreCorrelation;
    }

    public void setScoreCorrelation(Double scoreCorrelation) {
        this.scoreCorrelation = scoreCorrelation;
    }

    public Double getIssuePrecision() {
        return issuePrecision;
    }

    public void setIssuePrecision(Double issuePrecision) {
        this.issuePrecision = issuePrecision;
    }

    public Double getIssueRecall() {
        return issueRecall;
    }

    public void setIssueRecall(Double issueRecall) {
        this.issueRecall = issueRecall;
    }

    public Double getCoverage() {
        return coverage;
    }

    public void setCoverage(Double coverage) {
        this.coverage = coverage;
    }

    public JsonNode getBlindSpots() {
        return blindSpots;
    }

    public void setBlindSpots(JsonNode blindSpots) {
        this.blindSpots = blindSpots;
    }
}
