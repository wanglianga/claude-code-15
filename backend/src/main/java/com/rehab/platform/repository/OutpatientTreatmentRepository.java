package com.rehab.platform.repository;

import com.rehab.platform.model.OutpatientTreatment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface OutpatientTreatmentRepository extends JpaRepository<OutpatientTreatment, Long> {
    List<OutpatientTreatment> findByPatientIdOrderByTreatmentDateDesc(Long patientId);
    List<OutpatientTreatment> findByPatientIdAndTreatmentDateBetween(Long patientId, LocalDate start, LocalDate end);
}
