package com.rehab.platform.repository;

import com.rehab.platform.enums.PrescriptionStatus;
import com.rehab.platform.model.Prescription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PrescriptionRepository extends JpaRepository<Prescription, Long> {
    List<Prescription> findByPatientIdOrderByCreatedAtDesc(Long patientId);
    Optional<Prescription> findByPatientIdAndStatus(Long patientId, PrescriptionStatus status);
    List<Prescription> findByStatus(PrescriptionStatus status);
}
