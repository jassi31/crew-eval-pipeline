package com.crew.evalpipeline.evaluation.repository;

import com.crew.evalpipeline.evaluation.entity.EvaluationJobEntity;
import com.crew.evalpipeline.shared.DomainEnums.EvaluationJobStatus;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface EvaluationJobRepository extends JpaRepository<EvaluationJobEntity, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select j from EvaluationJobEntity j
            where j.status in :statuses
              and (j.nextRetryAt is null or j.nextRetryAt <= :now)
            order by j.createdAt asc
            """)
    List<EvaluationJobEntity> findClaimableJobs(Collection<EvaluationJobStatus> statuses, Instant now, Pageable pageable);

    List<EvaluationJobEntity> findByConversationConversationIdOrderByCreatedAtDesc(String conversationId);
}
