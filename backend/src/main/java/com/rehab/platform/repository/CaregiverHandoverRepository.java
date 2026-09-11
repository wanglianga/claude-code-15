package com.rehab.platform.repository;

import com.rehab.platform.enums.HandoverStatus;
import com.rehab.platform.model.CaregiverHandover;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface CaregiverHandoverRepository extends JpaRepository<CaregiverHandover, Long> {

    List<CaregiverHandover> findByPatientIdOrderByCreatedAtDesc(Long patientId);

    Optional<CaregiverHandover> findFirstByPatientIdAndStatusOrderByCreatedAtDesc(Long patientId, HandoverStatus status);

    /** 首周观察期内的新照护人交接（确认完成且首周未结束） */
    List<CaregiverHandover> findByStatusAndFirstWeekEndGreaterThanEqualOrderByConfirmedAtDesc(
            HandoverStatus status, LocalDate date);
}
