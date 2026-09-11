package com.rehab.platform.repository;

import com.rehab.platform.enums.EscalationStatus;
import com.rehab.platform.model.PainEscalation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface PainEscalationRepository extends JpaRepository<PainEscalation, Long> {

    List<PainEscalation> findByPatientIdOrderByCreatedAtDesc(Long patientId);

    List<PainEscalation> findByPatientIdAndStatusNotOrderByCreatedAtDesc(Long patientId, EscalationStatus status);

    List<PainEscalation> findByStatusOrderByCreatedAtDesc(EscalationStatus status);

    List<PainEscalation> findAllByOrderByCreatedAtDesc();

    List<PainEscalation> findByPatientIdAndCreatedAtBetween(Long patientId, LocalDateTime from, LocalDateTime to);

    long countByStatusNot(EscalationStatus status);
}
