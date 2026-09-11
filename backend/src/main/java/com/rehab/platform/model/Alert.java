package com.rehab.platform.model;

import com.rehab.platform.enums.AlertLevel;
import com.rehab.platform.enums.AlertStatus;
import com.rehab.platform.enums.AlertType;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 风险预警（推送康复护士电话随访）
 */
@Data
@Entity
@Table(name = "alerts")
public class Alert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private AlertType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AlertLevel level = AlertLevel.MEDIUM;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AlertStatus status = AlertStatus.PENDING;

    @Column(columnDefinition = "TEXT")
    private String message;

    /** 负责随访的护士 */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "nurse_id")
    private User nurse;

    /** 处理医生 */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "doctor_id")
    private User doctor;

    /** 来源打卡记录（可空） */
    @Column(name = "source_log_id")
    private Long sourceLogId;

    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime handledAt;

    private LocalDateTime resolvedAt;
}
