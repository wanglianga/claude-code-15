package com.rehab.platform.repository;

import com.rehab.platform.model.NurseFollowup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NurseFollowupRepository extends JpaRepository<NurseFollowup, Long> {
    List<NurseFollowup> findByAlertIdOrderByCreatedAtDesc(Long alertId);
    List<NurseFollowup> findByPatientIdOrderByCreatedAtDesc(Long patientId);
}
