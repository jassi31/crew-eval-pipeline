package com.crew.evalpipeline.evaluation;

import static org.assertj.core.api.Assertions.assertThat;

import com.crew.evalpipeline.conversation.entity.ConversationEntity;
import com.crew.evalpipeline.conversation.entity.ToolCallEntity;
import com.crew.evalpipeline.conversation.entity.TurnEntity;
import com.crew.evalpipeline.evaluation.model.EvaluationComponentResult;
import com.crew.evalpipeline.evaluation.model.EvaluationContext;
import com.crew.evalpipeline.evaluation.service.ToolCallEvaluator;
import com.crew.evalpipeline.evaluation.service.ToolRegistryService.ToolDefinition;
import com.crew.evalpipeline.feedback.service.FeedbackConsensus;
import com.crew.evalpipeline.shared.DomainEnums.ToolExecutionStatus;
import com.crew.evalpipeline.shared.DomainEnums.TurnRole;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ToolCallEvaluatorTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldFlagFormatAndHallucinatedParameters() {
        ToolCallEvaluator evaluator = new ToolCallEvaluator();

        ConversationEntity conversation = new ConversationEntity();
        conversation.setConversationId("conv-1");
        conversation.setAgentVersion("v1");

        TurnEntity userTurn = new TurnEntity();
        userTurn.setTurnId("1");
        userTurn.setTurnIndex(1);
        userTurn.setRole(TurnRole.USER);
        userTurn.setContent("Please book me a flight to NYC next week.");
        conversation.addTurn(userTurn);

        TurnEntity assistantTurn = new TurnEntity();
        assistantTurn.setTurnId("2");
        assistantTurn.setTurnIndex(2);
        assistantTurn.setRole(TurnRole.ASSISTANT);
        assistantTurn.setContent("I found flights for you.");

        ToolCallEntity toolCall = new ToolCallEntity();
        toolCall.setToolName("flight_search");
        toolCall.setParameters(objectMapper.createObjectNode()
                .put("destination", "NYC")
                .put("date_range", "next-week")
                .put("seat_type", "window"));
        toolCall.setResult(objectMapper.createObjectNode().put("status", "success"));
        toolCall.setExecutionStatus(ToolExecutionStatus.SUCCESS);
        assistantTurn.addToolCall(toolCall);
        conversation.addTurn(assistantTurn);

        EvaluationContext context = new EvaluationContext(
                conversation,
                new FeedbackConsensus(null, null, null, null, 1.0, Map.of(), Map.of()),
                Map.of("flight_search", new ToolDefinition(
                        "flight_search",
                        "Flight search tool",
                        List.of("destination", "date_range"),
                        Map.of(
                                "destination", "^[A-Z]{3}$",
                                "date_range", "^\\d{4}-\\d{2}-\\d{2}/\\d{4}-\\d{2}-\\d{2}$"
                        ),
                        List.of("flight", "fly", "booking")
                ))
        );

        EvaluationComponentResult result = evaluator.evaluate(context);

        assertThat(result.applicable()).isTrue();
        assertThat(result.score()).isLessThan(1.0);
        assertThat(result.issues()).hasSizeGreaterThanOrEqualTo(2);
        assertThat(result.issues()).anyMatch(issue -> issue.description().contains("format validation failed"));
        assertThat(result.issues()).anyMatch(issue -> issue.description().contains("Hallucinated tool parameter"));
        assertThat(result.metrics()).containsKeys("selectionAccuracy", "parameterAccuracy", "executionSuccess");
    }
}
