package com.rehab.platform.model;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 线下（门诊）治疗记录，用于医保结算与居家训练衔接
 */
@Data
@Entity
@Table(name = "outpatient_treatments")
public class OutpatientTreatment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "therapist_id")
    private User therapist;

    private LocalDate treatmentDate;

    /** 治疗项目，如 运动疗法（门诊） */
    @Column(nullable = false, length = 128)
    private String itemName;

    @Column(length = 32)
    private String insuranceCode;

    @Column(nullable = false)
    private Boolean reimbursable = true;

    @Column(nullable = false)
    private BigDecimal amount = BigDecimal.ZERO;

    @Column(columnDefinition = "TEXT")
    private String note;

    private LocalDateTime createdAt = LocalDateTime.now();
}
