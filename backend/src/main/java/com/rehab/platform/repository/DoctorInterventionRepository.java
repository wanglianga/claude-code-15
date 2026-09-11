package com.rehab.platform.repository;

import com.rehab.platform.model.DoctorIntervention;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DoctorInterventionRepository extends JpaRepository<DoctorIntervention, Long> {
    List<DoctorIntervention> findByPatientIdOrderByCreatedAtDesc(Long patientId);
}
