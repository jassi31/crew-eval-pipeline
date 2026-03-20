package com.crew.evalpipeline.evaluation;

import static org.assertj.core.api.Assertions.assertThat;

import com.crew.evalpipeline.conversation.entity.ConversationEntity;
import com.crew.evalpipeline.conversation.entity.TurnEntity;
import com.crew.evalpipeline.evaluation.model.EvaluationComponentResult;
import com.crew.evalpipeline.evaluation.model.EvaluationContext;
import com.crew.evalpipeline.evaluation.service.CoherenceEvaluator;
import com.crew.evalpipeline.feedback.service.FeedbackConsensus;
import com.crew.evalpipeline.shared.DomainEnums.TurnRole;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CoherenceEvaluatorTest {

    @Test
    void shouldFlagContextLossInLongConversations() {
        CoherenceEvaluator evaluator = new CoherenceEvaluator();
        ConversationEntity conversation = new ConversationEntity();
        conversation.setConversationId("conv-2");
        conversation.setAgentVersion("v1");

        addTurn(conversation, 1, TurnRole.USER, "I prefer a window seat and vegetarian meal.");
        addTurn(conversation, 2, TurnRole.ASSISTANT, "Noted, I will keep those preferences in mind.");
        addTurn(conversation, 3, TurnRole.USER, "I am flying to NYC.");
        addTurn(conversation, 4, TurnRole.ASSISTANT, "Understood.");
        addTurn(conversation, 5, TurnRole.USER, "Can you confirm my options?");
        addTurn(conversation, 6, TurnRole.ASSISTANT, "Your booking is in progress.");

        EvaluationComponentResult result = evaluator.evaluate(new EvaluationContext(
                conversation,
                new FeedbackConsensus(null, null, null, null, 1.0, Map.of(), Map.of()),
                Map.of()
        ));

        assertThat(result.score()).isLessThan(1.0);
        assertThat(result.issues()).anyMatch(issue -> issue.description().contains("lost context"));
    }

    private void addTurn(ConversationEntity conversation, int index, TurnRole role, String content) {
        TurnEntity turn = new TurnEntity();
        turn.setTurnId(String.valueOf(index));
        turn.setTurnIndex(index);
        turn.setRole(role);
        turn.setContent(content);
        conversation.addTurn(turn);
    }
}
