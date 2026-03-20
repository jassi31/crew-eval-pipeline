package com.crew.evalpipeline.evaluation.repository;

import com.crew.evalpipeline.evaluation.entity.EvaluationEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface EvaluationRepository extends JpaRepository<EvaluationEntity, String>, JpaSpecificationExecutor<EvaluationEntity> {

    @EntityGraph(attributePaths = {"issues"})
    Optional<EvaluationEntity> findTopByConversationConversationIdOrderByCreatedAtDesc(String conversationId);
}
