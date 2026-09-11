package com.rehab.platform.repository;

import com.rehab.platform.enums.DiseaseStage;
import com.rehab.platform.enums.DiseaseType;
import com.rehab.platform.model.Patient;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PatientRepository extends JpaRepository<Patient, Long> {
    List<Patient> findByTherapistIdOrderByCreatedAtDesc(Long therapistId);
    List<Patient> findByDiseaseStage(DiseaseStage stage);
    List<Patient> findByDiseaseType(DiseaseType type);
    Optional<Patient> findByFamilyUserId(Long familyUserId);
    Optional<Patient> findByPatientNo(String patientNo);
    List<Patient> findAllByOrderByCreatedAtDesc();
}
