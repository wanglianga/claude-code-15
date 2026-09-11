package com.rehab.platform.repository;

import com.rehab.platform.model.Assessment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface AssessmentRepository extends JpaRepository<Assessment, Long> {
    List<Assessment> findByPatientIdOrderByAssessmentDateDesc(Long patientId);
    List<Assessment> findByPatientIdAndAssessmentDateBetween(Long patientId, LocalDate start, LocalDate end);
}
