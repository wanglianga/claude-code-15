package com.rehab.platform.model;

import com.rehab.platform.enums.AssessmentType;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 康复评估（初评 / 复诊评估）
 */
@Data
@Entity
@Table(name = "assessments")
public class Assessment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "therapist_id", nullable = false)
    private User therapist;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AssessmentType type;

    /** 关节活动度 JSON，如 {"肩关节前屈":120,"膝关节屈曲":95} */
    @Column(columnDefinition = "TEXT")
    private String romJson;

    /** 肌力（0-5 级） */
    private Integer muscleStrength;

    /** 疼痛评分 VAS 0-10 */
    private Integer painScore;

    /** 日常生活能力 ADL 评分 0-100 */
    private Integer adlScore;

    /** 步态视频路径 */
    @Column(length = 255)
    private String gaitVideoPath;

    @Column(columnDefinition = "TEXT")
    private String notes;

    private LocalDate assessmentDate;

    private LocalDateTime createdAt = LocalDateTime.now();
}
