package com.rehab.platform.repository;

import com.rehab.platform.model.PrescriptionAdjustment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PrescriptionAdjustmentRepository extends JpaRepository<PrescriptionAdjustment, Long> {
    List<PrescriptionAdjustment> findByPatientIdOrderByCreatedAtDesc(Long patientId);
    List<PrescriptionAdjustment> findByPrescriptionIdOrderByCreatedAtDesc(Long prescriptionId);
}
