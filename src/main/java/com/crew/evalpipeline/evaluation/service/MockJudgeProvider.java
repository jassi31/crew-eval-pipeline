package com.crew.evalpipeline.evaluation.service;

import com.crew.evalpipeline.conversation.entity.ConversationEntity;
import com.crew.evalpipeline.conversation.entity.TurnEntity;
import com.crew.evalpipeline.shared.DomainEnums.TurnRole;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.judge", name = "provider", havingValue = "mock", matchIfMissing = true)
public class MockJudgeProvider implements JudgeProvider {

    @Override
    public JudgeResult judge(ConversationEntity conversation) {
        List<TurnEntity> assistantTurns = conversation.getTurns().stream()
                .filter(turn -> turn.getRole() == TurnRole.ASSISTANT)
                .toList();
        if (assistantTurns.isEmpty()) {
            return new JudgeResult(0.1, 0.9);
        }

        TurnEntity lastAssistantTurn = assistantTurns.get(assistantTurns.size() - 1);
        int contentLength = lastAssistantTurn.getContent() == null ? 0 : lastAssistantTurn.getContent().length();
        double score = 0.4;
        if (contentLength > 30) {
            score += 0.2;
        }
        if (contentLength > 80) {
            score += 0.15;
        }
        if (lastAssistantTurn.getContent() != null && lastAssistantTurn.getContent().toLowerCase().contains("happy to help")) {
            score += 0.1;
        }
        if (Boolean.TRUE.equals(conversation.getMissionCompleted())) {
            score += 0.1;
        }
        return new JudgeResult(Math.min(score, 1.0), 0.7);
    }
}
