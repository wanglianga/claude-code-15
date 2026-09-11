package com.rehab.platform.repository;

import com.rehab.platform.enums.AlertStatus;
import com.rehab.platform.enums.AlertType;
import com.rehab.platform.model.Alert;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AlertRepository extends JpaRepository<Alert, Long> {
    List<Alert> findByPatientIdOrderByCreatedAtDesc(Long patientId);
    List<Alert> findByStatusOrderByCreatedAtDesc(AlertStatus status);
    List<Alert> findByStatusInOrderByCreatedAtDesc(List<AlertStatus> statuses);
    List<Alert> findByPatientIdAndTypeAndStatusIn(Long patientId, AlertType type, List<AlertStatus> statuses);
    List<Alert> findByPatientIdAndStatusIn(Long patientId, List<AlertStatus> statuses);
    List<Alert> findAllByOrderByCreatedAtDesc();
}
