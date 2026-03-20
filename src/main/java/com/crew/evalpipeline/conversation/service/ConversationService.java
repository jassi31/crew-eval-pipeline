package com.crew.evalpipeline.conversation.service;

import com.crew.evalpipeline.api.dto.ConversationDtos.AnnotationRequest;
import com.crew.evalpipeline.api.dto.ConversationDtos.AnnotationResponse;
import com.crew.evalpipeline.api.dto.ConversationDtos.BatchIngestionResponse;
import com.crew.evalpipeline.api.dto.ConversationDtos.ConversationAcceptedResponse;
import com.crew.evalpipeline.api.dto.ConversationDtos.ConversationIngestRequest;
import com.crew.evalpipeline.api.dto.ConversationDtos.ConversationResponse;
import com.crew.evalpipeline.api.dto.ConversationDtos.EvaluationSummaryResponse;
import com.crew.evalpipeline.api.dto.ConversationDtos.FeedbackRequest;
import com.crew.evalpipeline.api.dto.ConversationDtos.FeedbackResponse;
import com.crew.evalpipeline.api.dto.ConversationDtos.FeedbackUpsertRequest;
import com.crew.evalpipeline.api.dto.ConversationDtos.MetadataResponse;
import com.crew.evalpipeline.api.dto.ConversationDtos.OpsReviewResponse;
import com.crew.evalpipeline.api.dto.ConversationDtos.ToolCallRequest;
import com.crew.evalpipeline.api.dto.ConversationDtos.ToolCallResponse;
import com.crew.evalpipeline.api.dto.ConversationDtos.TurnRequest;
import com.crew.evalpipeline.api.dto.ConversationDtos.TurnResponse;
import com.crew.evalpipeline.api.error.BadRequestException;
import com.crew.evalpipeline.api.error.ResourceNotFoundException;
import com.crew.evalpipeline.conversation.entity.ConversationEntity;
import com.crew.evalpipeline.conversation.entity.ToolCallEntity;
import com.crew.evalpipeline.conversation.entity.TurnEntity;
import com.crew.evalpipeline.conversation.repository.ConversationRepository;
import com.crew.evalpipeline.evaluation.entity.EvaluationEntity;
import com.crew.evalpipeline.evaluation.entity.EvaluationJobEntity;
import com.crew.evalpipeline.evaluation.repository.EvaluationRepository;
import com.crew.evalpipeline.evaluation.service.EvaluationJobService;
import com.crew.evalpipeline.feedback.entity.AnnotationEntity;
import com.crew.evalpipeline.feedback.entity.FeedbackEntity;
import com.crew.evalpipeline.shared.DomainEnums.ConversationStatus;
import com.crew.evalpipeline.shared.DomainEnums.ToolExecutionStatus;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final EvaluationRepository evaluationRepository;
    private final EvaluationJobService evaluationJobService;
    private final ObjectMapper objectMapper;

    public ConversationService(
            ConversationRepository conversationRepository,
            EvaluationRepository evaluationRepository,
            EvaluationJobService evaluationJobService,
            ObjectMapper objectMapper
    ) {
        this.conversationRepository = conversationRepository;
        this.evaluationRepository = evaluationRepository;
        this.evaluationJobService = evaluationJobService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public ConversationAcceptedResponse ingestConversation(ConversationIngestRequest request) {
        if (conversationRepository.existsById(request.conversationId())) {
            throw new BadRequestException("Conversation already exists: " + request.conversationId());
        }

        ConversationEntity conversation = mapConversation(request);
        conversationRepository.save(conversation);
        EvaluationJobEntity job = evaluationJobService.enqueue(conversation, "INGESTION");
        return new ConversationAcceptedResponse(conversation.getConversationId(), job.getJobId(), conversation.getStatus());
    }

    @Transactional
    public BatchIngestionResponse ingestBatch(MultipartFile file) {
        String ingestionJobId = UUID.randomUUID().toString();
        List<ConversationIngestRequest> requests = parseBatchFile(file);
        List<String> conversationIds = new ArrayList<>();
        for (ConversationIngestRequest request : requests) {
            conversationIds.add(ingestConversation(request).conversationId());
        }
        return new BatchIngestionResponse(ingestionJobId, conversationIds.size(), conversationIds);
    }

    @Transactional(readOnly = true)
    public ConversationResponse getConversation(String conversationId) {
        ConversationEntity conversation = findConversation(conversationId);
        EvaluationSummaryResponse latestEvaluation = evaluationRepository.findTopByConversationConversationIdOrderByCreatedAtDesc(conversationId)
                .map(this::toEvaluationSummary)
                .orElse(null);

        return new ConversationResponse(
                conversation.getConversationId(),
                conversation.getAgentVersion(),
                conversation.getStatus(),
                conversation.getSource(),
                conversation.getTags(),
                conversation.getExpectedOutcome(),
                new MetadataResponse(conversation.getTotalLatencyMs(), conversation.getMissionCompleted()),
                toFeedbackResponse(conversation.getFeedback()),
                conversation.getTurns().stream()
                        .sorted(Comparator.comparing(TurnEntity::getTurnIndex))
                        .map(this::toTurnResponse)
                        .toList(),
                latestEvaluation
        );
    }

    @Transactional
    public ConversationAcceptedResponse upsertFeedback(String conversationId, FeedbackUpsertRequest request) {
        ConversationEntity conversation = findConversation(conversationId);
        FeedbackEntity feedback = conversation.getFeedback();
        if (feedback == null) {
            feedback = new FeedbackEntity();
            conversation.setFeedback(feedback);
        }

        applyFeedback(feedback, request.userRating(), request.opsReview() == null ? null : request.opsReview().quality(),
                request.opsReview() == null ? null : request.opsReview().notes(), request.annotations());
        conversationRepository.save(conversation);
        EvaluationJobEntity job = evaluationJobService.enqueue(conversation, "FEEDBACK_UPDATE");
        return new ConversationAcceptedResponse(conversation.getConversationId(), job.getJobId(), conversation.getStatus());
    }

    @Transactional(readOnly = true)
    public ConversationEntity findConversation(String conversationId) {
        return conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found: " + conversationId));
    }

    private ConversationEntity mapConversation(ConversationIngestRequest request) {
        ConversationEntity conversation = new ConversationEntity();
        conversation.setConversationId(request.conversationId());
        conversation.setAgentVersion(request.agentVersion());
        conversation.setRawPayload(objectMapper.valueToTree(request));
        conversation.setExpectedOutcome(request.expectedOutcome());
        conversation.setSource(request.source());
        conversation.setTags(request.tags() == null ? new ArrayList<>() : new ArrayList<>(request.tags()));
        conversation.setStatus(ConversationStatus.QUEUED);
        conversation.setTotalLatencyMs(request.metadata() != null && request.metadata().totalLatencyMs() != null
                ? request.metadata().totalLatencyMs() : 0L);
        conversation.setMissionCompleted(request.metadata() != null && request.metadata().missionCompleted() != null
                ? request.metadata().missionCompleted() : Boolean.FALSE);

        for (TurnRequest turnRequest : request.turns()) {
            TurnEntity turn = new TurnEntity();
            turn.setTurnId(String.valueOf(turnRequest.turnId()));
            turn.setTurnIndex(turnRequest.turnId());
            turn.setRole(turnRequest.role());
            turn.setContent(turnRequest.content());
            turn.setTimestamp(turnRequest.timestamp());

            if (turnRequest.toolCalls() != null) {
                for (ToolCallRequest toolCallRequest : turnRequest.toolCalls()) {
                    ToolCallEntity toolCall = new ToolCallEntity();
                    toolCall.setToolName(toolCallRequest.toolName());
                    toolCall.setParameters(toolCallRequest.parameters());
                    toolCall.setResult(toolCallRequest.result());
                    toolCall.setLatencyMs(toolCallRequest.latencyMs());
                    toolCall.setExecutionStatus(resolveExecutionStatus(toolCallRequest));
                    turn.addToolCall(toolCall);
                }
            }
            conversation.addTurn(turn);
        }

        if (request.feedback() != null) {
            FeedbackEntity feedback = new FeedbackEntity();
            applyFeedback(feedback, request.feedback().userRating(),
                    request.feedback().opsReview() == null ? null : request.feedback().opsReview().quality(),
                    request.feedback().opsReview() == null ? null : request.feedback().opsReview().notes(),
                    request.feedback().annotations());
            conversation.setFeedback(feedback);
        }
        return conversation;
    }

    private void applyFeedback(
            FeedbackEntity feedback,
            Integer userRating,
            com.crew.evalpipeline.shared.DomainEnums.OpsQuality opsQuality,
            String opsNotes,
            List<AnnotationRequest> annotations
    ) {
        feedback.setUserRating(userRating);
        feedback.setOpsQuality(opsQuality);
        feedback.setOpsNotes(opsNotes);
        feedback.clearAnnotations();
        if (annotations != null) {
            for (AnnotationRequest annotationRequest : annotations) {
                AnnotationEntity annotation = new AnnotationEntity();
                annotation.setType(annotationRequest.type());
                annotation.setLabel(annotationRequest.label());
                annotation.setAnnotatorId(annotationRequest.annotatorId());
                annotation.setConfidence(annotationRequest.confidence() == null ? 1.0 : annotationRequest.confidence());
                annotation.setGroundTruth(annotationRequest.groundTruth());
                feedback.addAnnotation(annotation);
            }
        }
    }

    private List<ConversationIngestRequest> parseBatchFile(MultipartFile file) {
        try {
            String content = new String(file.getBytes(), StandardCharsets.UTF_8).trim();
            if (content.startsWith("[")) {
                JsonNode root = objectMapper.readTree(content);
                List<ConversationIngestRequest> requests = new ArrayList<>();
                for (JsonNode node : root) {
                    requests.add(objectMapper.treeToValue(node, ConversationIngestRequest.class));
                }
                return requests;
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
                List<ConversationIngestRequest> requests = new ArrayList<>();
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.isBlank()) {
                        requests.add(objectMapper.readValue(line, ConversationIngestRequest.class));
                    }
                }
                return requests;
            }
        } catch (IOException exception) {
            throw new BadRequestException("Unable to parse batch file: " + exception.getMessage());
        }
    }

    private ToolExecutionStatus resolveExecutionStatus(ToolCallRequest request) {
        if (request.executionStatus() != null) {
            return request.executionStatus();
        }
        if (request.result() != null && request.result().hasNonNull("status")) {
            String status = request.result().get("status").asText("unknown").toLowerCase();
            if ("success".equals(status)) {
                return ToolExecutionStatus.SUCCESS;
            }
            if ("failure".equals(status) || "failed".equals(status) || "error".equals(status)) {
                return ToolExecutionStatus.FAILURE;
            }
            if ("partial".equals(status)) {
                return ToolExecutionStatus.PARTIAL;
            }
        }
        return ToolExecutionStatus.UNKNOWN;
    }

    private TurnResponse toTurnResponse(TurnEntity turn) {
        return new TurnResponse(
                turn.getTurnId(),
                turn.getTurnIndex(),
                turn.getRole(),
                turn.getContent(),
                turn.getTimestamp(),
                turn.getToolCalls().stream()
                        .map(toolCall -> new ToolCallResponse(
                                toolCall.getToolName(),
                                toolCall.getParameters(),
                                toolCall.getResult(),
                                toolCall.getLatencyMs(),
                                toolCall.getExecutionStatus()))
                        .toList()
        );
    }

    private FeedbackResponse toFeedbackResponse(FeedbackEntity feedback) {
        if (feedback == null) {
            return null;
        }
        return new FeedbackResponse(
                feedback.getUserRating(),
                new OpsReviewResponse(feedback.getOpsQuality(), feedback.getOpsNotes()),
                feedback.getAnnotations().stream()
                        .map(annotation -> new AnnotationResponse(
                                annotation.getType(),
                                annotation.getLabel(),
                                annotation.getAnnotatorId(),
                                annotation.getConfidence(),
                                annotation.getGroundTruth()))
                        .toList()
        );
    }

    private EvaluationSummaryResponse toEvaluationSummary(EvaluationEntity evaluation) {
        return new EvaluationSummaryResponse(
                evaluation.getEvaluationId(),
                evaluation.getOverallScore(),
                evaluation.getOverallConfidence(),
                evaluation.getNeedsHumanReview()
        );
    }
}
