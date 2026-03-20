package com.crew.evalpipeline.evaluation.service;

import com.crew.evalpipeline.conversation.entity.ToolCallEntity;
import com.crew.evalpipeline.evaluation.model.EvaluationComponentResult;
import com.crew.evalpipeline.evaluation.model.EvaluationContext;
import com.crew.evalpipeline.evaluation.model.EvaluationIssueDraft;
import com.crew.evalpipeline.shared.DomainEnums.EvaluationIssueType;
import com.crew.evalpipeline.shared.DomainEnums.EvaluatorType;
import com.crew.evalpipeline.shared.DomainEnums.IssueSeverity;
import com.crew.evalpipeline.shared.DomainEnums.ToolExecutionStatus;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class ToolCallEvaluator implements ConversationEvaluator {

    @Override
    public EvaluationComponentResult evaluate(EvaluationContext context) {
        List<ToolCallEntity> toolCalls = context.conversation().getTurns().stream()
                .flatMap(turn -> turn.getToolCalls().stream())
                .toList();

        Set<String> expectedTools = inferExpectedTools(context);
        if (toolCalls.isEmpty() && expectedTools.isEmpty()) {
            return new EvaluationComponentResult(EvaluatorType.TOOL_ACCURACY, false, 1.0, 0.4, List.of(), Map.of());
        }

        double selectionAccuracy = 1.0;
        double parameterAccuracy = 1.0;
        double executionScore = 1.0;
        List<EvaluationIssueDraft> issues = new ArrayList<>();
        Set<String> actualTools = new HashSet<>();

        for (ToolCallEntity toolCall : toolCalls) {
            actualTools.add(toolCall.getToolName());
            ToolRegistryService.ToolDefinition definition = context.toolDefinitions().get(toolCall.getToolName());
            if (definition == null) {
                selectionAccuracy -= 0.4;
                issues.add(issue(EvaluationIssueType.TOOL_SELECTION, IssueSeverity.ERROR,
                        "Unknown tool called: " + toolCall.getToolName(),
                        toolCall.getToolName(), null, null));
                continue;
            }

            Set<String> providedParameters = new HashSet<>();
            JsonNode parameters = toolCall.getParameters();
            parameters.fieldNames().forEachRemaining(providedParameters::add);

            for (String requiredParameter : definition.requiredParameters()) {
                if (!parameters.hasNonNull(requiredParameter)) {
                    parameterAccuracy -= 0.15;
                    issues.add(issue(EvaluationIssueType.TOOL_PARAMETERS, IssueSeverity.ERROR,
                            "Missing required tool parameter: " + requiredParameter,
                            toolCall.getToolName(), requiredParameter, "missing"));
                }
            }

            for (String parameterName : providedParameters) {
                if (!definition.requiredParameters().contains(parameterName)
                        && !definition.validationPatterns().containsKey(parameterName)) {
                    parameterAccuracy -= 0.1;
                    issues.add(issue(EvaluationIssueType.TOOL_PARAMETERS, IssueSeverity.WARNING,
                            "Hallucinated tool parameter: " + parameterName,
                            toolCall.getToolName(), parameterName, "hallucinated"));
                }

                String pattern = definition.validationPatterns().get(parameterName);
                if (pattern != null && parameters.hasNonNull(parameterName)) {
                    String value = parameters.get(parameterName).asText("");
                    if (!Pattern.compile(pattern).matcher(value).matches()) {
                        parameterAccuracy -= 0.1;
                        issues.add(issue(EvaluationIssueType.TOOL_PARAMETERS, IssueSeverity.WARNING,
                                "Parameter format validation failed for " + parameterName,
                                toolCall.getToolName(), parameterName, "format"));
                    }
                }
            }

            if (toolCall.getExecutionStatus() == ToolExecutionStatus.FAILURE) {
                executionScore -= 0.3;
                issues.add(issue(EvaluationIssueType.TOOL_EXECUTION, IssueSeverity.WARNING,
                        "Tool execution failed for " + toolCall.getToolName(),
                        toolCall.getToolName(), null, "failure"));
            } else if (toolCall.getExecutionStatus() == ToolExecutionStatus.PARTIAL) {
                executionScore -= 0.15;
                issues.add(issue(EvaluationIssueType.TOOL_EXECUTION, IssueSeverity.WARNING,
                        "Tool execution only partially succeeded for " + toolCall.getToolName(),
                        toolCall.getToolName(), null, "partial"));
            }
        }

        if (!expectedTools.isEmpty() && actualTools.stream().noneMatch(expectedTools::contains)) {
            selectionAccuracy -= 0.5;
            ObjectNode details = JsonNodeFactory.instance.objectNode();
            details.putPOJO("expectedTools", expectedTools);
            details.putPOJO("actualTools", actualTools);
            issues.add(new EvaluationIssueDraft(
                    EvaluationIssueType.TOOL_SELECTION,
                    IssueSeverity.ERROR,
                    "Agent selected the wrong tool for the request",
                    details
            ));
        }

        double finalScore = clamp((selectionAccuracy + parameterAccuracy + executionScore) / 3.0);
        return new EvaluationComponentResult(
                EvaluatorType.TOOL_ACCURACY,
                true,
                finalScore,
                0.85,
                issues,
                Map.of(
                        "selectionAccuracy", clamp(selectionAccuracy),
                        "parameterAccuracy", clamp(parameterAccuracy),
                        "executionSuccess", executionScore > 0.99 ? 1.0 : 0.0
                )
        );
    }

    private Set<String> inferExpectedTools(EvaluationContext context) {
        String userContent = context.conversation().getTurns().stream()
                .filter(turn -> turn.getRole() == com.crew.evalpipeline.shared.DomainEnums.TurnRole.USER)
                .map(turn -> turn.getContent().toLowerCase(Locale.ROOT))
                .reduce("", (left, right) -> left + " " + right);

        Set<String> expectedTools = new HashSet<>();
        context.toolDefinitions().forEach((toolName, definition) -> {
            boolean matches = definition.selectionKeywords().stream()
                    .map(keyword -> keyword.toLowerCase(Locale.ROOT))
                    .anyMatch(userContent::contains);
            if (matches) {
                expectedTools.add(toolName);
            }
        });
        return expectedTools;
    }

    private EvaluationIssueDraft issue(
            EvaluationIssueType type,
            IssueSeverity severity,
            String description,
            String toolName,
            String parameter,
            String issueCategory
    ) {
        ObjectNode details = JsonNodeFactory.instance.objectNode();
        if (toolName != null) {
            details.put("toolName", toolName);
        }
        if (parameter != null) {
            details.put("parameter", parameter);
        }
        if (issueCategory != null) {
            details.put("issueCategory", issueCategory);
        }
        return new EvaluationIssueDraft(type, severity, description, details);
    }

    private double clamp(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }
}
