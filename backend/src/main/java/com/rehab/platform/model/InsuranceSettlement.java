package com.rehab.platform.model;

import com.rehab.platform.enums.SettlementStatus;
import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 医保结算单：结合处方执行 + 复诊评估 + 线下治疗记录
 */
@Data
@Entity
@Table(name = "insurance_settlements")
public class InsuranceSettlement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    private LocalDate periodStart;

    private LocalDate periodEnd;

    /** 周期内训练依从性 % */
    private Integer adherencePercent;

    /** 居家训练可报销金额 */
    private BigDecimal homeTrainingAmount = BigDecimal.ZERO;

    /** 复诊评估费用 */
    private BigDecimal assessmentAmount = BigDecimal.ZERO;

    /** 线下治疗费用 */
    private BigDecimal outpatientAmount = BigDecimal.ZERO;

    /** 费用合计 */
    private BigDecimal totalAmount = BigDecimal.ZERO;

    /** 医保报销金额 */
    private BigDecimal reimbursableAmount = BigDecimal.ZERO;

    /** 个人自付金额 */
    private BigDecimal selfPayAmount = BigDecimal.ZERO;

    /** 明细 JSON */
    @Column(columnDefinition = "TEXT")
    private String detailJson;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SettlementStatus status = SettlementStatus.DRAFT;

    @Column(length = 64)
    private String createdBy;

    private LocalDateTime createdAt = LocalDateTime.now();
}
