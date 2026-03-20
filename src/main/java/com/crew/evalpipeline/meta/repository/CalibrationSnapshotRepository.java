package com.crew.evalpipeline.meta.repository;

import com.crew.evalpipeline.meta.entity.CalibrationSnapshotEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CalibrationSnapshotRepository extends JpaRepository<CalibrationSnapshotEntity, Long> {

    Optional<CalibrationSnapshotEntity> findTopByOrderByCreatedAtDesc();
}
