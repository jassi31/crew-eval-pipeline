package com.crew.evalpipeline.evaluation.service;

import com.crew.evalpipeline.conversation.entity.TurnEntity;
import com.crew.evalpipeline.evaluation.model.EvaluationComponentResult;
import com.crew.evalpipeline.evaluation.model.EvaluationContext;
import com.crew.evalpipeline.evaluation.model.EvaluationIssueDraft;
import com.crew.evalpipeline.shared.DomainEnums.EvaluationIssueType;
import com.crew.evalpipeline.shared.DomainEnums.EvaluatorType;
import com.crew.evalpipeline.shared.DomainEnums.IssueSeverity;
import com.crew.evalpipeline.shared.DomainEnums.TurnRole;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class CoherenceEvaluator implements ConversationEvaluator {

    private static final Pattern PREFERENCE_PATTERN = Pattern.compile("(?i)\\b(prefer|want|need|like)\\s+([a-zA-Z0-9\\s-]{2,30})");

    @Override
    public EvaluationComponentResult evaluate(EvaluationContext context) {
        List<TurnEntity> turns = context.conversation().getTurns().stream()
                .sorted(java.util.Comparator.comparing(TurnEntity::getTurnIndex))
                .toList();

        if (turns.isEmpty()) {
            return new EvaluationComponentResult(EvaluatorType.COHERENCE, true, 0.1, 0.9, List.of(), java.util.Map.of());
        }

        List<String> preferences = extractPreferences(turns);
        List<TurnEntity> assistantTurns = turns.stream().filter(turn -> turn.getRole() == TurnRole.ASSISTANT).toList();
        String finalAssistantContent = assistantTurns.isEmpty() ? "" : assistantTurns.get(assistantTurns.size() - 1).getContent().toLowerCase(Locale.ROOT);

        double score = 1.0;
        List<EvaluationIssueDraft> issues = new ArrayList<>();

        if (turns.size() > 5 && !preferences.isEmpty()) {
            List<String> missingPreferences = preferences.stream()
                    .filter(preference -> !finalAssistantContent.contains(preference.toLowerCase(Locale.ROOT)))
                    .toList();
            if (!missingPreferences.isEmpty()) {
                ObjectNode details = JsonNodeFactory.instance.objectNode();
                details.putPOJO("missingPreferences", missingPreferences);
                issues.add(new EvaluationIssueDraft(
                        EvaluationIssueType.COHERENCE,
                        IssueSeverity.WARNING,
                        "Agent appears to have lost context from earlier turns",
                        details
                ));
                score -= Math.min(0.5, missingPreferences.size() * 0.12);
            }
        }

        boolean asksForKnownContextAgain = finalAssistantContent.contains("which")
                && preferences.stream().anyMatch(preference -> finalAssistantContent.contains(preference.substring(0, Math.min(preference.length(), 3)).toLowerCase(Locale.ROOT)));
        if (asksForKnownContextAgain) {
            ObjectNode details = JsonNodeFactory.instance.objectNode();
            details.put("signal", "asked_for_already_provided_context");
            issues.add(new EvaluationIssueDraft(
                    EvaluationIssueType.COHERENCE,
                    IssueSeverity.WARNING,
                    "Agent may be asking for context that was already provided earlier",
                    details
            ));
            score -= 0.15;
        }

        return new EvaluationComponentResult(
                EvaluatorType.COHERENCE,
                true,
                clamp(score),
                preferences.isEmpty() ? 0.55 : 0.75,
                issues,
                java.util.Map.of("preferenceCount", (double) preferences.size())
        );
    }

    private List<String> extractPreferences(List<TurnEntity> turns) {
        Set<String> preferences = new LinkedHashSet<>();
        for (TurnEntity turn : turns) {
            if (turn.getRole() != TurnRole.USER || turn.getContent() == null) {
                continue;
            }
            Matcher matcher = PREFERENCE_PATTERN.matcher(turn.getContent());
            while (matcher.find()) {
                preferences.add(matcher.group(2).trim().toLowerCase(Locale.ROOT));
            }
            if (turn.getContent().toLowerCase(Locale.ROOT).contains("window seat")) {
                preferences.add("window seat");
            }
            if (turn.getContent().toLowerCase(Locale.ROOT).contains("vegetarian")) {
                preferences.add("vegetarian");
            }
        }
        return new ArrayList<>(preferences);
    }

    private double clamp(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }
}
