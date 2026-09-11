package com.rehab.platform.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;

import java.math.BigDecimal;

/**
 * 处方训练项目（一个动作）
 */
@Data
@Entity
@Table(name = "prescription_items")
public class PrescriptionItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ToString.Exclude
    @lombok.EqualsAndHashCode.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prescription_id", nullable = false)
    private Prescription prescription;

    /** 动作名称，如 桥式运动 */
    @Column(nullable = false, length = 128)
    private String exerciseName;

    /** 组数 */
    private Integer targetSets;

    /** 每组次数 */
    private Integer targetReps;

    /** 每日几次 */
    private Integer frequencyPerDay = 1;

    /** 禁忌动作 */
    @Column(columnDefinition = "TEXT")
    private String contraindication;

    /** 辅助器具 */
    @Column(length = 128)
    private String assistiveDevice;

    /** 该项疼痛阈值（VAS），超过应停止 */
    private Integer painThreshold;

    /** 家属观察点 */
    @Column(columnDefinition = "TEXT")
    private String familyObservation;

    /** 动作示范视频 */
    @Column(length = 255)
    private String demoVideoPath;

    /** 是否医保可报销 */
    @Column(nullable = false)
    private Boolean reimbursable = false;

    /** 医保项目编码 */
    @Column(length = 32)
    private String insuranceCode;

    /** 单价（元/次） */
    private BigDecimal unitPrice = BigDecimal.ZERO;
}
