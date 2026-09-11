package com.rehab.platform.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 居家训练每日打卡记录
 */
@Data
@Entity
@Table(name = "training_logs", uniqueConstraints = @UniqueConstraint(columnNames = {"patient_id", "log_date"}))
public class TrainingLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "prescription_id", nullable = false)
    private Prescription prescription;

    @Column(name = "log_date", nullable = false)
    private LocalDate logDate;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "submitted_by")
    private User submittedBy;

    /** 完成率 0-100 */
    @Column(nullable = false)
    private Integer completionRate = 0;

    /** 各项目完成情况 JSON：[{"itemId":1,"done":true,"actualSets":3,"actualReps":10}] */
    @Column(columnDefinition = "TEXT")
    private String completedDetail;

    /** 训练前疼痛 VAS */
    private Integer painBefore;

    /** 训练后疼痛 VAS */
    private Integer painAfter;

    /** 是否出现肿胀/麻木 */
    private Boolean swellingNumbness = false;

    /** 夜间疼痛是否加重 */
    private Boolean nightPainWorse = false;

    /** 异常照片路径 JSON 数组 */
    @Column(columnDefinition = "TEXT")
    private String abnormalPhotos;

    /** 训练视频片段路径 JSON 数组 */
    @Column(columnDefinition = "TEXT")
    private String videoClips;

    /** 家属备注 */
    @Column(columnDefinition = "TEXT")
    private String familyNote;

    /** 家属能否陪练 */
    private Boolean companionAvailable = true;

    /** 是否观察到明显动作代偿 */
    private Boolean compensationObserved = false;

    /** 治疗师纠错/反馈（视频纠错记录） */
    @Column(columnDefinition = "TEXT")
    private String therapistFeedback;

    @Column(length = 64)
    private String feedbackBy;

    private LocalDateTime feedbackAt;

    private LocalDateTime createdAt = LocalDateTime.now();
}
