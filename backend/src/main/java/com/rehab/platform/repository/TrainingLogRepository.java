package com.rehab.platform.repository;

import com.rehab.platform.model.TrainingLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TrainingLogRepository extends JpaRepository<TrainingLog, Long> {
    List<TrainingLog> findByPatientIdOrderByLogDateDesc(Long patientId);
    List<TrainingLog> findByPatientIdAndLogDateBetweenOrderByLogDateAsc(Long patientId, LocalDate start, LocalDate end);
    Optional<TrainingLog> findByPatientIdAndLogDate(Long patientId, LocalDate logDate);
    List<TrainingLog> findByPatientIdAndTherapistFeedbackIsNotNullOrderByLogDateDesc(Long patientId);
}
