package com.crew.evalpipeline.api;

import com.crew.evalpipeline.api.dto.EvaluationDtos.SuggestionResponse;
import com.crew.evalpipeline.suggestion.entity.ImprovementSuggestionEntity;
import com.crew.evalpipeline.suggestion.service.SuggestionService;
import java.util.ArrayList;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/suggestions")
public class SuggestionController {

    private final SuggestionService suggestionService;

    public SuggestionController(SuggestionService suggestionService) {
        this.suggestionService = suggestionService;
    }

    @GetMapping
    public List<SuggestionResponse> listSuggestions(
            @RequestParam(required = false) String scope,
            @RequestParam(required = false) String targetType,
            @RequestParam(required = false) String agentVersion,
            @RequestParam(required = false) String status
    ) {
        return suggestionService.listSuggestions(scope, targetType, agentVersion, status).stream()
                .map(this::toResponse)
                .toList();
    }

    private SuggestionResponse toResponse(ImprovementSuggestionEntity suggestion) {
        List<String> evidenceConversationIds = new ArrayList<>();
        suggestion.getEvidenceConversationIds().forEach(node -> evidenceConversationIds.add(node.asText()));
        return new SuggestionResponse(
                suggestion.getSuggestionId(),
                suggestion.getScope(),
                suggestion.getTargetType(),
                suggestion.getTargetKey(),
                suggestion.getSuggestion(),
                suggestion.getRationale(),
                suggestion.getConfidence(),
                suggestion.getFrequency(),
                suggestion.getExpectedImpact(),
                evidenceConversationIds,
                suggestion.getStatus(),
                suggestion.getCreatedAt()
        );
    }
}
