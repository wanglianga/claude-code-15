package com.rehab.platform.model;

import com.rehab.platform.enums.CorrectionStatus;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 动作视频纠错任务：
 * 治疗师打回某段训练视频并标注关键动作点 → 患者下次训练前必须确认观看 →
 * 下一次视频自动关联对比 → 治疗师复评是否真正掌握；连续未掌握触发线下复评/降强度。
 */
@Data
@Entity
@Table(name = "correction_tasks", indexes = @Index(columnList = "patient_id, status"))
public class CorrectionTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    /** 被打回的视频所在打卡记录 */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "source_log_id", nullable = false)
    private TrainingLog sourceLog;

    /** 关联训练项目（哪个动作） */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "prescription_item_id")
    private PrescriptionItem prescriptionItem;

    /** 关键动作点 JSON：[{"code":"KNEE_ANGLE","label":"膝关节角度","note":"屈曲过大","timestamp":"00:12"}] */
    @Column(columnDefinition = "TEXT")
    private String keyPoints;

    /** 纠错说明 */
    @Column(columnDefinition = "TEXT", nullable = false)
    private String correctionNote;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CorrectionStatus status = CorrectionStatus.PENDING_CONFIRM;

    /** 连续复评未掌握次数 */
    @Column(nullable = false)
    private Integer consecutiveErrors = 0;

    /** 患者/家属确认观看 */
    @Column(length = 64)
    private String confirmedBy;

    private LocalDateTime confirmedAt;

    /** 用于对比复评的新打卡视频 */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "recheck_log_id")
    private TrainingLog recheckLog;

    /** 复评结论 */
    @Column(columnDefinition = "TEXT")
    private String reviewNote;

    @Column(length = 64)
    private String reviewedBy;

    private LocalDateTime reviewedAt;

    private LocalDateTime createdAt = LocalDateTime.now();
}
