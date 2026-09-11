package com.rehab.platform.repository;

import com.rehab.platform.enums.CorrectionStatus;
import com.rehab.platform.model.CorrectionTask;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CorrectionTaskRepository extends JpaRepository<CorrectionTask, Long> {
    List<CorrectionTask> findByPatientIdOrderByCreatedAtDesc(Long patientId);
    List<CorrectionTask> findByPatientIdAndStatus(Long patientId, CorrectionStatus status);
    List<CorrectionTask> findByPatientIdAndStatusIn(Long patientId, List<CorrectionStatus> statuses);
}
