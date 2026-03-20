package com.crew.evalpipeline.evaluation.model;

import com.crew.evalpipeline.conversation.entity.ConversationEntity;
import com.crew.evalpipeline.feedback.service.FeedbackConsensus;
import com.crew.evalpipeline.evaluation.service.ToolRegistryService.ToolDefinition;
import java.util.Map;

public record EvaluationContext(
        ConversationEntity conversation,
        FeedbackConsensus feedbackConsensus,
        Map<String, ToolDefinition> toolDefinitions
) {
}
