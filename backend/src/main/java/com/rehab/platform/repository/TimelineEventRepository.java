package com.rehab.platform.repository;

import com.rehab.platform.model.TimelineEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TimelineEventRepository extends JpaRepository<TimelineEvent, Long> {
    List<TimelineEvent> findByPatientIdOrderByCreatedAtDesc(Long patientId);
}
