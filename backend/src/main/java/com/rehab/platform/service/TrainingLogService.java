package com.rehab.platform.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rehab.platform.config.BusinessException;
import com.rehab.platform.config.SecurityUtils;
import com.rehab.platform.dto.Dtos;
import com.rehab.platform.enums.*;
import com.rehab.platform.model.*;
import com.rehab.platform.repository.PrescriptionRepository;
import com.rehab.platform.repository.TrainingLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TrainingLogService {

    private final TrainingLogRepository trainingLogRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final PatientService patientService;
    private final TimelineService timelineService;
    private final AlertService alertService;
    private final StatsService statsService;
    private final ObjectMapper objectMapper;

    /** 今日任务 = 当前执行中处方 */
    public Map<String, Object> todayTasks(Long patientId) {
        Patient patient = patientService.getAccessible(patientId);
        Prescription active = prescriptionRepository
                .findByPatientIdAndStatus(patientId, PrescriptionStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(404, "当前没有执行中的处方，请联系治疗师"));
        TrainingLog todayLog = trainingLogRepository.findByPatientIdAndLogDate(patientId, LocalDate.now()).orElse(null);
        return Map.of(
                "patient", patient,
                "prescription", active,
                "todayLog", todayLog == null ? "" : todayLog,
                "logSubmitted", todayLog != null);
    }

    /** 提交每日训练打卡（同日重复提交则覆盖更新），并运行预警规则引擎 */
    @Transactional
    public TrainingLog submit(Long patientId, Dtos.TrainingLogRequest req) {
        Patient patient = patientService.getAccessible(patientId);
        User submitter = SecurityUtils.currentUser();
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
        log.setCompletedDetail(toJson(req.items()));
        log.setAbnormalPhotos(toJson(req.abnormalPhotos()));
        log.setVideoClips(toJson(req.videoClips()));
        TrainingLog saved = trainingLogRepository.save(log);

        timelineService.record(patient, TimelineEventType.TRAINING_LOG_SUBMITTED, submitter,
                (isNew ? "" : "更新") + "居家训练打卡（" + logDate + "）：完成率 " + req.completionRate()
                        + "%，疼痛 " + nullToDash(req.painBefore()) + " → " + nullToDash(req.painAfter()) + " 分",
                req.familyNote(), "trainingLog", saved.getId());

        runRules(patient, active, saved);
        return saved;
    }

    /**
     * 预警规则引擎：
     * 1. 疼痛升高：训练后疼痛 ≥ 处方疼痛阈值，或较训练前升高 ≥2 分
     * 2. 动作代偿明显
     * 3. 家属无法陪练
     * 4. 连续漏练 ≥2 天
     */
    private void runRules(Patient patient, Prescription prescription, TrainingLog log) {
        Integer threshold = prescription.getPainThreshold() == null ? 6 : prescription.getPainThreshold();
        if (log.getPainAfter() != null) {
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
