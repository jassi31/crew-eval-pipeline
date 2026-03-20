package com.crew.evalpipeline.evaluation.service;

import com.crew.evalpipeline.evaluation.model.EvaluationComponentResult;
import com.crew.evalpipeline.evaluation.model.EvaluationContext;
import com.crew.evalpipeline.shared.DomainEnums.EvaluatorType;
import com.crew.evalpipeline.shared.DomainEnums.TurnRole;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class LlmJudgeEvaluator implements ConversationEvaluator {

    private final JudgeProvider judgeProvider;

    public LlmJudgeEvaluator(JudgeProvider judgeProvider) {
        this.judgeProvider = judgeProvider;
    }

    @Override
    public EvaluationComponentResult evaluate(EvaluationContext context) {
        boolean hasAssistantTurns = context.conversation().getTurns().stream()
                .anyMatch(turn -> turn.getRole() == TurnRole.ASSISTANT);
        if (!hasAssistantTurns) {
            return new EvaluationComponentResult(EvaluatorType.RESPONSE_QUALITY, false, 0.0, 0.9, List.of(), Map.of());
        }

        JudgeProvider.JudgeResult result = judgeProvider.judge(context.conversation());
        return new EvaluationComponentResult(
                EvaluatorType.RESPONSE_QUALITY,
                true,
                result.score(),
                result.confidence(),
                List.of(),
                Map.of("judgeScore", result.score())
        );
    }
}
