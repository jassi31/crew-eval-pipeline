package com.crew.evalpipeline.api;

import com.crew.evalpipeline.api.dto.ConversationDtos.BatchIngestionResponse;
import com.crew.evalpipeline.api.dto.ConversationDtos.ConversationAcceptedResponse;
import com.crew.evalpipeline.api.dto.ConversationDtos.ConversationIngestRequest;
import com.crew.evalpipeline.api.dto.ConversationDtos.ConversationResponse;
import com.crew.evalpipeline.api.dto.ConversationDtos.FeedbackUpsertRequest;
import com.crew.evalpipeline.conversation.service.ConversationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/conversations")
public class ConversationController {

    private final ConversationService conversationService;

    public ConversationController(ConversationService conversationService) {
        this.conversationService = conversationService;
    }

    @PostMapping
    public ResponseEntity<ConversationAcceptedResponse> ingestConversation(@Valid @RequestBody ConversationIngestRequest request) {
        return ResponseEntity.accepted().body(conversationService.ingestConversation(request));
    }

    @PostMapping("/batch")
    public ResponseEntity<BatchIngestionResponse> ingestBatch(@RequestPart("file") MultipartFile file) {
        return ResponseEntity.accepted().body(conversationService.ingestBatch(file));
    }

    @GetMapping("/{conversationId}")
    public ConversationResponse getConversation(@PathVariable String conversationId) {
        return conversationService.getConversation(conversationId);
    }

    @PostMapping("/{conversationId}/feedback")
    public ResponseEntity<ConversationAcceptedResponse> upsertFeedback(
            @PathVariable String conversationId,
            @Valid @RequestBody FeedbackUpsertRequest request
    ) {
        return ResponseEntity.accepted().body(conversationService.upsertFeedback(conversationId, request));
    }
}
