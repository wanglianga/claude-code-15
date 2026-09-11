package com.rehab.platform.service;

import com.rehab.platform.config.BusinessException;
import com.rehab.platform.config.SecurityUtils;
import com.rehab.platform.dto.Dtos;
import com.rehab.platform.enums.*;
import com.rehab.platform.model.*;
import com.rehab.platform.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AlertService {

    private final AlertRepository alertRepository;
    private final NurseFollowupRepository followupRepository;
    private final DoctorInterventionRepository interventionRepository;
    private final PatientRepository patientRepository;
    private final PatientService patientService;
    private final TimelineService timelineService;
    private final StatsService statsService;

    private static final List<AlertStatus> OPEN_STATUSES =
            List.of(AlertStatus.PENDING, AlertStatus.FOLLOWING, AlertStatus.ESCALATED);

    public List<Alert> list(AlertStatus status) {
        SecurityUtils.requireRole(Role.NURSE, Role.DOCTOR, Role.THERAPIST, Role.ADMIN);
        if (status != null) {
            return alertRepository.findByStatusOrderByCreatedAtDesc(status);
        }
        return alertRepository.findAllByOrderByCreatedAtDesc();
    }

    public List<Alert> listByPatient(Long patientId) {
        patientService.getAccessible(patientId);
        return alertRepository.findByPatientIdOrderByCreatedAtDesc(patientId);
    }

    public Alert getOne(Long id) {
        return alertRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "预警不存在"));
    }

    /** 规则引擎调用：若同类未处理预警已存在则不重复创建 */
    public Alert createIfAbsent(Patient patient, AlertType type, AlertLevel level, String message, Long sourceLogId) {
        List<Alert> existing = alertRepository.findByPatientIdAndTypeAndStatusIn(patient.getId(), type, OPEN_STATUSES);
        if (!existing.isEmpty()) {
            return existing.get(0);
        }
        Alert alert = new Alert();
        alert.setPatient(patient);
        alert.setType(type);
        alert.setLevel(level);
        alert.setMessage(message);
        alert.setSourceLogId(sourceLogId);
        Alert saved = alertRepository.save(alert);
        timelineService.record(patient, TimelineEventType.ALERT_CREATED, null,
                "触发预警：" + type.getLabel() + "（" + level.getLabel() + "风险）",
                message + "。已推送康复护士电话随访队列。", "alert", saved.getId());
        return saved;
    }

    /** 护士接单 */
    @Transactional
    public Alert assign(Long id) {
        SecurityUtils.requireRole(Role.NURSE, Role.ADMIN);
        Alert alert = getOne(id);
        if (alert.getStatus() != AlertStatus.PENDING) {
            throw new BusinessException("该预警已被处理或正在随访中");
        }
        User nurse = SecurityUtils.currentUser();
        alert.setNurse(nurse);
        alert.setStatus(AlertStatus.FOLLOWING);
        alert.setHandledAt(LocalDateTime.now());
        Alert saved = alertRepository.save(alert);
        timelineService.record(alert.getPatient(), TimelineEventType.ALERT_ASSIGNED, nurse,
                "护士接单：" + alert.getType().getLabel(),
                nurse.getName() + " 开始电话随访。", "alert", alert.getId());
        return saved;
    }

    /** 护士记录电话随访 */
    @Transactional
    public NurseFollowup addFollowup(Long alertId, Dtos.FollowupRequest req) {
        SecurityUtils.requireRole(Role.NURSE, Role.ADMIN);
        Alert alert = getOne(alertId);
        User nurse = SecurityUtils.currentUser();
        if (alert.getStatus() == AlertStatus.PENDING) {
            alert.setNurse(nurse);
            alert.setStatus(AlertStatus.FOLLOWING);
            alert.setHandledAt(LocalDateTime.now());
            alertRepository.save(alert);
        }
        NurseFollowup followup = new NurseFollowup();
        followup.setAlert(alert);
        followup.setPatient(alert.getPatient());
        followup.setNurse(nurse);
        followup.setMethod(req.method() == null || req.method().isBlank() ? "电话" : req.method());
        followup.setContent(req.content());
        followup.setOutcome(req.outcome());
        followup.setNextAction(req.nextAction());
        NurseFollowup saved = followupRepository.save(followup);
        timelineService.record(alert.getPatient(), TimelineEventType.NURSE_FOLLOWUP, nurse,
                "电话随访（" + alert.getType().getLabel() + "）：" + req.outcome(),
                "随访内容：" + req.content()
                        + (req.nextAction() != null && !req.nextAction().isBlank() ? "；后续安排：" + req.nextAction() : ""),
                "followup", saved.getId());
        return saved;
    }

    public List<NurseFollowup> followups(Long alertId) {
        return followupRepository.findByAlertIdOrderByCreatedAtDesc(alertId);
    }

    /** 转医生处理 */
    @Transactional
    public Alert escalate(Long id) {
        SecurityUtils.requireRole(Role.NURSE, Role.THERAPIST, Role.ADMIN);
        Alert alert = getOne(id);
        alert.setStatus(AlertStatus.ESCALATED);
        Alert saved = alertRepository.save(alert);
        timelineService.record(alert.getPatient(), TimelineEventType.ALERT_ESCALATED, SecurityUtils.currentUser(),
                "预警升级：转医生处理（" + alert.getType().getLabel() + "）",
                alert.getMessage(), "alert", alert.getId());
        return saved;
    }

    /** 医生填写处理意见 */
    @Transactional
    public DoctorIntervention doctorOpinion(Long alertId, Dtos.DoctorOpinionRequest req) {
        SecurityUtils.requireRole(Role.DOCTOR, Role.ADMIN);
        Alert alert = getOne(alertId);
        User doctor = SecurityUtils.currentUser();
        DoctorIntervention intervention = new DoctorIntervention();
        intervention.setPatient(alert.getPatient());
        intervention.setAlert(alert);
        intervention.setDoctor(doctor);
        intervention.setOpinion(req.opinion());
        DoctorIntervention saved = interventionRepository.save(intervention);
        alert.setDoctor(doctor);
        alertRepository.save(alert);
        timelineService.record(alert.getPatient(), TimelineEventType.DOCTOR_OPINION, doctor,
                "医生处理意见（" + alert.getType().getLabel() + "）",
                req.opinion(), "intervention", saved.getId());
        return saved;
    }

    /** 解除预警 */
    @Transactional
    public Alert resolve(Long id) {
        SecurityUtils.requireRole(Role.NURSE, Role.DOCTOR, Role.THERAPIST, Role.ADMIN);
        Alert alert = getOne(id);
        alert.setStatus(AlertStatus.RESOLVED);
        alert.setResolvedAt(LocalDateTime.now());
        Alert saved = alertRepository.save(alert);
        timelineService.record(alert.getPatient(), TimelineEventType.ALERT_RESOLVED, SecurityUtils.currentUser(),
                "预警解除：" + alert.getType().getLabel(),
                alert.getMessage(), "alert", alert.getId());
        return saved;
    }

    /** 每日扫描：连续漏练 ≥2 天自动生成预警（也可由打卡/查询时触发） */
    public void scanMissedTraining() {
        for (Patient patient : patientRepository.findAll()) {
            int missed = statsService.consecutiveMissedDays(patient);
            if (missed >= 2) {
                createIfAbsent(patient, AlertType.MISSED_TRAINING,
                        missed >= 4 ? AlertLevel.HIGH : AlertLevel.MEDIUM,
                        "患者已连续 " + missed + " 天未提交居家训练打卡", null);
            }
        }
    }
}
