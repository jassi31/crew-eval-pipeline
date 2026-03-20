package com.crew.evalpipeline.suggestion.repository;

import com.crew.evalpipeline.shared.DomainEnums.SuggestionScope;
import com.crew.evalpipeline.shared.DomainEnums.SuggestionStatus;
import com.crew.evalpipeline.suggestion.entity.ImprovementSuggestionEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ImprovementSuggestionRepository extends JpaRepository<ImprovementSuggestionEntity, String>, JpaSpecificationExecutor<ImprovementSuggestionEntity> {

    Optional<ImprovementSuggestionEntity> findByScopeAndTargetTypeAndTargetKeyAndAgentVersionAndStatus(
            SuggestionScope scope,
            String targetType,
            String targetKey,
            String agentVersion,
            SuggestionStatus status
    );
}
