package com.rehab.platform.config;

import com.rehab.platform.enums.*;
import com.rehab.platform.model.*;
import com.rehab.platform.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 演示数据初始化：5 位患者覆盖 4 类疾病、3 个疾病阶段，
 * 含评估、分阶段处方、打卡、预警、随访、医生介入、线下治疗与医保结算的完整故事线。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PatientRepository patientRepository;
    private final AssessmentRepository assessmentRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final TrainingLogRepository trainingLogRepository;
    private final AlertRepository alertRepository;
    private final NurseFollowupRepository followupRepository;
    private final PrescriptionAdjustmentRepository adjustmentRepository;
    private final DoctorInterventionRepository interventionRepository;
    private final OutpatientTreatmentRepository treatmentRepository;
    private final InsuranceSettlementRepository settlementRepository;
    private final TimelineEventRepository timelineRepository;
    private final CorrectionTaskRepository correctionTaskRepository;
    private final PainEscalationRepository painEscalationRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.upload-dir}")
    private String uploadDir;

    private User admin, therapist, therapist2, nurse, doctor, family, family2, family3, family4, family5;

    @Override
    public void run(String... args) throws Exception {
        if (userRepository.count() > 0) {
            log.info("数据库已有数据，跳过演示数据初始化");
            return;
        }
        log.info("初始化演示数据...");
        seedUsers();
        seedPlaceholderPhoto();

        Patient p1 = seedPatient1();
        Patient p2 = seedPatient2();
        seedPatient3();
        seedPatient4();
        Patient p5 = seedPatient5();
        log.info("演示数据初始化完成：{} 位患者", patientRepository.count());
    }

    // ---------------- 用户 ----------------

    private void seedUsers() {
        admin = user("admin", "系统管理员", Role.ADMIN, "平台管理员", "0571-88000001");
        therapist = user("therapist", "王慧敏", Role.THERAPIST, "主管康复治疗师", "0571-88000002");
        therapist2 = user("therapist2", "李国强", Role.THERAPIST, "康复治疗师", "0571-88000003");
        nurse = user("nurse", "赵晓燕", Role.NURSE, "康复专科护士", "0571-88000004");
        doctor = user("doctor", "陈建军", Role.DOCTOR, "康复医学科主治医师", "0571-88000005");
        family = user("family", "张丽", Role.FAMILY, "患者张建国家属（女儿）", "13800000001");
        family2 = user("family2", "王大海", Role.FAMILY, "患者刘桂芳家属（丈夫）", "13800000002");
        family3 = user("family3", "王强", Role.FAMILY, "患者本人（自我管理）", "13905710003");
        family4 = user("family4", "刘敏", Role.FAMILY, "患者陈晨家属（母亲）", "13905710004");
        family5 = user("family5", "吴强", Role.FAMILY, "患者周淑英家属（儿子）", "13905710005");
    }

    private User user(String username, String name, Role role, String title, String phone) {
        User u = new User();
        u.setUsername(username);
        u.setPassword(passwordEncoder.encode("123456"));
        u.setName(name);
        u.setRole(role);
        u.setTitle(title);
        u.setPhone(phone);
        return userRepository.save(u);
    }

    private void seedPlaceholderPhoto() throws Exception {
        Path root = Paths.get(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(root);
        String svg = "<svg xmlns='http://www.w3.org/2000/svg' width='320' height='240'>"
                + "<rect width='100%' height='100%' fill='#fdecea'/>"
                + "<circle cx='160' cy='100' r='46' fill='#e57373'/>"
                + "<text x='160' y='180' font-size='18' text-anchor='middle' fill='#b71c1c'>异常照片示例：患肢皮肤红肿</text>"
                + "</svg>";
        Files.writeString(root.resolve("seed-redness.svg"), svg);
    }

    // ---------------- 患者1：张建国（脑卒中·刚出院，有完整故事线） ----------------

    private Patient seedPatient1() {
        LocalDate today = LocalDate.now();
        Patient p = new Patient();
        p.setPatientNo("P20260001");
        p.setName("张建国");
        p.setGender("男");
        p.setBirthDate(LocalDate.of(1964, 3, 12));
        p.setPhone("13905710001");
        p.setDiseaseType(DiseaseType.STROKE);
        p.setDiseaseStage(DiseaseStage.NEWLY_DISCHARGED);
        p.setDiagnosis("左侧基底节区脑梗死，右侧肢体偏瘫（Brunnstrom III 期）");
        p.setCaregiverName("张丽");
        p.setCaregiverRelation("女儿");
        p.setCaregiverPhone("13800000001");
        p.setAddress("城关镇建设路 12 号 3 单元 502");
        p.setInsuranceType("城乡居民医保");
        p.setInsuranceNo("YB3301020001");
        p.setDischargeDate(today.minusDays(18));
        p.setNextReviewDate(today.plusDays(4));
        p.setTherapist(therapist);
        p.setFamilyUser(family);
        p.setCreatedAt(today.minusDays(18).atTime(9, 0));
        patientRepository.save(p);
        tl(p, TimelineEventType.PATIENT_CREATED, therapist, "建立患者档案",
                "诊断：左侧基底节区脑梗死，右侧肢体偏瘫；疾病类型：脑卒中；初始阶段：刚出院",
                "patient", p.getId(), today.minusDays(18).atTime(9, 0));

        // 初次评估
        Assessment a1 = assessment(p, therapist, AssessmentType.INITIAL,
                "{\"肩关节前屈\": 90, \"肘关节伸展\": -20, \"腕关节背伸\": 30, \"髋关节屈曲\": 80, \"踝关节背屈\": 0}",
                3, 4, 45, null,
                "右侧偏瘫，坐位平衡 1 级，需中度辅助完成转移；建议居家训练以床上运动与坐位平衡为主。",
                today.minusDays(18));

        // 第一阶段处方（已被替代）
        Prescription rx1 = prescription(p, therapist, a1, 1, PrescriptionStatus.SUPERSEDED,
                today.minusDays(18), today.minusDays(4), today.minusDays(4), 6,
                "出院后第一阶段，以床上训练为主", "出院带训，建立居家训练常规",
                List.of(
                        item("桥式运动", 3, 10, 2, "避免憋气与颈部过度用力", "无", 5,
                                "是否耸肩代偿、面色苍白、气促", true, "340200020", "20"),
                        item("患侧上肢被动关节活动", 2, 15, 2, "避免暴力牵拉肩关节", "弹力带", 4,
                                "肩关节疼痛、半脱位迹象", true, "340200026", "15"),
                        item("坐位平衡训练", 3, 5, 1, "无保护下禁止单独训练", "平衡垫", 6,
                                "身体向患侧倾倒程度", false, null, "0"),
                        item("扶助行器步行训练", 1, 10, 1, "地面湿滑禁止训练", "四脚助行器", 6,
                                "步态对称性、足下垂", true, "340200023", "25")
                ), today.minusDays(18).atTime(10, 0));

        // 打卡：-17 ~ -1 天，第 -8 天疼痛升高
        TrainingLog logD5 = null, logD3 = null, logD1 = null;
        for (int d = 17; d >= 1; d--) {
            LocalDate date = today.minusDays(d);
            int painBefore = 2 + (d % 3);
            int painAfter = painBefore + (d % 2);
            boolean compensation = false;
            if (d == 8) {
                painBefore = 4;
                painAfter = 7;
            }
            TrainingLog log = trainingLog(p, rx1, date, 60 + (17 - d) * 2 > 100 ? 100 : 60 + (17 - d) * 2,
                    painBefore, painAfter,
                    d == 8 ? "[\"seed-redness.svg\"]" : null,
                    d == 8 ? "今天练完桥式运动后右肩很疼，皮肤有点红" : (d % 5 == 0 ? "完成得不错，精神可以" : null),
                    true, compensation);
            if (d == 5) {
                logD5 = log;
            }
            if (d == 3) {
                logD3 = log;
            }
            if (d == 1) {
                logD1 = log;
            }
            if (d == 8) {
                // 疼痛升高 → 预警 → 护士随访 → 治疗师降低强度
                Alert alert = alert(p, AlertType.PAIN_RISE, AlertLevel.MEDIUM, AlertStatus.RESOLVED,
                        "训练后疼痛 7 分，达到/超过处方疼痛阈值 6 分", log.getId(),
                        date.atTime(20, 5), nurse, date.atTime(21, 0), date.plusDays(1).atTime(9, 0));
                NurseFollowup f = followup(alert, p, "电话回访：患者诉桥式运动后右肩疼痛明显，伴皮肤发红。指导暂停桥式运动 1 天，局部冷敷，明日复评疼痛。",
                        "建议降低强度", "已将情况反馈王慧敏治疗师，等待处方调整", date.atTime(21, 0));
                tl(p, TimelineEventType.ALERT_CREATED, null, "触发预警：疼痛升高（中风险）",
                        "训练后疼痛 7 分，达到/超过处方疼痛阈值 6 分。已推送康复护士电话随访队列。",
                        "alert", alert.getId(), date.atTime(20, 5));
                tl(p, TimelineEventType.ALERT_ASSIGNED, nurse, "护士接单：疼痛升高",
                        "赵晓燕 开始电话随访。", "alert", alert.getId(), date.atTime(20, 30));
                tl(p, TimelineEventType.NURSE_FOLLOWUP, nurse, "电话随访（疼痛升高）：建议降低强度",
                        "随访内容：" + f.getContent() + "；后续安排：" + f.getNextAction(),
                        "followup", f.getId(), date.atTime(21, 0));
                PrescriptionAdjustment adj = adjustment(p, rx1, therapist, AdjustmentDecision.REDUCE_INTENSITY,
                        "第 " + date + " 打卡疼痛升至 7 分并伴皮肤发红，护士随访确认。将桥式运动由 3 组×10 次降为 2 组×8 次，疼痛阈值下调至 5 分。",
                        date.plusDays(1).atTime(9, 30));
                tl(p, TimelineEventType.PRESCRIPTION_ADJUSTED, therapist, "处方决策：降低训练强度",
                        "依据：" + adj.getReason(), "adjustment", adj.getId(), date.plusDays(1).atTime(9, 30));
                tl(p, TimelineEventType.ALERT_RESOLVED, nurse, "预警解除：疼痛升高",
                        alert.getMessage(), "alert", alert.getId(), date.plusDays(1).atTime(9, 0));

                // 疼痛升级处置（完整闭环：暂停动作 → 家属补充 → 护士评估转医生 → 医生处置 → 风险解除）
                PainEscalation esc = new PainEscalation();
                esc.setPatient(p);
                esc.setPrescription(rx1);
                esc.setTrainingLog(log);
                esc.setAlert(alert);
                esc.setTriggers("PAIN_OVER_THRESHOLD");
                esc.setPainScore(7);
                esc.setSuspendedItemIds("[" + rx1.getItems().get(0).getId() + "]");
                esc.setSuspendedItemNames("桥式运动");
                esc.setStatus(EscalationStatus.CLEARED);
                esc.setFamilySymptoms("练完桥式运动后右肩疼痛明显，皮肤发红，无肿胀麻木，夜间不痛");
                esc.setFamilyMedication("外用双氯芬酸二乙胺乳膏（扶他林），未口服止痛药");
                esc.setFamilyFell(false);
                esc.setFamilyReportedBy(family.getName());
                esc.setFamilyReportedAt(date.atTime(20, 20));
                esc.setNurseAssessment("电话评估：疼痛局限于右肩，无肿胀麻木、无夜间痛醒、否认摔倒，考虑训练强度过大所致，转医生复核确认。");
                esc.setNurseDecision(NurseDecision.ESCALATE_DOCTOR.name());
                esc.setNurse(nurse);
                esc.setNurseAssessedAt(date.atTime(21, 0));
                esc.setDoctorDispositions("REST,ICE");
                esc.setDoctorConclusion("考虑肩部软组织劳损。处置：暂停桥式运动 2 天，局部冰敷每日 3 次、每次 15 分钟；其余动作减量继续。2 天后疼痛缓解则解除暂停。");
                esc.setDoctor(doctor);
                esc.setDoctorReviewedAt(date.plusDays(1).atTime(9, 30));
                esc.setClearedBy(doctor.getName());
                esc.setClearedAt(date.plusDays(2).atTime(10, 0));
                esc.setClearNote("复评疼痛降至 3 分，红肿消退，恢复桥式运动（按降强度处方执行）。");
                esc.setCreatedAt(date.atTime(20, 5));
                painEscalationRepository.save(esc);
                tl(p, TimelineEventType.PAIN_ESCALATION_CREATED, null, "疼痛升级：暂停「桥式运动」",
                        "触发原因：训练后疼痛超阈值（训练后疼痛 7 分）。平台已暂停相关动作，待家属补充症状/用药/是否摔倒后由康复护士电话评估。",
                        "painEscalation", esc.getId(), date.atTime(20, 5));
                tl(p, TimelineEventType.ESCALATION_FAMILY_REPORT, family, "家属补充症状（疼痛升级）",
                        "症状：" + esc.getFamilySymptoms() + "；用药：" + esc.getFamilyMedication() + "；是否摔倒：否。已转康复护士电话评估。",
                        "painEscalation", esc.getId(), date.atTime(20, 20));
                tl(p, TimelineEventType.ESCALATION_NURSE_ASSESSMENT, nurse, "护士电话评估：转医生复核",
                        "评估内容：" + esc.getNurseAssessment() + "。相关动作保持暂停，等待医生复核处置。",
                        "painEscalation", esc.getId(), date.atTime(21, 0));
                tl(p, TimelineEventType.ESCALATION_DOCTOR_DISPOSITION, doctor, "医生处置结论：休息、冰敷",
                        esc.getDoctorConclusion() + "。结论已同步治疗师与家属；「桥式运动」保持暂停，待风险解除后恢复。",
                        "painEscalation", esc.getId(), date.plusDays(1).atTime(9, 30));
                tl(p, TimelineEventType.ESCALATION_CLEARED, doctor, "风险解除：「桥式运动」恢复训练",
                        esc.getClearNote() + "。相关动作重新进入每日训练任务。",
                        "painEscalation", esc.getId(), date.plusDays(2).atTime(10, 0));
            }
        }

        // 第二阶段处方（降低强度，当前执行中）
        Prescription rx2 = prescription(p, therapist, null, 2, PrescriptionStatus.ACTIVE,
                today.minusDays(7), today.plusDays(7), today.plusDays(4), 5,
                "疼痛事件后降低强度，逐步恢复", "第 8 天打卡疼痛升至 7 分，降低桥式运动强度（3 组×10 次 → 2 组×8 次）",
                List.of(
                        item("桥式运动", 2, 8, 2, "避免憋气与颈部过度用力", "无", 5,
                                "是否耸肩代偿、面色苍白、气促", true, "340200020", "20"),
                        item("患侧上肢被动关节活动", 2, 15, 2, "避免暴力牵拉肩关节", "弹力带", 4,
                                "肩关节疼痛、半脱位迹象", true, "340200026", "15"),
                        item("坐位平衡训练", 3, 5, 1, "无保护下禁止单独训练", "平衡垫", 6,
                                "身体向患侧倾倒程度", false, null, "0"),
                        item("扶助行器步行训练", 1, 10, 1, "地面湿滑禁止训练", "四脚助行器", 6,
                                "步态对称性、足下垂", true, "340200023", "25")
                ), today.minusDays(7).atTime(9, 45));

        // 近 7 天打卡挂到第二阶段处方
        trainingLogRepository.findByPatientIdOrderByLogDateDesc(p.getId()).stream()
                .filter(l -> !l.getLogDate().isBefore(today.minusDays(7)))
                .forEach(l -> {
                    l.setPrescription(rx2);
                    trainingLogRepository.save(l);
                });

        // ---- 纠错任务 A（已完成全流程：打回→确认→复评→已掌握） ----
        PrescriptionItem walkItem = rx2.getItems().get(3); // 扶助行器步行训练
        CorrectionTask taskA = new CorrectionTask();
        taskA.setPatient(p);
        taskA.setSourceLog(logD5);
        taskA.setPrescriptionItem(walkItem);
        taskA.setKeyPoints("[{\"code\":\"GAIT_DRAGGING\",\"label\":\"步态拖曳\",\"note\":\"患侧迈步拖曳明显\",\"timestamp\":\"00:08\"},"
                + "{\"code\":\"TRUNK_LEAN\",\"label\":\"躯干倾斜\",\"note\":\"躯干向健侧倾斜\",\"timestamp\":\"00:15\"}]");
        taskA.setCorrectionNote("步行训练中患侧支撑期过短、步态拖曳，躯干向健侧倾斜。请先站稳再迈步，患腿承重默数 3 秒；下次打卡请拍摄侧面步态视频。");
        taskA.setStatus(CorrectionStatus.MASTERED);
        taskA.setConsecutiveErrors(0);
        taskA.setConfirmedBy(family.getName());
        taskA.setConfirmedAt(today.minusDays(4).atTime(8, 30));
        taskA.setRecheckLog(logD3);
        taskA.setReviewNote("侧面视频对比：拖曳明显改善，患侧支撑期延长，判定已掌握。");
        taskA.setReviewedBy(therapist.getName());
        taskA.setReviewedAt(today.minusDays(3).atTime(21, 30));
        taskA.setCreatedAt(today.minusDays(5).atTime(21, 0));
        correctionTaskRepository.save(taskA);
        tl(p, TimelineEventType.CORRECTION_CREATED, therapist,
                "视频打回纠错：扶助行器步行训练（" + today.minusDays(5) + " 打卡）",
                "关键动作点：步态拖曳（00:08）：患侧迈步拖曳明显；躯干倾斜（00:15）：躯干向健侧倾斜。纠错说明："
                        + taskA.getCorrectionNote() + "。患者下次训练前必须确认观看。",
                "correctionTask", taskA.getId(), today.minusDays(5).atTime(21, 0));
        tl(p, TimelineEventType.CORRECTION_CONFIRMED, family, "已确认观看纠错内容：扶助行器步行训练",
                "关键动作点已知晓，下次训练将拍摄视频供对比复评。", "correctionTask", taskA.getId(),
                today.minusDays(4).atTime(8, 30));
        tl(p, TimelineEventType.CORRECTION_RECHECK, null, "复评视频已提交：扶助行器步行训练",
                "患者上传了新的训练视频，系统已与 " + today.minusDays(5) + " 被纠错的旧视频关联，等待治疗师对比复评。",
                "correctionTask", taskA.getId(), today.minusDays(3).atTime(20, 5));
        tl(p, TimelineEventType.CORRECTION_REVIEWED, therapist, "纠错复评：已掌握 ✓ 扶助行器步行训练",
                "对比新旧视频后判定患者已真正掌握动作。 复评备注：" + taskA.getReviewNote(),
                "correctionTask", taskA.getId(), today.minusDays(3).atTime(21, 30));

        // ---- 纠错任务 B（待患者确认：家属下次打卡前必须先观看确认） ----
        PrescriptionItem bridgeItem = rx2.getItems().get(0); // 桥式运动
        CorrectionTask taskB = new CorrectionTask();
        taskB.setPatient(p);
        taskB.setSourceLog(logD1);
        taskB.setPrescriptionItem(bridgeItem);
        taskB.setKeyPoints("[{\"code\":\"KNEE_ANGLE\",\"label\":\"膝关节角度\",\"note\":\"抬臀时膝屈曲约 110°，角度过大\",\"timestamp\":\"00:05\"},"
                + "{\"code\":\"SCAPULAR_COMPENSATION\",\"label\":\"肩胛代偿\",\"note\":\"肩胛上提代偿明显\",\"timestamp\":\"00:11\"}]");
        taskB.setCorrectionNote("桥式运动抬臀时膝关节屈曲角度过大、肩胛上提代偿。请收紧核心、双膝保持约 90°，肩部放松贴床，下次训练请拍摄侧面视频复评。");
        taskB.setStatus(CorrectionStatus.PENDING_CONFIRM);
        taskB.setCreatedAt(today.minusDays(1).atTime(21, 0));
        correctionTaskRepository.save(taskB);
        tl(p, TimelineEventType.CORRECTION_CREATED, therapist,
                "视频打回纠错：桥式运动（" + today.minusDays(1) + " 打卡）",
                "关键动作点：膝关节角度（00:05）：抬臀时膝屈曲约 110°，角度过大；肩胛代偿（00:11）：肩胛上提代偿明显。纠错说明："
                        + taskB.getCorrectionNote() + "。患者下次训练前必须确认观看。",
                "correctionTask", taskB.getId(), today.minusDays(1).atTime(21, 0));

        // 线下治疗记录
        OutpatientTreatment t1 = treatment(p, therapist, today.minusDays(10), "运动疗法（门诊）", "340200020", true, "40",
                "门诊 PT 一次，重点训练坐位平衡与转移");
        OutpatientTreatment t2 = treatment(p, therapist, today.minusDays(3), "中频脉冲电治疗", "340100017", true, "25",
                "患侧肩周镇痛、预防肩手综合征");

        // 上月医保结算单（已确认）
        InsuranceSettlement s = new InsuranceSettlement();
        s.setPatient(p);
        s.setPeriodStart(today.minusDays(30));
        s.setPeriodEnd(today.minusDays(1));
        s.setAdherencePercent(83);
        s.setHomeTrainingAmount(new BigDecimal("498.00"));
        s.setAssessmentAmount(new BigDecimal("30.00"));
        s.setOutpatientAmount(new BigDecimal("65.00"));
        s.setTotalAmount(new BigDecimal("593.00"));
        s.setReimbursableAmount(new BigDecimal("415.10"));
        s.setSelfPayAmount(new BigDecimal("177.90"));
        s.setDetailJson("[{\"name\":\"居家训练-桥式运动\",\"code\":\"340200020\",\"note\":\"按依从性折算 24.9 次 × ¥20\",\"amount\":498.00,\"reimbursable\":true},"
                + "{\"name\":\"复诊评估（康复评定）× 1\",\"code\":\"340200001\",\"note\":\"周期内完成 1 次复诊评估 × ¥30\",\"amount\":30.00,\"reimbursable\":true},"
                + "{\"name\":\"线下治疗-运动疗法（门诊）\",\"code\":\"340200020\",\"note\":\"门诊治疗\",\"amount\":40.00,\"reimbursable\":true},"
                + "{\"name\":\"线下治疗-中频脉冲电治疗\",\"code\":\"340100017\",\"note\":\"门诊治疗\",\"amount\":25.00,\"reimbursable\":true}]");
        s.setStatus(SettlementStatus.CONFIRMED);
        s.setCreatedBy(therapist.getName());
        s.setCreatedAt(today.minusDays(1).atTime(10, 0));
        s.setInterruptionNote(today.minusDays(8) + " 疼痛升级中断（训练后疼痛超阈值）：暂停「桥式运动」，处置：休息、冰敷（风险已解除）");
        settlementRepository.save(s);
        tl(p, TimelineEventType.SETTLEMENT_CREATED, therapist,
                "生成医保结算单（" + s.getPeriodStart() + " ~ " + s.getPeriodEnd() + "）",
                "居家训练 ¥498.00 + 复诊评估 ¥30.00 + 线下治疗 ¥65.00 = 合计 ¥593.00；按「城乡居民医保」报销比例 70%，报销 ¥415.10，自付 ¥177.90；已标注 1 次疼痛升级中断",
                "settlement", s.getId(), today.minusDays(1).atTime(10, 0));
        return p;
    }

    // ---------------- 患者2：刘桂芳（骨折术后·稳定训练） ----------------

    private Patient seedPatient2() {
        LocalDate today = LocalDate.now();
        Patient p = new Patient();
        p.setPatientNo("P20260002");
        p.setName("刘桂芳");
        p.setGender("女");
        p.setBirthDate(LocalDate.of(1968, 7, 25));
        p.setPhone("13905710002");
        p.setDiseaseType(DiseaseType.FRACTURE_POST_OP);
        p.setDiseaseStage(DiseaseStage.STABLE_TRAINING);
        p.setDiagnosis("右股骨颈骨折空心钉内固定术后 6 周");
        p.setCaregiverName("王大海");
        p.setCaregiverRelation("丈夫");
        p.setCaregiverPhone("13800000002");
        p.setAddress("溪口镇幸福小区 4 栋 201");
        p.setInsuranceType("职工医保");
        p.setInsuranceNo("YB3301020002");
        p.setDischargeDate(today.minusDays(42));
        p.setNextReviewDate(today.plusDays(10));
        p.setTherapist(therapist);
        p.setFamilyUser(family2);
        p.setCreatedAt(today.minusDays(30).atTime(9, 0));
        patientRepository.save(p);
        tl(p, TimelineEventType.PATIENT_CREATED, therapist, "建立患者档案",
                "诊断：右股骨颈骨折空心钉内固定术后 6 周；疾病类型：骨折术后；初始阶段：稳定训练",
                "patient", p.getId(), today.minusDays(30).atTime(9, 0));

        Assessment a1 = assessment(p, therapist, AssessmentType.INITIAL,
                "{\"髋关节屈曲\": 75, \"髋关节外展\": 25, \"膝关节屈曲\": 110, \"踝关节背屈\": 15}",
                4, 3, 70, null,
                "术后 6 周，部分负重期，髋部活动度受限，肌力 4 级。", today.minusDays(30));
        Assessment a2 = assessment(p, therapist, AssessmentType.FOLLOWUP,
                "{\"髋关节屈曲\": 90, \"髋关节外展\": 30, \"膝关节屈曲\": 120, \"踝关节背屈\": 18}",
                4, 2, 80, null,
                "复诊评估：活动度与肌力较初评改善，可过渡至单拐部分负重。", today.minusDays(7));

        Prescription rx = prescription(p, therapist, a2, 2, PrescriptionStatus.ACTIVE,
                today.minusDays(14), today.plusDays(14), today.plusDays(10), 6,
                "部分负重期强化肌力与关节活动度", "复诊评估后进入第 2 阶段：由床上训练过渡至负重训练",
                List.of(
                        item("踝泵运动", 3, 20, 3, "避免暴力背屈", "无", 4,
                                "小腿肿胀、皮温变化（警惕深静脉血栓）", true, "340200020", "20"),
                        item("直腿抬高训练", 3, 10, 2, "避免腰部代偿弓起", "沙袋（1kg）", 5,
                                "抬腿时腰部是否弓起、髋部疼痛", true, "340200020", "20"),
                        item("髋关节活动度训练", 2, 10, 1, "避免髋关节内收内旋（防脱位）", "无", 5,
                                "髋部疼痛、异常弹响", false, null, "0"),
                        item("助行器部分负重步行", 1, 15, 1, "负重不超过体重 50%", "四脚助行器", 6,
                                "步态对称性、患肢负重耐受", true, "340200023", "25")
                ), today.minusDays(14).atTime(10, 0));

        TrainingLog logD2 = null;
        for (int d = 14; d >= 1; d--) {
            if (d == 2) {
                // 疼痛升级事件：训练后疼痛 7 分超阈值 + 肿胀麻木
                logD2 = trainingLog(p, rx, today.minusDays(d), 60, 4, 7, null,
                        "练完直腿抬高后右髋又肿又麻，疼痛明显加重", true, false);
                logD2.setSwellingNumbness(true);
                trainingLogRepository.save(logD2);
            } else {
                trainingLog(p, rx, today.minusDays(d), 80 + (d % 3) * 6 > 100 ? 100 : 80 + (d % 3) * 6,
                        2, 3, null, d % 6 == 0 ? "走路比上周稳了" : null, true, false);
            }
        }
        treatment(p, therapist, today.minusDays(7), "运动疗法（门诊）", "340200020", true, "40", "复诊当日门诊 PT");

        // 疼痛升级处置（进行中：家属已补充 → 护士已转医生 → 医生已处置「休息+冰敷」，等待风险解除）
        Alert escAlert = alert(p, AlertType.PAIN_ESCALATION, AlertLevel.MEDIUM, AlertStatus.RESOLVED,
                "疼痛升级（训练后疼痛超阈值、出现肿胀/麻木）：已暂停「直腿抬高训练、助行器部分负重步行」，等待家属补充症状/用药/是否摔倒",
                logD2.getId(), today.minusDays(2).atTime(20, 5), nurse, today.minusDays(2).atTime(21, 0),
                today.minusDays(1).atTime(10, 0));
        escAlert.setDoctor(doctor);
        alertRepository.save(escAlert);
        PainEscalation esc2 = new PainEscalation();
        esc2.setPatient(p);
        esc2.setPrescription(rx);
        esc2.setTrainingLog(logD2);
        esc2.setAlert(escAlert);
        esc2.setTriggers("PAIN_OVER_THRESHOLD,SWELLING_NUMBNESS");
        esc2.setPainScore(7);
        esc2.setSuspendedItemIds("[" + rx.getItems().get(1).getId() + "," + rx.getItems().get(3).getId() + "]");
        esc2.setSuspendedItemNames("直腿抬高训练、助行器部分负重步行");
        esc2.setStatus(EscalationStatus.DISPOSITION_ACTIVE);
        esc2.setFamilySymptoms("右髋肿胀，大腿前侧发麻，走路时加重，休息后稍缓解，夜间不痛醒");
        esc2.setFamilyMedication("口服布洛芬缓释胶囊 0.3g，每日 2 次");
        esc2.setFamilyFell(false);
        esc2.setFamilyReportedBy(family2.getName());
        esc2.setFamilyReportedAt(today.minusDays(2).atTime(20, 30));
        esc2.setNurseAssessment("电话评估：右髋肿胀伴麻木，VAS 7 分，已指导冰敷并暂停负重训练；因涉及术后髋部症状，转医生复核。");
        esc2.setNurseDecision(NurseDecision.ESCALATE_DOCTOR.name());
        esc2.setNurse(nurse);
        esc2.setNurseAssessedAt(today.minusDays(2).atTime(21, 0));
        esc2.setDoctorDispositions("REST,ICE");
        esc2.setDoctorConclusion("考虑术后软组织反应性肿胀，未见脱位征象。处置：暂停直腿抬高与负重步行 3 天，抬高患肢、局部冰敷每日 3 次；若麻木持续超过 24 小时或加重，立即门诊复查。");
        esc2.setDoctor(doctor);
        esc2.setDoctorReviewedAt(today.minusDays(1).atTime(10, 0));
        esc2.setCreatedAt(today.minusDays(2).atTime(20, 5));
        painEscalationRepository.save(esc2);
        tl(p, TimelineEventType.PAIN_ESCALATION_CREATED, null, "疼痛升级：暂停「直腿抬高训练、助行器部分负重步行」",
                "触发原因：训练后疼痛超阈值、出现肿胀/麻木（训练后疼痛 7 分）。平台已暂停相关动作，待家属补充症状/用药/是否摔倒后由康复护士电话评估。",
                "painEscalation", esc2.getId(), today.minusDays(2).atTime(20, 5));
        tl(p, TimelineEventType.ESCALATION_FAMILY_REPORT, family2, "家属补充症状（疼痛升级）",
                "症状：" + esc2.getFamilySymptoms() + "；用药：" + esc2.getFamilyMedication() + "；是否摔倒：否。已转康复护士电话评估。",
                "painEscalation", esc2.getId(), today.minusDays(2).atTime(20, 30));
        tl(p, TimelineEventType.ESCALATION_NURSE_ASSESSMENT, nurse, "护士电话评估：转医生复核",
                "评估内容：" + esc2.getNurseAssessment() + "。相关动作保持暂停，等待医生复核处置。",
                "painEscalation", esc2.getId(), today.minusDays(2).atTime(21, 0));
        tl(p, TimelineEventType.ESCALATION_DOCTOR_DISPOSITION, doctor, "医生处置结论：休息、冰敷",
                esc2.getDoctorConclusion() + "。结论已同步治疗师与家属；「直腿抬高训练、助行器部分负重步行」保持暂停，待风险解除后恢复。",
                "painEscalation", esc2.getId(), today.minusDays(1).atTime(10, 0));
        return p;
    }

    // ---------------- 患者3：王强（颈肩腰腿痛·稳定训练） ----------------

    private void seedPatient3() {
        LocalDate today = LocalDate.now();
        Patient p = new Patient();
        p.setPatientNo("P20260003");
        p.setName("王强");
        p.setGender("男");
        p.setBirthDate(LocalDate.of(1981, 11, 3));
        p.setPhone("13905710003");
        p.setDiseaseType(DiseaseType.NECK_SHOULDER_PAIN);
        p.setDiseaseStage(DiseaseStage.STABLE_TRAINING);
        p.setDiagnosis("慢性非特异性腰痛，久坐职业");
        p.setCaregiverName("王强（自我管理）");
        p.setCaregiverRelation("本人");
        p.setCaregiverPhone("13905710003");
        p.setAddress("城关镇解放街 88 号");
        p.setInsuranceType("新农合");
        p.setInsuranceNo("YB3301020003");
        p.setNextReviewDate(today.plusDays(14));
        p.setTherapist(therapist2);
        p.setFamilyUser(family3);
        p.setCreatedAt(today.minusDays(12).atTime(9, 0));
        patientRepository.save(p);
        tl(p, TimelineEventType.PATIENT_CREATED, therapist2, "建立患者档案",
                "诊断：慢性非特异性腰痛；疾病类型：颈肩腰腿痛；初始阶段：稳定训练",
                "patient", p.getId(), today.minusDays(12).atTime(9, 0));

        Assessment a1 = assessment(p, therapist2, AssessmentType.INITIAL,
                "{\"腰椎前屈\": 45, \"腰椎后伸\": 10, \"直腿抬高\": 60}",
                5, 4, 85, null,
                "久坐后腰痛加重，无下肢放射痛，核心肌群耐力不足。", today.minusDays(12));

        Prescription rx = prescription(p, therapist2, a1, 1, PrescriptionStatus.ACTIVE,
                today.minusDays(12), today.plusDays(16), today.plusDays(14), 6,
                "核心稳定 + 姿势管理", "门诊初评后开具居家训练处方",
                List.of(
                        item("麦肯基伸展", 3, 10, 2, "疼痛向下肢放射时立即停止", "瑜伽垫", 5,
                                "疼痛是否向臀部/下肢放射", true, "340200020", "20"),
                        item("猫式伸展", 2, 12, 1, "动作缓慢，避免弹振", "瑜伽垫", 5,
                                "腰部僵硬缓解程度", false, null, "0"),
                        item("核心激活（死虫式）", 3, 8, 1, "腰部贴紧床面，避免拱腰", "无", 5,
                                "腰部是否拱起代偿", true, "340200020", "20")
                ), today.minusDays(12).atTime(10, 0));

        for (int d = 10; d >= 1; d--) {
            int pain = d > 6 ? 4 : (d > 3 ? 3 : 2);
            trainingLog(p, rx, today.minusDays(d), 90, pain, pain, null,
                    d == 4 ? "久坐后还是会酸，练完缓解" : null, true, false);
        }
    }

    // ---------------- 患者4：陈晨（儿童发育迟缓·稳定训练） ----------------

    private void seedPatient4() {
        LocalDate today = LocalDate.now();
        Patient p = new Patient();
        p.setPatientNo("P20260004");
        p.setName("陈晨");
        p.setGender("男");
        p.setBirthDate(LocalDate.of(2021, 5, 18));
        p.setPhone("13905710004");
        p.setDiseaseType(DiseaseType.CHILD_DEVELOPMENT_DELAY);
        p.setDiseaseStage(DiseaseStage.STABLE_TRAINING);
        p.setDiagnosis("全面性发育迟缓，粗大运动发育商 68");
        p.setCaregiverName("刘敏");
        p.setCaregiverRelation("母亲");
        p.setCaregiverPhone("13905710004");
        p.setAddress("溪口镇育才路 6 号");
        p.setInsuranceType("城乡居民医保");
        p.setInsuranceNo("YB3301020004");
        p.setNextReviewDate(today.plusDays(21));
        p.setTherapist(therapist2);
        p.setFamilyUser(family4);
        p.setCreatedAt(today.minusDays(14).atTime(9, 0));
        patientRepository.save(p);
        tl(p, TimelineEventType.PATIENT_CREATED, therapist2, "建立患者档案",
                "诊断：全面性发育迟缓，粗大运动发育商 68；疾病类型：儿童发育迟缓；初始阶段：稳定训练",
                "patient", p.getId(), today.minusDays(14).atTime(9, 0));

        Assessment a1 = assessment(p, therapist2, AssessmentType.INITIAL,
                "{\"俯卧抬头\": 45, \"四点支撑维持秒数\": 10}",
                4, 0, 60, null,
                "粗大运动落后约 12 个月，可独走但步态不稳，易跌倒。", today.minusDays(14));

        Prescription rx = prescription(p, therapist2, a1, 1, PrescriptionStatus.ACTIVE,
                today.minusDays(14), today.plusDays(14), today.plusDays(21), 3,
                "游戏化家庭训练，家长全程陪同", "门诊评估后开具家庭训练处方",
                List.of(
                        item("俯卧抬头训练", 3, 5, 2, "哭闹剧烈时暂停", "玩具诱导", 3,
                                "抬头维持时间、情绪反应", true, "340200020", "20"),
                        item("四点支撑爬行", 2, 10, 1, "地面需铺软垫", "爬行垫", 3,
                                "爬行动作协调性", true, "340200020", "20"),
                        item("平衡木行走（家长保护）", 1, 8, 1, "家长必须全程牵手保护", "低平衡木", 3,
                                "步态稳定性、跌倒次数", false, null, "0")
                ), today.minusDays(14).atTime(10, 0));

        for (int d = 12; d >= 1; d--) {
            trainingLog(p, rx, today.minusDays(d), 85, 0, 0, null,
                    d % 4 == 0 ? "今天配合度不错，爬了 10 米" : null, true, false);
        }
    }

    // ---------------- 患者5：周淑英（脑卒中·复发预警，多条待处理预警） ----------------

    private Patient seedPatient5() {
        LocalDate today = LocalDate.now();
        Patient p = new Patient();
        p.setPatientNo("P20260005");
        p.setName("周淑英");
        p.setGender("女");
        p.setBirthDate(LocalDate.of(1956, 1, 30));
        p.setPhone("13905710005");
        p.setDiseaseType(DiseaseType.STROKE);
        p.setDiseaseStage(DiseaseStage.RECURRENCE_WARNING);
        p.setDiagnosis("脑干梗死后遗症期，左侧肢体无力，平衡功能障碍");
        p.setCaregiverName("吴强");
        p.setCaregiverRelation("儿子");
        p.setCaregiverPhone("13905710005");
        p.setAddress("城关镇和平巷 3 号");
        p.setInsuranceType("职工医保");
        p.setInsuranceNo("YB3301020005");
        p.setDischargeDate(today.minusDays(60));
        p.setNextReviewDate(today.plusDays(2));
        p.setTherapist(therapist);
        p.setFamilyUser(family5);
        p.setCreatedAt(today.minusDays(45).atTime(9, 0));
        patientRepository.save(p);
        tl(p, TimelineEventType.PATIENT_CREATED, therapist, "建立患者档案",
                "诊断：脑干梗死后遗症期；疾病类型：脑卒中；初始阶段：稳定训练",
                "patient", p.getId(), today.minusDays(45).atTime(9, 0));

        Assessment a1 = assessment(p, therapist, AssessmentType.INITIAL,
                "{\"肩关节前屈\": 110, \"肘关节伸展\": -10, \"膝关节屈曲\": 100}",
                4, 3, 55, null,
                "左侧肢体肌力 4 级，坐位平衡 2 级，站立需监护。", today.minusDays(45));

        Prescription rx = prescription(p, therapist, a1, 1, PrescriptionStatus.ACTIVE,
                today.minusDays(20), today.plusDays(10), today.plusDays(2), 6,
                "维持期训练，注意防跌倒", "出院后维持期处方",
                List.of(
                        item("坐位站起训练", 3, 8, 2, "头晕时立即停止并坐下", "扶手椅", 5,
                                "站起时是否头晕、摇晃", true, "340200020", "20"),
                        item("原地踏步训练", 2, 20, 1, "需家属在旁保护", "扶手", 5,
                                "步态稳定性、是否拖步", true, "340200023", "25"),
                        item("肩关节活动训练", 2, 12, 1, "避免暴力上举", "弹力带", 5,
                                "肩部疼痛、活动范围", false, null, "0")
                ), today.minusDays(20).atTime(10, 0));

        // -20 ~ -4 天打卡：完成率下降、疼痛上升、出现代偿与无法陪练
        int[] completion = {90, 88, 85, 80, 82, 75, 70, 72, 65, 60, 55, 50, 45, 40, 35, 30, 25};
        int idx = 0;
        TrainingLog logD5 = null, logD4 = null;
        for (int d = 20; d >= 4; d--) {
            LocalDate date = today.minusDays(d);
            int painBefore = d > 10 ? 3 : 4;
            int painAfter = d > 10 ? 4 : (d > 6 ? 5 : 6);
            boolean compensation = d <= 6;
            boolean companion = d != 4;
            TrainingLog log = trainingLog(p, rx, date, completion[Math.min(idx++, completion.length - 1)],
                    painBefore, painAfter, null,
                    d == 6 ? "这两天练完肩膀疼得厉害" : (d == 4 ? "我这两天要出差，没法陪我妈练" : null),
                    companion, compensation);
            if (d == 5) {
                logD5 = log;
            }
            if (d == 4) {
                logD4 = log;
            }
            if (d == 6) {
                Alert alert = alert(p, AlertType.PAIN_RISE, AlertLevel.MEDIUM, AlertStatus.FOLLOWING,
                        "训练后疼痛 6 分，达到/超过处方疼痛阈值 6 分", log.getId(),
                        date.atTime(20, 5), nurse, date.atTime(20, 40), null);
                NurseFollowup f = followup(alert, p,
                        "电话回访：患者诉近一周训练后肩痛加重，夜间也有隐痛。建议暂停弹力带肩关节训练，注意夜间保暖，已预约 2 天后线下复诊。",
                        "建议线下复诊", "等待复诊评估结果，必要时转医生", date.atTime(20, 40));
                tl(p, TimelineEventType.ALERT_CREATED, null, "触发预警：疼痛升高（中风险）",
                        alert.getMessage() + "。已推送康复护士电话随访队列。", "alert", alert.getId(), date.atTime(20, 5));
                tl(p, TimelineEventType.ALERT_ASSIGNED, nurse, "护士接单：疼痛升高",
                        "赵晓燕 开始电话随访。", "alert", alert.getId(), date.atTime(20, 30));
                tl(p, TimelineEventType.NURSE_FOLLOWUP, nurse, "电话随访（疼痛升高）：建议线下复诊",
                        "随访内容：" + f.getContent() + "；后续安排：" + f.getNextAction(),
                        "followup", f.getId(), date.atTime(20, 40));
            }
            if (d == 5) {
                Alert alert = alert(p, AlertType.COMPENSATION, AlertLevel.MEDIUM, AlertStatus.PENDING,
                        "家属观察到明显动作代偿，需要治疗师视频纠错", log.getId(),
                        date.atTime(20, 5), null, null, null);
                tl(p, TimelineEventType.ALERT_CREATED, null, "触发预警：动作代偿明显（中风险）",
                        alert.getMessage() + "。已推送康复护士电话随访队列。", "alert", alert.getId(), date.atTime(20, 5));
            }
            if (d == 4) {
                Alert alert = alert(p, AlertType.NO_COMPANION, AlertLevel.MEDIUM, AlertStatus.PENDING,
                        "家属反馈无法陪练，居家训练缺少监督", log.getId(),
                        date.atTime(20, 5), null, null, null);
                tl(p, TimelineEventType.ALERT_CREATED, null, "触发预警：家属无法陪练（中风险）",
                        alert.getMessage() + "。已推送康复护士电话随访队列。", "alert", alert.getId(), date.atTime(20, 5));
            }
        }
        // -3 ~ -1 天未打卡 → 连续漏练 3 天
        Alert missed = alert(p, AlertType.MISSED_TRAINING, AlertLevel.HIGH, AlertStatus.PENDING,
                "患者已连续 3 天未提交居家训练打卡", null,
                today.atTime(7, 0), null, null, null);
        tl(p, TimelineEventType.ALERT_CREATED, null, "触发预警：连续漏练（高风险）",
                missed.getMessage() + "。已推送康复护士电话随访队列。", "alert", missed.getId(), today.atTime(7, 0));

        // 疼痛升级处置（待家属补充：-4 天打卡疼痛 6 分达阈值，平台已暂停「坐位站起训练」）
        Alert escAlert5 = alert(p, AlertType.PAIN_ESCALATION, AlertLevel.MEDIUM, AlertStatus.PENDING,
                "疼痛升级（训练后疼痛超阈值）：已暂停「坐位站起训练」，等待家属补充症状/用药/是否摔倒",
                logD4.getId(), today.minusDays(4).atTime(20, 6), null, null, null);
        PainEscalation esc5 = new PainEscalation();
        esc5.setPatient(p);
        esc5.setPrescription(rx);
        esc5.setTrainingLog(logD4);
        esc5.setAlert(escAlert5);
        esc5.setTriggers("PAIN_OVER_THRESHOLD");
        esc5.setPainScore(6);
        esc5.setSuspendedItemIds("[" + rx.getItems().get(0).getId() + "]");
        esc5.setSuspendedItemNames("坐位站起训练");
        esc5.setStatus(EscalationStatus.PENDING_FAMILY_INFO);
        esc5.setCreatedAt(today.minusDays(4).atTime(20, 6));
        painEscalationRepository.save(esc5);
        tl(p, TimelineEventType.PAIN_ESCALATION_CREATED, null, "疼痛升级：暂停「坐位站起训练」",
                "触发原因：训练后疼痛超阈值（训练后疼痛 6 分）。平台已暂停相关动作，待家属补充症状/用药/是否摔倒后由康复护士电话评估。",
                "painEscalation", esc5.getId(), today.minusDays(4).atTime(20, 6));

        // ---- 纠错任务 C（连续未掌握 1 次：再一次未掌握将触发自动线下复评） ----
        PrescriptionItem stepItem = rx.getItems().get(1); // 原地踏步训练
        CorrectionTask taskC = new CorrectionTask();
        taskC.setPatient(p);
        taskC.setSourceLog(logD5);
        taskC.setPrescriptionItem(stepItem);
        taskC.setKeyPoints("[{\"code\":\"GAIT_DRAGGING\",\"label\":\"步态拖曳\",\"note\":\"原地踏步左足拖曳\",\"timestamp\":\"00:06\"}]");
        taskC.setCorrectionNote("踏步训练左足拖曳明显，请先抬高膝部再落足，家属在旁保护防跌倒。");
        taskC.setStatus(CorrectionStatus.NOT_MASTERED);
        taskC.setConsecutiveErrors(1);
        taskC.setConfirmedBy(family5.getName());
        taskC.setConfirmedAt(today.minusDays(5).atTime(21, 30));
        taskC.setRecheckLog(logD4);
        taskC.setReviewNote("复评视频左足仍拖曳，纠正不到位，继续练习。");
        taskC.setReviewedBy(therapist.getName());
        taskC.setReviewedAt(today.minusDays(4).atTime(21, 0));
        taskC.setCreatedAt(today.minusDays(5).atTime(21, 0));
        correctionTaskRepository.save(taskC);
        tl(p, TimelineEventType.CORRECTION_CREATED, therapist,
                "视频打回纠错：原地踏步训练（" + today.minusDays(5) + " 打卡）",
                "关键动作点：步态拖曳（00:06）：原地踏步左足拖曳。纠错说明：" + taskC.getCorrectionNote()
                        + "。患者下次训练前必须确认观看。",
                "correctionTask", taskC.getId(), today.minusDays(5).atTime(21, 0));
        tl(p, TimelineEventType.CORRECTION_CONFIRMED, family5, "已确认观看纠错内容：原地踏步训练",
                "关键动作点已知晓，下次训练将拍摄视频供对比复评。", "correctionTask", taskC.getId(),
                today.minusDays(5).atTime(21, 30));
        tl(p, TimelineEventType.CORRECTION_RECHECK, null, "复评视频已提交：原地踏步训练",
                "患者上传了新的训练视频，系统已与 " + today.minusDays(5) + " 被纠错的旧视频关联，等待治疗师对比复评。",
                "correctionTask", taskC.getId(), today.minusDays(4).atTime(20, 5));
        tl(p, TimelineEventType.CORRECTION_REVIEWED, therapist, "纠错复评：未掌握（连续第 1 次）原地踏步训练",
                "旧问题仍未纠正，需继续练习。 复评备注：" + taskC.getReviewNote(),
                "correctionTask", taskC.getId(), today.minusDays(4).atTime(21, 0));

        // 一周前医生介入记录（已解决）
        Alert referral = alert(p, AlertType.DOCTOR_REFERRAL, AlertLevel.MEDIUM, AlertStatus.RESOLVED,
                "治疗师提请医生介入：疼痛持续升高，需排除肩手综合征", null,
                today.minusDays(7).atTime(10, 0), null, null, today.minusDays(6).atTime(15, 0));
        referral.setDoctor(doctor);
        alertRepository.save(referral);
        DoctorIntervention di = new DoctorIntervention();
        di.setPatient(p);
        di.setAlert(referral);
        di.setDoctor(doctor);
        di.setOpinion("考虑肩手综合征早期表现。建议：1. 暂停弹力带抗阻训练 1 周；2. 抬高患肢、向心性按摩；3. 一周后门诊复查，必要时行肌骨超声。");
        di.setCreatedAt(today.minusDays(6).atTime(15, 0));
        interventionRepository.save(di);
        PrescriptionAdjustment adj = adjustment(p, rx, therapist, AdjustmentDecision.DOCTOR_REFERRAL,
                "疼痛连续 3 天 ≥5 分且夜间隐痛，居家处理效果不佳，提请医生介入。", today.minusDays(7).atTime(10, 0));
        tl(p, TimelineEventType.PRESCRIPTION_ADJUSTED, therapist, "处方决策：提醒医生介入",
                "依据：" + adj.getReason(), "adjustment", adj.getId(), today.minusDays(7).atTime(10, 0));
        tl(p, TimelineEventType.DOCTOR_OPINION, doctor, "医生处理意见（转诊医生处理）",
                di.getOpinion(), "intervention", di.getId(), today.minusDays(6).atTime(15, 0));
        tl(p, TimelineEventType.ALERT_RESOLVED, doctor, "预警解除：转诊医生处理",
                referral.getMessage(), "alert", referral.getId(), today.minusDays(6).atTime(15, 0));

        // 阶段变更记录：稳定训练 → 复发预警
        tl(p, TimelineEventType.STAGE_CHANGED, therapist, "疾病阶段调整：稳定训练 → 复发预警",
                "调整原因：连续漏练 3 天 + 疼痛持续升高 + 动作代偿，符合复发预警标准，转入密集随访节奏。",
                "patient", p.getId(), today.minusDays(1).atTime(9, 0));
        return p;
    }

    // ---------------- 辅助方法 ----------------

    private Assessment assessment(Patient p, User therapist, AssessmentType type, String romJson,
                                  Integer muscle, Integer pain, Integer adl, String gaitVideo,
                                  String notes, LocalDate date) {
        Assessment a = new Assessment();
        a.setPatient(p);
        a.setTherapist(therapist);
        a.setType(type);
        a.setRomJson(romJson);
        a.setMuscleStrength(muscle);
        a.setPainScore(pain);
        a.setAdlScore(adl);
        a.setGaitVideoPath(gaitVideo);
        a.setNotes(notes);
        a.setAssessmentDate(date);
        a.setCreatedAt(date.atTime(9, 30));
        Assessment saved = assessmentRepository.save(a);
        tl(p, TimelineEventType.ASSESSMENT_RECORDED, therapist,
                type.getLabel() + "：疼痛 " + pain + " 分，肌力 " + muscle + " 级",
                notes, "assessment", saved.getId(), date.atTime(9, 30));
        return saved;
    }

    private Prescription prescription(Patient p, User therapist, Assessment assessment, int phase,
                                      PrescriptionStatus status, LocalDate start, LocalDate end,
                                      LocalDate nextReview, Integer painThreshold, String notes,
                                      String adjustReason, List<PrescriptionItem> items, LocalDateTime createdAt) {
        Prescription rx = new Prescription();
        rx.setPatient(p);
        rx.setTherapist(therapist);
        rx.setAssessment(assessment);
        rx.setPhase(phase);
        rx.setStatus(status);
        rx.setStartDate(start);
        rx.setEndDate(end);
        rx.setNextReviewDate(nextReview);
        rx.setPainThreshold(painThreshold);
        rx.setNotes(notes);
        rx.setAdjustReason(adjustReason);
        rx.setCreatedAt(createdAt);
        items.forEach(rx::addItem);
        Prescription saved = prescriptionRepository.save(rx);
        tl(p, TimelineEventType.PRESCRIPTION_CREATED, therapist,
                "开具第 " + phase + " 阶段居家训练处方（" + items.size() + " 个训练项目）",
                "生成原因：" + adjustReason + "；计划复诊日期：" + nextReview,
                "prescription", saved.getId(), createdAt);
        return saved;
    }

    private PrescriptionItem item(String name, int sets, int reps, int freq, String contraindication,
                                  String device, int painThreshold, String observation,
                                  boolean reimbursable, String code, String price) {
        PrescriptionItem item = new PrescriptionItem();
        item.setExerciseName(name);
        item.setTargetSets(sets);
        item.setTargetReps(reps);
        item.setFrequencyPerDay(freq);
        item.setContraindication(contraindication);
        item.setAssistiveDevice(device);
        item.setPainThreshold(painThreshold);
        item.setFamilyObservation(observation);
        item.setReimbursable(reimbursable);
        item.setInsuranceCode(code);
        item.setUnitPrice(new BigDecimal(price));
        return item;
    }

    private TrainingLog trainingLog(Patient p, Prescription rx, LocalDate date, int completion,
                                    Integer painBefore, Integer painAfter, String photos, String note,
                                    boolean companion, boolean compensation) {
        TrainingLog log = new TrainingLog();
        log.setPatient(p);
        log.setPrescription(rx);
        log.setLogDate(date);
        log.setSubmittedBy(p.getFamilyUser() != null ? p.getFamilyUser() : family);
        log.setCompletionRate(completion);
        log.setPainBefore(painBefore);
        log.setPainAfter(painAfter);
        log.setAbnormalPhotos(photos);
        log.setFamilyNote(note);
        log.setCompanionAvailable(companion);
        log.setCompensationObserved(compensation);
        log.setCreatedAt(date.atTime(20, 0));
        // 项目级完成明细（器具使用统计用）：完成率高则全部完成，低则隔项完成
        StringBuilder detail = new StringBuilder("[");
        List<PrescriptionItem> items = rx.getItems();
        for (int i = 0; i < items.size(); i++) {
            PrescriptionItem it = items.get(i);
            boolean done = completion >= 60 || (i % 2 == 0 && completion >= 40);
            if (i > 0) {
                detail.append(',');
            }
            detail.append("{\"itemId\":").append(it.getId())
                    .append(",\"done\":").append(done)
                    .append(",\"actualSets\":").append(done ? it.getTargetSets() : 0)
                    .append(",\"actualReps\":").append(done ? it.getTargetReps() : 0)
                    .append("}");
        }
        detail.append(']');
        log.setCompletedDetail(detail.toString());
        TrainingLog saved = trainingLogRepository.save(log);
        tl(p, TimelineEventType.TRAINING_LOG_SUBMITTED, saved.getSubmittedBy(),
                "居家训练打卡（" + date + "）：完成率 " + completion + "%，疼痛 " + painBefore + " → " + painAfter + " 分",
                note, "trainingLog", saved.getId(), date.atTime(20, 0));
        return saved;
    }

    private Alert alert(Patient p, AlertType type, AlertLevel level, AlertStatus status, String message,
                        Long sourceLogId, LocalDateTime createdAt, User nurse, LocalDateTime handledAt,
                        LocalDateTime resolvedAt) {
        Alert alert = new Alert();
        alert.setPatient(p);
        alert.setType(type);
        alert.setLevel(level);
        alert.setStatus(status);
        alert.setMessage(message);
        alert.setSourceLogId(sourceLogId);
        alert.setCreatedAt(createdAt);
        alert.setNurse(nurse);
        alert.setHandledAt(handledAt);
        alert.setResolvedAt(resolvedAt);
        return alertRepository.save(alert);
    }

    private NurseFollowup followup(Alert alert, Patient p, String content, String outcome,
                                   String nextAction, LocalDateTime time) {
        NurseFollowup f = new NurseFollowup();
        f.setAlert(alert);
        f.setPatient(p);
        f.setNurse(nurse);
        f.setFollowTime(time);
        f.setMethod("电话");
        f.setContent(content);
        f.setOutcome(outcome);
        f.setNextAction(nextAction);
        f.setCreatedAt(time);
        return followupRepository.save(f);
    }

    private PrescriptionAdjustment adjustment(Patient p, Prescription rx, User therapist,
                                              AdjustmentDecision decision, String reason, LocalDateTime time) {
        PrescriptionAdjustment adj = new PrescriptionAdjustment();
        adj.setPatient(p);
        adj.setPrescription(rx);
        adj.setTherapist(therapist);
        adj.setDecision(decision);
        adj.setReason(reason);
        adj.setCreatedAt(time);
        return adjustmentRepository.save(adj);
    }

    private OutpatientTreatment treatment(Patient p, User therapist, LocalDate date, String itemName,
                                          String code, boolean reimbursable, String amount, String note) {
        OutpatientTreatment t = new OutpatientTreatment();
        t.setPatient(p);
        t.setTherapist(therapist);
        t.setTreatmentDate(date);
        t.setItemName(itemName);
        t.setInsuranceCode(code);
        t.setReimbursable(reimbursable);
        t.setAmount(new BigDecimal(amount));
        t.setNote(note);
        t.setCreatedAt(date.atTime(11, 0));
        OutpatientTreatment saved = treatmentRepository.save(t);
        tl(p, TimelineEventType.OUTPATIENT_TREATMENT, therapist,
                "线下治疗：" + itemName + "（¥" + amount + "）", note, "treatment", saved.getId(), date.atTime(11, 0));
        return saved;
    }

    private void tl(Patient p, TimelineEventType type, User actor, String title, String content,
                    String refType, Long refId, LocalDateTime createdAt) {
        TimelineEvent event = new TimelineEvent();
        event.setPatient(p);
        event.setEventType(type);
        if (actor != null) {
            event.setActorId(actor.getId());
            event.setActorName(actor.getName());
            event.setActorRole(actor.getRole().getLabel());
        } else {
            event.setActorName("系统");
            event.setActorRole("系统");
        }
        event.setTitle(title);
        event.setContent(content);
        event.setRefType(refType);
        event.setRefId(refId);
        event.setCreatedAt(createdAt);
        timelineRepository.save(event);
    }
}
