package com.rehab.platform.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.rehab.platform.enums.EscalationStatus;
import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 疼痛升级处置单：
 * 训练后疼痛超阈值 / 肿胀麻木 / 夜间疼痛加重 → 平台暂停相关动作 →
 * 家属补充症状/用药/是否摔倒 → 护士电话评估 → 医生复核处置（休息/冰敷/影像检查/门诊复诊）→
 * 风险解除前相关动作不出现在每日训练任务中。
 */
@Data
@Entity
@Table(name = "pain_escalations", indexes = @Index(columnList = "patient_id, status"))
public class PainEscalation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    /** 触发时执行中的处方 */
    @JsonIgnore
    @ToString.Exclude
    @lombok.EqualsAndHashCode.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prescription_id")
    private Prescription prescription;

    /** 来源打卡记录 */
    @JsonIgnore
    @ToString.Exclude
    @lombok.EqualsAndHashCode.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "training_log_id")
    private TrainingLog trainingLog;

    /** 推送护士随访队列的关联预警 */
    @JsonIgnore
    @ToString.Exclude
    @lombok.EqualsAndHashCode.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "alert_id")
    private Alert alert;

    /** 触发原因（逗号分隔 EscalationTrigger） */
    @Column(nullable = false, length = 120)
    private String triggers;

    /** 触发时训练后疼痛 VAS */
    private Integer painScore;

    /** 被暂停的处方项目 id（JSON 数组） */
    @Column(columnDefinition = "TEXT")
    private String suspendedItemIds;

    /** 被暂停的动作名称（逗号分隔，便于展示与医保标注） */
    @Column(length = 255)
    private String suspendedItemNames;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EscalationStatus status = EscalationStatus.PENDING_FAMILY_INFO;

    // ---------- 家属补充 ----------

    /** 症状补充（肿胀/麻木部位、疼痛规律等） */
    @Column(columnDefinition = "TEXT")
    private String familySymptoms;

    /** 用药情况 */
    @Column(columnDefinition = "TEXT")
    private String familyMedication;

    /** 是否摔倒 */
    private Boolean familyFell;

    /** 摔倒经过说明 */
    @Column(columnDefinition = "TEXT")
    private String familyFellDetail;

    @Column(length = 64)
    private String familyReportedBy;

    private LocalDateTime familyReportedAt;

    // ---------- 护士电话评估 ----------

    @Column(columnDefinition = "TEXT")
    private String nurseAssessment;

    /** 护士结论：OBSERVE_RESUME 继续观察恢复训练 / ESCALATE_DOCTOR 转医生复核 */
    @Column(length = 30)
    private String nurseDecision;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "nurse_id")
    private User nurse;

    private LocalDateTime nurseAssessedAt;

    // ---------- 医生复核处置 ----------

    /** 处置方式（逗号分隔 Disposition：REST/ICE/IMAGING/OUTPATIENT） */
    @Column(length = 120)
    private String doctorDispositions;

    /** 医生介入结论（同步治疗师与家属） */
    @Column(columnDefinition = "TEXT")
    private String doctorConclusion;

    /** 门诊复诊/影像检查预约日期 */
    private LocalDate reviewDate;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "doctor_id")
    private User doctor;

    private LocalDateTime doctorReviewedAt;

    // ---------- 风险解除 ----------

    @Column(length = 64)
    private String clearedBy;

    private LocalDateTime clearedAt;

    @Column(columnDefinition = "TEXT")
    private String clearNote;

    private LocalDateTime createdAt = LocalDateTime.now();

    @JsonProperty("prescriptionId")
    public Long getPrescriptionId() {
        return prescription == null ? null : prescription.getId();
    }

    @JsonProperty("sourceLogId")
    public Long getSourceLogId() {
        return trainingLog == null ? null : trainingLog.getId();
    }

    @JsonProperty("sourceLogDate")
    public LocalDate getSourceLogDate() {
        return trainingLog == null ? null : trainingLog.getLogDate();
    }

    @JsonProperty("alertId")
    public Long getAlertId() {
        return alert == null ? null : alert.getId();
    }
}
