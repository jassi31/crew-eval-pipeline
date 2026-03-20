package com.crew.evalpipeline.feedback.repository;

import com.crew.evalpipeline.feedback.entity.AnnotationEntity;
import com.crew.evalpipeline.shared.DomainEnums.AnnotationType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnnotationRepository extends JpaRepository<AnnotationEntity, Long> {

    List<AnnotationEntity> findByType(AnnotationType type);
}
