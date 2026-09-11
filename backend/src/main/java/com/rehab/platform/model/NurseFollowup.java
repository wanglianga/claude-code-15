package com.rehab.platform.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 护士电话随访记录
 */
@Data
@Entity
@Table(name = "nurse_followups")
public class NurseFollowup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "alert_id", nullable = false)
    private Alert alert;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "nurse_id", nullable = false)
    private User nurse;

    private LocalDateTime followTime = LocalDateTime.now();

    /** 随访方式：电话 / 视频 / 上门 */
    @Column(length = 20)
    private String method = "电话";

    @Column(columnDefinition = "TEXT")
    private String content;

    /** 随访结果：已恢复训练 / 建议降低强度 / 建议线下复诊 / 需医生介入 / 无法接通 */
    @Column(length = 64)
    private String outcome;

    @Column(columnDefinition = "TEXT")
    private String nextAction;

    private LocalDateTime createdAt = LocalDateTime.now();
}
