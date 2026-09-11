package com.rehab.platform.repository;

import com.rehab.platform.model.InsuranceSettlement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InsuranceSettlementRepository extends JpaRepository<InsuranceSettlement, Long> {
    List<InsuranceSettlement> findByPatientIdOrderByCreatedAtDesc(Long patientId);
}
