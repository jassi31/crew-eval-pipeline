package com.crew.evalpipeline.feedback.repository;

import com.crew.evalpipeline.feedback.entity.FeedbackEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeedbackRepository extends JpaRepository<FeedbackEntity, Long> {

    @EntityGraph(attributePaths = {"annotations"})
    Optional<FeedbackEntity> findByConversationConversationId(String conversationId);
}
