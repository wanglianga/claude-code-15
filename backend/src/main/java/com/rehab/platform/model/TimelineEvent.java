package com.rehab.platform.model;

import com.rehab.platform.enums.TimelineEventType;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 患者时间线事件：串联治疗师、护士、家属、医生的全部处理记录
 */
@Data
@Entity
@Table(name = "timeline_events", indexes = @Index(columnList = "patient_id, created_at"))
public class TimelineEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private TimelineEventType eventType;

    private Long actorId;

    @Column(length = 64)
    private String actorName;

    @Column(length = 20)
    private String actorRole;

    @Column(nullable = false, length = 200)
    private String title;

    /** 事件说明（为什么发生） */
    @Column(columnDefinition = "TEXT")
    private String content;

    /** 关联业务对象 */
    @Column(length = 40)
    private String refType;

    private Long refId;

    private LocalDateTime createdAt = LocalDateTime.now();
}
