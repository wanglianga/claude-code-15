package com.rehab.platform.model;

import com.rehab.platform.enums.PrescriptionStatus;
import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 居家训练处方（按阶段）
 */
@Data
@Entity
@Table(name = "prescriptions")
public class Prescription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "therapist_id", nullable = false)
    private User therapist;

    /** 基于哪次评估开具（可空） */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "assessment_id")
    private Assessment assessment;

    /** 第几阶段 */
    @Column(nullable = false)
    private Integer phase = 1;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PrescriptionStatus status = PrescriptionStatus.ACTIVE;

    private LocalDate startDate;

    private LocalDate endDate;

    /** 计划复诊日期 */
    private LocalDate nextReviewDate;

    /** 整体疼痛阈值（VAS），超过即触发预警 */
    private Integer painThreshold = 6;

    @Column(columnDefinition = "TEXT")
    private String notes;

    /** 本处方生成原因（为什么开/为什么调整），用于时间线解释 */
    @Column(columnDefinition = "TEXT")
    private String adjustReason;

    @ToString.Exclude
    @lombok.EqualsAndHashCode.Exclude
    @OneToMany(mappedBy = "prescription", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("id ASC")
    private List<PrescriptionItem> items = new ArrayList<>();

    private LocalDateTime createdAt = LocalDateTime.now();

    public void addItem(PrescriptionItem item) {
        item.setPrescription(this);
        this.items.add(item);
    }
}
