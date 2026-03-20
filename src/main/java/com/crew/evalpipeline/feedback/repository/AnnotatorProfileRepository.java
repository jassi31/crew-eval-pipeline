package com.crew.evalpipeline.feedback.repository;

import com.crew.evalpipeline.feedback.entity.AnnotatorProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnnotatorProfileRepository extends JpaRepository<AnnotatorProfileEntity, String> {
}
