package com.rehab.platform.model;

import com.rehab.platform.enums.DiseaseStage;
import com.rehab.platform.enums.DiseaseType;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "patients")
public class Patient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 档案编号，如 P2026001 */
    @Column(nullable = false, unique = true, length = 32)
    private String patientNo;

    @Column(nullable = false, length = 64)
    private String name;

    @Column(length = 8)
    private String gender;

    private LocalDate birthDate;

    @Column(length = 32)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private DiseaseType diseaseType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private DiseaseStage diseaseStage = DiseaseStage.NEWLY_DISCHARGED;

    /** 诊断 */
    @Column(columnDefinition = "TEXT")
    private String diagnosis;

    /** 家庭照护人 */
    @Column(length = 64)
    private String caregiverName;

    @Column(length = 32)
    private String caregiverRelation;

    @Column(length = 32)
    private String caregiverPhone;

    @Column(length = 255)
    private String address;

    /** 医保类型：职工医保 / 城乡居民医保 / 新农合 / 自费 */
    @Column(length = 32)
    private String insuranceType;

    @Column(length = 64)
    private String insuranceNo;

    /** 出院日期（刚出院阶段计算用） */
    private LocalDate dischargeDate;

    /** 下次复诊日期 */
    private LocalDate nextReviewDate;

    /** 负责治疗师 */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "therapist_id")
    private User therapist;

    /** 家属登录账号 */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "family_user_id")
    private User familyUser;

    private LocalDateTime createdAt = LocalDateTime.now();
}
