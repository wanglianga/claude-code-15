package com.rehab.platform.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rehab.platform.config.BusinessException;
import com.rehab.platform.config.SecurityUtils;
import com.rehab.platform.dto.Dtos;
import com.rehab.platform.enums.*;
import com.rehab.platform.model.*;
import com.rehab.platform.repository.CorrectionTaskRepository;
import com.rehab.platform.repository.PrescriptionRepository;
import com.rehab.platform.repository.TrainingLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class TrainingLogService {

    private final TrainingLogRepository trainingLogRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final PatientService patientService;
    private final TimelineService timelineService;
    private final AlertService alertService;
    private final StatsService statsService;
    private final CorrectionTaskService correctionTaskService;
    private final CorrectionTaskRepository correctionTaskRepository;
    private final PainEscalationService painEscalationService;
    private final CaregiverHandoverService handoverService;
    private final ObjectMapper objectMapper;

    /** 今日任务 = 当前执行中处方（剔除疼痛升级暂停的动作） + 待确认/进行中的纠错任务 */
    public Map<String, Object> todayTasks(Long patientId) {
        Patient patient = patientService.getAccessible(patientId);
        Prescription active = prescriptionRepository
                .findByPatientIdAndStatus(patientId, PrescriptionStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(404, "当前没有执行中的处方，请联系治疗师"));
        TrainingLog todayLog = trainingLogRepository.findByPatientIdAndLogDate(patientId, LocalDate.now()).orElse(null);

        // 疼痛升级：解除风险前，相关动作不出现在每日训练任务中
        Set<Long> suspendedIds = painEscalationService.suspendedItemIds(patientId);
        List<PrescriptionItem> visibleItems = active.getItems().stream()
                .filter(i -> !suspendedIds.contains(i.getId()))
                .toList();
        List<PrescriptionItem> suspendedItems = active.getItems().stream()
                .filter(i -> suspendedIds.contains(i.getId()))
                .toList();
        Map<String, Object> rx = new HashMap<>();
        rx.put("id", active.getId());
        rx.put("phase", active.getPhase());
        rx.put("status", active.getStatus());
        rx.put("startDate", active.getStartDate());
        rx.put("endDate", active.getEndDate());
        rx.put("nextReviewDate", active.getNextReviewDate());
        rx.put("painThreshold", active.getPainThreshold());
        rx.put("notes", active.getNotes());
        rx.put("adjustReason", active.getAdjustReason());
        rx.put("items", visibleItems);

        Map<String, Object> result = new HashMap<>();
        result.put("patient", patient);
        result.put("prescription", rx);
        result.put("suspendedItems", suspendedItems);
        result.put("openEscalations", painEscalationService.openEscalations(patientId));
        // 照护人交接：待新照护人确认（打卡闸门）/ 首周观察期内的交接（首周标记）
        result.put("pendingHandover", handoverService.pendingForPatient(patientId));
        result.put("firstWeekHandover", handoverService.activeFirstWeek(patientId));
        result.put("todayLog", todayLog == null ? "" : todayLog);
        result.put("logSubmitted", todayLog != null);
        result.put("pendingCorrections", correctionTaskService.pendingConfirmations(patientId));
        result.put("activeCorrections", correctionTaskRepository
                .findByPatientIdAndStatusIn(patientId,
                        List.of(CorrectionStatus.CONFIRMED, CorrectionStatus.NOT_MASTERED, CorrectionStatus.RECHECK)));
        return result;
    }

    /** 提交每日训练打卡（同日重复提交则覆盖更新），并运行预警规则引擎 */
    @Transactional
    public TrainingLog submit(Long patientId, Dtos.TrainingLogRequest req) {
        Patient patient = patientService.getAccessible(patientId);
        User submitter = SecurityUtils.currentUser();

        // 闸门：照护人更换后，新照护人未完成三项确认前禁止打卡
        var pendingHandover = handoverService.pendingForPatient(patientId);
        if (pendingHandover != null) {
            throw new BusinessException("照护人已更换为「" + pendingHandover.getNewCaregiverName()
                    + "」，请先完成动作注意事项、禁忌风险、器具使用三项确认后再打卡");
        }

        // 闸门：存在未确认的视频纠错任务时禁止打卡
        var pendingCorrections = correctionTaskService.pendingConfirmations(patientId);
        if (!pendingCorrections.isEmpty()) {
            throw new BusinessException("有 " + pendingCorrections.size()
                    + " 条视频纠错尚未确认观看，请先在「今日训练」页面观看纠错内容并确认后再打卡");
        }

        Prescription active = prescriptionRepository
                .findByPatientIdAndStatus(patientId, PrescriptionStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException("当前没有执行中的处方，无法打卡"));

        LocalDate logDate = req.logDate() == null ? LocalDate.now() : req.logDate();
        TrainingLog log = trainingLogRepository.findByPatientIdAndLogDate(patientId, logDate).orElse(null);
        boolean isNew = log == null;
        if (isNew) {
            log = new TrainingLog();
            log.setPatient(patient);
            log.setLogDate(logDate);
        }
        log.setPrescription(active);
        log.setSubmittedBy(submitter);
        log.setCompletionRate(req.completionRate());
        log.setPainBefore(req.painBefore());
        log.setPainAfter(req.painAfter());
        log.setFamilyNote(req.familyNote());
        log.setCompanionAvailable(req.companionAvailable() == null || req.companionAvailable());
        log.setCompensationObserved(Boolean.TRUE.equals(req.compensationObserved()));
        log.setSwellingNumbness(Boolean.TRUE.equals(req.swellingNumbness()));
        log.setNightPainWorse(Boolean.TRUE.equals(req.nightPainWorse()));
        log.setCompletedDetail(toJson(req.items()));
        log.setAbnormalPhotos(toJson(req.abnormalPhotos()));
        log.setVideoClips(toJson(req.videoClips()));
        TrainingLog saved = trainingLogRepository.save(log);

        timelineService.record(patient, TimelineEventType.TRAINING_LOG_SUBMITTED, submitter,
                (isNew ? "" : "更新") + "居家训练打卡（" + logDate + "）：完成率 " + req.completionRate()
                        + "%，疼痛 " + nullToDash(req.painBefore()) + " → " + nullToDash(req.painAfter()) + " 分",
                req.familyNote(), "trainingLog", saved.getId());

        // 新视频自动关联到等待复评的纠错任务（与旧问题对比）
        correctionTaskService.onNewTrainingLog(patient, saved);

        // 疼痛升级处置：疼痛超阈值 / 肿胀麻木 / 夜间痛加重 → 暂停相关动作，走家属补充→护士评估→医生复核流
        var escalation = painEscalationService.checkAndCreate(patient, active, saved, req);

        runRules(patient, active, saved, escalation != null);
        return saved;
    }

    /**
     * 预警规则引擎：
     * 1. 疼痛升高：训练后疼痛 ≥ 处方疼痛阈值，或较训练前升高 ≥2 分（已触发疼痛升级处置时不重复预警）
     * 2. 动作代偿明显
     * 3. 家属无法陪练
     * 4. 连续漏练 ≥2 天
     */
    private void runRules(Patient patient, Prescription prescription, TrainingLog log, boolean painEscalated) {
        Integer threshold = prescription.getPainThreshold() == null ? 6 : prescription.getPainThreshold();
        if (!painEscalated && log.getPainAfter() != null) {
            if (log.getPainAfter() >= threshold) {
                alertService.createIfAbsent(patient, AlertType.PAIN_RISE,
                        log.getPainAfter() >= 8 ? AlertLevel.HIGH : AlertLevel.MEDIUM,
                        "训练后疼痛 " + log.getPainAfter() + " 分，达到/超过处方疼痛阈值 " + threshold + " 分",
                        log.getId());
            } else if (log.getPainBefore() != null && log.getPainAfter() - log.getPainBefore() >= 2) {
                alertService.createIfAbsent(patient, AlertType.PAIN_RISE, AlertLevel.MEDIUM,
                        "训练后疼痛较训练前升高 " + (log.getPainAfter() - log.getPainBefore())
                                + " 分（" + log.getPainBefore() + " → " + log.getPainAfter() + "）",
                        log.getId());
            }
        }
        if (Boolean.TRUE.equals(log.getCompensationObserved())) {
            alertService.createIfAbsent(patient, AlertType.COMPENSATION, AlertLevel.MEDIUM,
                    "家属观察到明显动作代偿，需要治疗师视频纠错", log.getId());
        }
        if (Boolean.FALSE.equals(log.getCompanionAvailable())) {
            alertService.createIfAbsent(patient, AlertType.NO_COMPANION, AlertLevel.MEDIUM,
                    "家属反馈无法陪练，居家训练缺少监督", log.getId());
        }
        int missed = statsService.consecutiveMissedDays(patient);
        if (missed >= 2) {
            alertService.createIfAbsent(patient, AlertType.MISSED_TRAINING,
                    missed >= 4 ? AlertLevel.HIGH : AlertLevel.MEDIUM,
                    "患者已连续 " + missed + " 天未提交居家训练打卡", null);
        }
    }

    public List<TrainingLog> listByPatient(Long patientId, Integer days) {
        patientService.getAccessible(patientId);
        if (days != null && days > 0) {
            LocalDate end = LocalDate.now();
            return trainingLogRepository.findByPatientIdAndLogDateBetweenOrderByLogDateAsc(
                    patientId, end.minusDays(days - 1L), end);
        }
        return trainingLogRepository.findByPatientIdOrderByLogDateDesc(patientId);
    }

    /** 视频纠错记录（有治疗师反馈的打卡） */
    public List<TrainingLog> corrections(Long patientId) {
        patientService.getAccessible(patientId);
        return trainingLogRepository.findByPatientIdAndTherapistFeedbackIsNotNullOrderByLogDateDesc(patientId);
    }

    /** 治疗师对打卡视频/照片写纠错反馈 */
    @Transactional
    public TrainingLog feedback(Long logId, Dtos.FeedbackRequest req) {
        SecurityUtils.requireRole(Role.THERAPIST, Role.ADMIN);
        TrainingLog log = trainingLogRepository.findById(logId)
                .orElseThrow(() -> new BusinessException(404, "打卡记录不存在"));
        patientService.getAccessible(log.getPatient().getId());
        User therapist = SecurityUtils.currentUser();
        log.setTherapistFeedback(req.feedback());
        log.setFeedbackBy(therapist.getName());
        log.setFeedbackAt(LocalDateTime.now());
        TrainingLog saved = trainingLogRepository.save(log);
        timelineService.record(log.getPatient(), TimelineEventType.VIDEO_CORRECTED, therapist,
                "视频纠错（" + log.getLogDate() + " 打卡）", req.feedback(), "trainingLog", log.getId());
        return saved;
    }

    private String toJson(Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private String nullToDash(Integer v) {
        return v == null ? "—" : String.valueOf(v);
    }
}
