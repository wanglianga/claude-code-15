package com.rehab.platform.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rehab.platform.config.BusinessException;
import com.rehab.platform.config.SecurityUtils;
import com.rehab.platform.dto.Dtos;
import com.rehab.platform.enums.*;
import com.rehab.platform.model.*;
import com.rehab.platform.repository.AlertRepository;
import com.rehab.platform.repository.PainEscalationRepository;
import com.rehab.platform.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 疼痛升级处置：
 * 打卡触发（疼痛超阈值/肿胀麻木/夜间痛加重）→ 平台暂停相关动作 → 家属补充症状/用药/是否摔倒 →
 * 护士电话评估 → 必要时转医生复核 → 医生处置（休息/冰敷/影像检查/门诊复诊）→
 * 风险解除前相关动作不出现在每日训练任务中；医保结算单标注中断原因。
 */
@Service
@RequiredArgsConstructor
public class PainEscalationService {

    private final PainEscalationRepository escalationRepository;
    private final AlertRepository alertRepository;
    private final PatientRepository patientRepository;
    private final PatientService patientService;
    private final TimelineService timelineService;
    private final ObjectMapper objectMapper;

    // ---------- 触发 ----------

    /**
     * 打卡提交后调用：命中触发条件则创建升级处置单、暂停相关动作并推送护士队列。
     * 暂停动作范围：家属指定的疼痛动作 > 单项疼痛阈值被超出的项目 > 本次已完成的项目 > 全部项目。
     */
    @Transactional
    public PainEscalation checkAndCreate(Patient patient, Prescription prescription, TrainingLog log,
                                         Dtos.TrainingLogRequest req) {
        List<EscalationTrigger> triggers = new ArrayList<>();
        int threshold = prescription.getPainThreshold() == null ? 6 : prescription.getPainThreshold();
        if (log.getPainAfter() != null && log.getPainAfter() >= threshold) {
            triggers.add(EscalationTrigger.PAIN_OVER_THRESHOLD);
        }
        if (Boolean.TRUE.equals(log.getSwellingNumbness())) {
            triggers.add(EscalationTrigger.SWELLING_NUMBNESS);
        }
        if (Boolean.TRUE.equals(log.getNightPainWorse())) {
            triggers.add(EscalationTrigger.NIGHT_PAIN_WORSE);
        }
        if (triggers.isEmpty()) {
            return null;
        }

        List<PrescriptionItem> items = prescription.getItems();
        List<PrescriptionItem> suspended = List.of();
        if (req.painItemIds() != null && !req.painItemIds().isEmpty()) {
            Set<Long> ids = new HashSet<>(req.painItemIds());
            suspended = items.stream().filter(i -> ids.contains(i.getId())).toList();
        }
        if (suspended.isEmpty() && log.getPainAfter() != null) {
            suspended = items.stream()
                    .filter(i -> i.getPainThreshold() != null && log.getPainAfter() >= i.getPainThreshold())
                    .toList();
        }
        if (suspended.isEmpty() && req.items() != null) {
            Set<Long> doneIds = req.items().stream()
                    .filter(i -> Boolean.TRUE.equals(i.done()))
                    .map(Dtos.TrainingLogItem::itemId)
                    .collect(Collectors.toSet());
            suspended = items.stream().filter(i -> doneIds.contains(i.getId())).toList();
        }
        if (suspended.isEmpty()) {
            suspended = items;
        }

        String triggerText = triggers.stream().map(EscalationTrigger::getLabel).collect(Collectors.joining("、"));
        String itemNames = suspended.stream().map(PrescriptionItem::getExerciseName).collect(Collectors.joining("、"));

        // 推送护士随访队列的关联预警
        Alert alert = new Alert();
        alert.setPatient(patient);
        alert.setType(AlertType.PAIN_ESCALATION);
        alert.setLevel(log.getPainAfter() != null && log.getPainAfter() >= 8 ? AlertLevel.HIGH : AlertLevel.MEDIUM);
        alert.setMessage("疼痛升级（" + triggerText + "）：已暂停「" + itemNames + "」，等待家属补充症状/用药/是否摔倒");
        alert.setSourceLogId(log.getId());
        alertRepository.save(alert);

        PainEscalation esc = new PainEscalation();
        esc.setPatient(patient);
        esc.setPrescription(prescription);
        esc.setTrainingLog(log);
        esc.setAlert(alert);
        esc.setTriggers(triggers.stream().map(Enum::name).collect(Collectors.joining(",")));
        esc.setPainScore(log.getPainAfter());
        esc.setSuspendedItemIds(toJson(suspended.stream().map(PrescriptionItem::getId).toList()));
        esc.setSuspendedItemNames(itemNames);
        esc.setStatus(EscalationStatus.PENDING_FAMILY_INFO);
        PainEscalation saved = escalationRepository.save(esc);

        timelineService.record(patient, TimelineEventType.PAIN_ESCALATION_CREATED, null,
                "疼痛升级：暂停「" + itemNames + "」",
                "触发原因：" + triggerText + (log.getPainAfter() != null ? "（训练后疼痛 " + log.getPainAfter() + " 分）" : "")
                        + "。平台已暂停相关动作，待家属补充症状/用药/是否摔倒后由康复护士电话评估。",
                "painEscalation", saved.getId());
        return saved;
    }

    // ---------- 家属补充 ----------

    /** 家属补充症状/用药/是否摔倒（护士可代登记） */
    @Transactional
    public PainEscalation familyReport(Long id, Dtos.FamilyReportRequest req) {
        PainEscalation esc = getOne(id);
        User user = SecurityUtils.currentUser();
        boolean proxy = false;
        if (user.getRole() == Role.FAMILY) {
            patientService.getAccessible(esc.getPatient().getId());
        } else {
            SecurityUtils.requireRole(Role.NURSE, Role.ADMIN);
            proxy = true;
        }
        if (esc.getStatus() != EscalationStatus.PENDING_FAMILY_INFO) {
            throw new BusinessException("该处置单当前状态无需补充家属信息");
        }
        if (Boolean.TRUE.equals(req.fell()) && (req.fellDetail() == null || req.fellDetail().isBlank())) {
            throw new BusinessException("患者有摔倒，请补充摔倒经过");
        }
        esc.setFamilySymptoms(req.symptoms());
        esc.setFamilyMedication(req.medication());
        esc.setFamilyFell(req.fell());
        esc.setFamilyFellDetail(req.fellDetail());
        esc.setFamilyReportedBy(user.getName() + (proxy ? "（护士代登记）" : ""));
        esc.setFamilyReportedAt(LocalDateTime.now());
        esc.setStatus(EscalationStatus.NURSE_ASSESSING);

        // 摔倒属于高危信号：关联预警升为高风险
        if (Boolean.TRUE.equals(req.fell()) && esc.getAlert() != null) {
            Alert alert = esc.getAlert();
            alert.setLevel(AlertLevel.HIGH);
            alert.setMessage(alert.getMessage() + "；家属补充：有摔倒史");
            alertRepository.save(alert);
        }
        PainEscalation saved = escalationRepository.save(esc);
        timelineService.record(esc.getPatient(), TimelineEventType.ESCALATION_FAMILY_REPORT, user,
                "家属补充症状（疼痛升级）",
                "症状：" + req.symptoms() + "；用药：" + req.medication()
                        + "；是否摔倒：" + (Boolean.TRUE.equals(req.fell()) ? "是（" + req.fellDetail() + "）" : "否")
                        + "。已转康复护士电话评估。",
                "painEscalation", saved.getId());
        return saved;
    }

    // ---------- 护士电话评估 ----------

    @Transactional
    public PainEscalation nurseAssessment(Long id, Dtos.NurseAssessmentRequest req) {
        SecurityUtils.requireRole(Role.NURSE, Role.ADMIN);
        PainEscalation esc = getOne(id);
        if (esc.getStatus() != EscalationStatus.NURSE_ASSESSING) {
            throw new BusinessException("家属尚未补充症状信息，无法完成电话评估（可先为家属代登记）");
        }
        User nurse = SecurityUtils.currentUser();
        esc.setNurseAssessment(req.content());
        esc.setNurseDecision(req.decision().name());
        esc.setNurse(nurse);
        esc.setNurseAssessedAt(LocalDateTime.now());

        Alert alert = esc.getAlert();
        if (alert != null) {
            alert.setNurse(nurse);
            alert.setHandledAt(LocalDateTime.now());
        }
        if (req.decision() == NurseDecision.ESCALATE_DOCTOR) {
            esc.setStatus(EscalationStatus.DOCTOR_REVIEW);
            if (alert != null) {
                alert.setStatus(AlertStatus.ESCALATED);
                alertRepository.save(alert);
            }
            escalationRepository.save(esc);
            timelineService.record(esc.getPatient(), TimelineEventType.ESCALATION_NURSE_ASSESSMENT, nurse,
                    "护士电话评估：转医生复核",
                    "评估内容：" + req.content() + "。相关动作保持暂停，等待医生复核处置。",
                    "painEscalation", esc.getId());
        } else {
            esc.setStatus(EscalationStatus.CLEARED);
            esc.setClearedBy(nurse.getName() + "（护士评估解除）");
            esc.setClearedAt(LocalDateTime.now());
            esc.setClearNote("护士电话评估判断风险可控：" + req.content());
            if (alert != null) {
                alert.setStatus(AlertStatus.RESOLVED);
                alert.setResolvedAt(LocalDateTime.now());
                alertRepository.save(alert);
            }
            escalationRepository.save(esc);
            timelineService.record(esc.getPatient(), TimelineEventType.ESCALATION_NURSE_ASSESSMENT, nurse,
                    "护士电话评估：继续观察，恢复训练",
                    "评估内容：" + req.content() + "。无需医生介入，解除动作暂停。",
                    "painEscalation", esc.getId());
            timelineService.record(esc.getPatient(), TimelineEventType.ESCALATION_CLEARED, nurse,
                    "风险解除：「" + esc.getSuspendedItemNames() + "」恢复训练",
                    "经护士电话评估风险可控，相关动作重新进入每日训练任务。",
                    "painEscalation", esc.getId());
        }
        return esc;
    }

    // ---------- 医生复核处置 ----------

    @Transactional
    public PainEscalation doctorDisposition(Long id, Dtos.DoctorDispositionRequest req) {
        SecurityUtils.requireRole(Role.DOCTOR, Role.ADMIN);
        PainEscalation esc = getOne(id);
        if (esc.getStatus() != EscalationStatus.DOCTOR_REVIEW) {
            throw new BusinessException("该处置单不在待医生复核状态");
        }
        if (req.dispositions() == null || req.dispositions().isEmpty()) {
            throw new BusinessException("请至少选择一种处置方式");
        }
        User doctor = SecurityUtils.currentUser();
        String dispositionText = req.dispositions().stream().map(Disposition::getLabel).collect(Collectors.joining("、"));
        esc.setDoctorDispositions(req.dispositions().stream().map(Enum::name).collect(Collectors.joining(",")));
        esc.setDoctorConclusion(req.conclusion());
        esc.setReviewDate(req.reviewDate());
        esc.setDoctor(doctor);
        esc.setDoctorReviewedAt(LocalDateTime.now());
        esc.setStatus(EscalationStatus.DISPOSITION_ACTIVE);

        // 门诊复诊 / 影像检查 → 同步患者复诊计划
        boolean needVisit = req.dispositions().contains(Disposition.OUTPATIENT)
                || req.dispositions().contains(Disposition.IMAGING);
        if (needVisit && req.reviewDate() != null) {
            Patient patient = esc.getPatient();
            patient.setNextReviewDate(req.reviewDate());
            patientRepository.save(patient);
        }

        // 医生已介入：关联预警标记解决（动作暂停仍由处置单控制，直至风险解除）
        if (esc.getAlert() != null) {
            Alert alert = esc.getAlert();
            alert.setDoctor(doctor);
            alert.setStatus(AlertStatus.RESOLVED);
            alert.setResolvedAt(LocalDateTime.now());
            alertRepository.save(alert);
        }
        PainEscalation saved = escalationRepository.save(esc);
        timelineService.record(esc.getPatient(), TimelineEventType.ESCALATION_DOCTOR_DISPOSITION, doctor,
                "医生处置结论：" + dispositionText,
                req.conclusion()
                        + (req.reviewDate() != null ? "；复诊/检查日期：" + req.reviewDate() : "")
                        + "。结论已同步治疗师与家属；「" + esc.getSuspendedItemNames() + "」保持暂停，待风险解除后恢复。",
                "painEscalation", saved.getId());
        return saved;
    }

    // ---------- 风险解除 ----------

    @Transactional
    public PainEscalation clear(Long id, Dtos.ClearRiskRequest req) {
        SecurityUtils.requireRole(Role.DOCTOR, Role.ADMIN);
        PainEscalation esc = getOne(id);
        if (esc.getStatus() != EscalationStatus.DISPOSITION_ACTIVE) {
            throw new BusinessException("仅医生已处置的升级单可以解除风险");
        }
        User doctor = SecurityUtils.currentUser();
        esc.setStatus(EscalationStatus.CLEARED);
        esc.setClearedBy(doctor.getName());
        esc.setClearedAt(LocalDateTime.now());
        esc.setClearNote(req == null ? null : req.note());
        PainEscalation saved = escalationRepository.save(esc);
        timelineService.record(esc.getPatient(), TimelineEventType.ESCALATION_CLEARED, doctor,
                "风险解除：「" + esc.getSuspendedItemNames() + "」恢复训练",
                (req != null && req.note() != null && !req.note().isBlank() ? req.note() : "医生确认风险已解除")
                        + "。相关动作重新进入每日训练任务。",
                "painEscalation", saved.getId());
        return saved;
    }

    // ---------- 查询 ----------

    public List<PainEscalation> listAll(EscalationStatus status) {
        SecurityUtils.requireRole(Role.NURSE, Role.DOCTOR, Role.THERAPIST, Role.ADMIN);
        return status == null ? escalationRepository.findAllByOrderByCreatedAtDesc()
                : escalationRepository.findByStatusOrderByCreatedAtDesc(status);
    }

    public List<PainEscalation> listByPatient(Long patientId) {
        patientService.getAccessible(patientId);
        return escalationRepository.findByPatientIdOrderByCreatedAtDesc(patientId);
    }

    public PainEscalation getOne(Long id) {
        return escalationRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "疼痛升级处置单不存在"));
    }

    /** 未解除（动作仍暂停中）的升级单 */
    public List<PainEscalation> openEscalations(Long patientId) {
        return escalationRepository.findByPatientIdAndStatusNotOrderByCreatedAtDesc(
                patientId, EscalationStatus.CLEARED);
    }

    /** 当前被暂停的处方项目 id 集合（今日任务过滤用） */
    public Set<Long> suspendedItemIds(Long patientId) {
        Set<Long> ids = new HashSet<>();
        for (PainEscalation esc : openEscalations(patientId)) {
            ids.addAll(parseIds(esc.getSuspendedItemIds()));
        }
        return ids;
    }

    public long countOpen() {
        return escalationRepository.countByStatusNot(EscalationStatus.CLEARED);
    }

    // ---------- 工具 ----------

    public List<Long> parseIds(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<Long>>() {
            });
        } catch (JsonProcessingException e) {
            return List.of();
        }
    }

    public static String triggerLabels(String triggers) {
        if (triggers == null || triggers.isBlank()) {
            return "";
        }
        return Arrays.stream(triggers.split(","))
                .map(t -> {
                    try {
                        return EscalationTrigger.valueOf(t.trim()).getLabel();
                    } catch (IllegalArgumentException e) {
                        return t;
                    }
                })
                .collect(Collectors.joining("、"));
    }

    public static String dispositionLabels(String dispositions) {
        if (dispositions == null || dispositions.isBlank()) {
            return "待医生处置";
        }
        return Arrays.stream(dispositions.split(","))
                .map(d -> {
                    try {
                        return Disposition.valueOf(d.trim()).getLabel();
                    } catch (IllegalArgumentException e) {
                        return d;
                    }
                })
                .collect(Collectors.joining("、"));
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }
}
