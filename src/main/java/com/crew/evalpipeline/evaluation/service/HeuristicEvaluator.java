package com.crew.evalpipeline.evaluation.service;

import com.crew.evalpipeline.config.AppProperties;
import com.crew.evalpipeline.conversation.entity.TurnEntity;
import com.crew.evalpipeline.evaluation.model.EvaluationComponentResult;
import com.crew.evalpipeline.evaluation.model.EvaluationContext;
import com.crew.evalpipeline.evaluation.model.EvaluationIssueDraft;
import com.crew.evalpipeline.shared.DomainEnums.EvaluationIssueType;
import com.crew.evalpipeline.shared.DomainEnums.EvaluatorType;
import com.crew.evalpipeline.shared.DomainEnums.IssueSeverity;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class HeuristicEvaluator implements ConversationEvaluator {

    private final AppProperties appProperties;

    public HeuristicEvaluator(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    @Override
    public EvaluationComponentResult evaluate(EvaluationContext context) {
        double score = 1.0;
        List<EvaluationIssueDraft> issues = new ArrayList<>();

        Long totalLatency = context.conversation().getTotalLatencyMs();
        long threshold = appProperties.getEvaluation().getLatencyThresholdMs();
        if (totalLatency != null && totalLatency > threshold) {
            ObjectNode details = JsonNodeFactory.instance.objectNode();
            details.put("observedLatencyMs", totalLatency);
            details.put("latencyThresholdMs", threshold);
            issues.add(new EvaluationIssueDraft(
                    EvaluationIssueType.LATENCY,
                    IssueSeverity.WARNING,
                    "Response latency " + totalLatency + "ms exceeds target " + threshold + "ms",
                    details
            ));
            score -= 0.2;
        }

        long missingTimestamps = context.conversation().getTurns().stream().filter(turn -> turn.getTimestamp() == null).count();
        if (missingTimestamps > 0) {
            ObjectNode details = JsonNodeFactory.instance.objectNode();
            details.put("missingTimestampCount", missingTimestamps);
            issues.add(new EvaluationIssueDraft(
                    EvaluationIssueType.FORMAT,
                    IssueSeverity.WARNING,
                    "Conversation contains turns without timestamps",
                    details
            ));
            score -= Math.min(0.2, missingTimestamps * 0.05);
        }

        long emptyContents = context.conversation().getTurns().stream()
                .map(TurnEntity::getContent)
                .filter(content -> content == null || content.isBlank())
                .count();
        if (emptyContents > 0) {
            ObjectNode details = JsonNodeFactory.instance.objectNode();
            details.put("emptyTurnCount", emptyContents);
            issues.add(new EvaluationIssueDraft(
                    EvaluationIssueType.FORMAT,
                    IssueSeverity.ERROR,
                    "Conversation contains empty turns",
                    details
            ));
            score -= Math.min(0.4, emptyContents * 0.1);
        }

        long toolCallsWithoutResults = context.conversation().getTurns().stream()
                .flatMap(turn -> turn.getToolCalls().stream())
                .filter(toolCall -> toolCall.getResult() == null)
                .count();
        if (toolCallsWithoutResults > 0) {
            ObjectNode details = JsonNodeFactory.instance.objectNode();
            details.put("toolCallsWithoutResults", toolCallsWithoutResults);
            issues.add(new EvaluationIssueDraft(
                    EvaluationIssueType.TOOL_EXECUTION,
                    IssueSeverity.WARNING,
                    "Tool calls are missing execution results",
                    details
            ));
            score -= Math.min(0.2, toolCallsWithoutResults * 0.05);
        }

        return new EvaluationComponentResult(
                EvaluatorType.HEURISTICS,
                true,
                clamp(score),
                0.9,
                issues,
                Map.of("latencyThresholdMs", (double) threshold)
        );
    }

    private double clamp(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }
}
