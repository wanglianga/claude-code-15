package com.rehab.platform.model;

import com.rehab.platform.enums.HandoverStatus;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 照护人更换交接单：
 * 主要陪练家属更换时，新照护人必须完成 动作注意事项 / 禁忌风险 / 器具使用 三项确认；
 * 旧照护人反馈记录保留在患者档案；交接后首周新照护人反馈重点标记，护士可追加电话指导。
 */
@Data
@Entity
@Table(name = "caregiver_handovers", indexes = @Index(columnList = "patient_id, status"))
public class CaregiverHandover {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    // ---------- 旧照护人快照（更换前的陪练家属） ----------

    @Column(length = 64)
    private String oldCaregiverName;

    @Column(length = 32)
    private String oldCaregiverRelation;

    /** 旧照护人绑定的家属账号 id（可空） */
    private Long oldFamilyUserId;

    @Column(length = 64)
    private String oldFamilyUserName;

    // ---------- 新照护人 ----------

    @Column(nullable = false, length = 64)
    private String newCaregiverName;

    @Column(length = 32)
    private String newCaregiverRelation;

    @Column(length = 32)
    private String newCaregiverPhone;

    /** 新照护人绑定的家属账号（确认后以其身份打卡） */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "new_family_user_id")
    private User newFamilyUser;

    /** 更换原因 */
    @Column(columnDefinition = "TEXT")
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private HandoverStatus status = HandoverStatus.PENDING_CONFIRM;

    // ---------- 新照护人三项确认 ----------

    /** 动作注意事项已确认 */
    private Boolean precautionsConfirmed = false;

    /** 禁忌风险已确认 */
    private Boolean contraindicationsConfirmed = false;

    /** 器具使用已确认 */
    private Boolean devicesConfirmed = false;

    @Column(length = 64)
    private String confirmedBy;

    private LocalDateTime confirmedAt;

    /** 首周重点观察期截止日（确认之日起 7 天） */
    private LocalDate firstWeekEnd;

    // ---------- 护士电话指导 ----------

    @Column(columnDefinition = "TEXT")
    private String nurseGuidanceNote;

    @Column(length = 64)
    private String nurseGuidanceBy;

    private LocalDateTime nurseGuidanceAt;

    @Column(length = 64)
    private String createdBy;

    private LocalDateTime createdAt = LocalDateTime.now();
}
