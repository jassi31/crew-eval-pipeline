package com.crew.evalpipeline.evaluation.service;

import com.crew.evalpipeline.config.AppProperties;
import com.crew.evalpipeline.conversation.entity.ConversationEntity;
import com.crew.evalpipeline.conversation.repository.ConversationRepository;
import com.crew.evalpipeline.evaluation.entity.EvaluationJobEntity;
import com.crew.evalpipeline.evaluation.repository.EvaluationJobRepository;
import com.crew.evalpipeline.shared.DomainEnums.ConversationStatus;
import com.crew.evalpipeline.shared.DomainEnums.EvaluationJobStatus;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class EvaluationJobService {

    private final EvaluationJobRepository evaluationJobRepository;
    private final ConversationRepository conversationRepository;
    private final EvaluationService evaluationService;
    private final AppProperties appProperties;
    private final Clock clock;
    private final TransactionTemplate transactionTemplate;

    public EvaluationJobService(
            EvaluationJobRepository evaluationJobRepository,
            ConversationRepository conversationRepository,
            EvaluationService evaluationService,
            AppProperties appProperties,
            Clock clock,
            PlatformTransactionManager transactionManager
    ) {
        this.evaluationJobRepository = evaluationJobRepository;
        this.conversationRepository = conversationRepository;
        this.evaluationService = evaluationService;
        this.appProperties = appProperties;
        this.clock = clock;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    public EvaluationJobEntity enqueue(ConversationEntity conversation, String triggerSource) {
        EvaluationJobEntity job = new EvaluationJobEntity();
        job.setJobId(UUID.randomUUID().toString());
        job.setConversation(conversation);
        job.setStatus(EvaluationJobStatus.QUEUED);
        job.setAttempts(0);
        job.setTriggerSource(triggerSource);
        job.setNextRetryAt(Instant.now(clock));
        conversation.setStatus(ConversationStatus.QUEUED);
        conversationRepository.save(conversation);
        return evaluationJobRepository.save(job);
    }

    public EvaluationJobEntity enqueueByConversationId(String conversationId, String triggerSource) {
        ConversationEntity conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new com.crew.evalpipeline.api.error.ResourceNotFoundException("Conversation not found: " + conversationId));
        return enqueue(conversation, triggerSource);
    }

    @Scheduled(fixedDelayString = "${app.evaluation.job-poll-delay-ms:5000}")
    public void processPendingJobs() {
        List<String> claimedJobIds = transactionTemplate.execute(status -> claimPendingJobIds());
        if (claimedJobIds == null || claimedJobIds.isEmpty()) {
            return;
        }
        for (String jobId : claimedJobIds) {
            transactionTemplate.executeWithoutResult(status -> processClaimedJob(jobId));
        }
    }

    private List<String> claimPendingJobIds() {
        Instant now = Instant.now(clock);
        List<EvaluationJobEntity> jobs = evaluationJobRepository.findClaimableJobs(
                List.of(EvaluationJobStatus.QUEUED),
                now,
                PageRequest.of(0, appProperties.getEvaluation().getMaxJobClaimSize()));

        String lockOwner = UUID.randomUUID().toString();
        jobs.forEach(job -> {
            job.setStatus(EvaluationJobStatus.IN_PROGRESS);
            job.setAttempts(job.getAttempts() + 1);
            job.setLockedAt(now);
            job.setLockOwner(lockOwner);
            job.getConversation().setStatus(ConversationStatus.PROCESSING);
        });
        evaluationJobRepository.saveAll(jobs);
        return jobs.stream().map(EvaluationJobEntity::getJobId).toList();
    }

    private void processClaimedJob(String jobId) {
        EvaluationJobEntity job = evaluationJobRepository.findById(jobId)
                .orElseThrow(() -> new com.crew.evalpipeline.api.error.ResourceNotFoundException("Evaluation job not found: " + jobId));
        try {
            evaluationService.evaluateConversation(job.getConversation().getConversationId());
            job.setStatus(EvaluationJobStatus.COMPLETED);
            job.setFailureReason(null);
            job.setNextRetryAt(null);
            job.getConversation().setStatus(ConversationStatus.EVALUATED);
        } catch (Exception exception) {
            boolean canRetry = job.getAttempts() < appProperties.getEvaluation().getMaxRetries();
            if (canRetry) {
                job.setStatus(EvaluationJobStatus.QUEUED);
                job.setNextRetryAt(Instant.now(clock).plusSeconds(30));
            } else {
                job.setStatus(EvaluationJobStatus.FAILED);
                job.getConversation().setStatus(ConversationStatus.FAILED);
            }
            job.setFailureReason(exception.getMessage());
        }
        evaluationJobRepository.save(job);
    }
}
