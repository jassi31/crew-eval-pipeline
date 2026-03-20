package com.crew.evalpipeline.conversation.repository;

import com.crew.evalpipeline.conversation.entity.ConversationEntity;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConversationRepository extends JpaRepository<ConversationEntity, String> {

    @Override
    @EntityGraph(attributePaths = {"turns", "turns.toolCalls", "feedback", "feedback.annotations"})
    java.util.Optional<ConversationEntity> findById(String conversationId);

    @EntityGraph(attributePaths = {"turns", "turns.toolCalls", "feedback", "feedback.annotations"})
    List<ConversationEntity> findByAgentVersion(String agentVersion);
}
