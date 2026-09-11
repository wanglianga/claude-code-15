package com.rehab.platform.model;

import com.rehab.platform.enums.AdjustmentDecision;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 处方调整决策记录（维持 / 降低强度 / 追加线下复诊 / 提醒医生介入）
 */
@Data
@Entity
@Table(name = "prescription_adjustments")
public class PrescriptionAdjustment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "prescription_id", nullable = false)
    private Prescription prescription;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "therapist_id", nullable = false)
    private User therapist;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private AdjustmentDecision decision;

    /** 决策原因（基于哪些反馈数据） */
    @Column(columnDefinition = "TEXT")
    private String reason;

    /** 若生成了新处方，记录新处方 id */
    private Long newPrescriptionId;

    private LocalDateTime createdAt = LocalDateTime.now();
}
