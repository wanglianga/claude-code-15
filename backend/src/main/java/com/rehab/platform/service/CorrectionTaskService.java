package com.rehab.platform.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rehab.platform.config.BusinessException;
import com.rehab.platform.config.SecurityUtils;
import com.rehab.platform.dto.Dtos;
import com.rehab.platform.enums.*;
import com.rehab.platform.model.*;
import com.rehab.platform.repository.CorrectionTaskRepository;
import com.rehab.platform.repository.PatientRepository;
import com.rehab.platform.repository.PrescriptionItemRepository;
import com.rehab.platform.repository.TrainingLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CorrectionTaskService {

    private final CorrectionTaskRepository correctionTaskRepository;
    private final TrainingLogRepository trainingLogRepository;
    private final PrescriptionItemRepository prescriptionItemRepository;
    private final PatientRepository patientRepository;
    private final PatientService patientService;
    private final TimelineService timelineService;
    private final AlertService alertService;
    private final ObjectMapper objectMapper;

    /** 连续未掌握达到该次数 → 自动追加线下复评并预警 */
    private static final int AUTO_REVIEW_THRESHOLD = 2;

    public List<CorrectionTask> listByPatient(Long patientId) {
        patientService.getAccessible(patientId);
        return correctionTaskRepository.findByPatientIdOrderByCreatedAtDesc(patientId);
    }

    /** 待患者确认的纠错任务（打卡前置闸门） */
    public List<CorrectionTask> pendingConfirmations(Long patientId) {
        return correctionTaskRepository.findByPatientIdAndStatus(patientId, CorrectionStatus.PENDING_CONFIRM);
    }

    /** 治疗师打回视频：标注关键动作点 + 纠错说明 */
    @Transactional
    public CorrectionTask create(Long logId, Dtos.CorrectionTaskRequest req) {
        SecurityUtils.requireRole(Role.THERAPIST, Role.ADMIN);
        TrainingLog log = trainingLogRepository.findById(logId)
                .orElseThrow(() -> new BusinessException(404, "打卡记录不存在"));
        Patient patient = patientService.getAccessible(log.getPatient().getId());
        if (log.getVideoClips() == null || log.getVideoClips().isBlank() || "[]".equals(log.getVideoClips())) {
            throw new BusinessException("该打卡没有训练视频，无法打回纠错");
        }
        if (req.keyPoints() == null || req.keyPoints().isEmpty()) {
            throw new BusinessException("请至少标注一个关键动作点");
        }

        CorrectionTask task = new CorrectionTask();
        task.setPatient(patient);
        task.setSourceLog(log);
        if (req.prescriptionItemId() != null) {
            prescriptionItemRepository.findById(req.prescriptionItemId()).ifPresent(task::setPrescriptionItem);
        }
        task.setKeyPoints(toJson(req.keyPoints()));
        task.setCorrectionNote(req.correctionNote());
        CorrectionTask saved = correctionTaskRepository.save(task);

        timelineService.record(patient, TimelineEventType.CORRECTION_CREATED, SecurityUtils.currentUser(),
                "视频打回纠错：" + exerciseName(task) + "（" + log.getLogDate() + " 打卡）",
                "关键动作点：" + keyPointSummary(req.keyPoints()) + "。纠错说明：" + req.correctionNote()
                        + "。患者下次训练前必须确认观看。",
                "correctionTask", saved.getId());
        return saved;
    }

    /** 患者/家属确认已观看纠错内容 */
    @Transactional
    public CorrectionTask confirm(Long taskId) {
        CorrectionTask task = getAccessibleTask(taskId);
        if (task.getStatus() != CorrectionStatus.PENDING_CONFIRM) {
            throw new BusinessException("该纠错任务当前无需确认（状态：" + task.getStatus().getLabel() + "）");
        }
        User user = SecurityUtils.currentUser();
        task.setStatus(CorrectionStatus.CONFIRMED);
        task.setConfirmedBy(user.getName());
        task.setConfirmedAt(LocalDateTime.now());
        CorrectionTask saved = correctionTaskRepository.save(task);
        timelineService.record(task.getPatient(), TimelineEventType.CORRECTION_CONFIRMED, user,
                "已确认观看纠错内容：" + exerciseName(task),
                "关键动作点已知晓，下次训练将拍摄视频供对比复评。", "correctionTask", task.getId());
        return saved;
    }

    /**
     * 新打卡提交后调用：若带有训练视频，把等待复评的纠错任务关联到新视频上
     */
    @Transactional
    public void onNewTrainingLog(Patient patient, TrainingLog newLog) {
        if (newLog.getVideoClips() == null || newLog.getVideoClips().isBlank()
                || "[]".equals(newLog.getVideoClips())) {
            return;
        }
        List<CorrectionTask> waiting = correctionTaskRepository.findByPatientIdAndStatusIn(patient.getId(),
                List.of(CorrectionStatus.CONFIRMED, CorrectionStatus.NOT_MASTERED));
        for (CorrectionTask task : waiting) {
            task.setStatus(CorrectionStatus.RECHECK);
            task.setRecheckLog(newLog);
            correctionTaskRepository.save(task);
            timelineService.record(patient, TimelineEventType.CORRECTION_RECHECK, null,
                    "复评视频已提交：" + exerciseName(task),
                    "患者上传了新的训练视频，系统已与 " + task.getSourceLog().getLogDate()
                            + " 被纠错的旧视频关联，等待治疗师对比复评。",
                    "correctionTask", task.getId());
        }
    }

    /** 治疗师复评：对比新旧视频，判断是否真正掌握 */
    @Transactional
    public CorrectionTask review(Long taskId, Dtos.CorrectionReviewRequest req) {
        SecurityUtils.requireRole(Role.THERAPIST, Role.ADMIN);
        CorrectionTask task = getAccessibleTask(taskId);
        if (task.getStatus() != CorrectionStatus.RECHECK) {
            throw new BusinessException("该纠错任务不在待复评状态（当前：" + task.getStatus().getLabel() + "）");
        }
        User therapist = SecurityUtils.currentUser();
        task.setReviewedBy(therapist.getName());
        task.setReviewedAt(LocalDateTime.now());
        task.setReviewNote(req.reviewNote());
        Patient patient = task.getPatient();

        if (Boolean.TRUE.equals(req.mastered())) {
            task.setStatus(CorrectionStatus.MASTERED);
            task.setConsecutiveErrors(0);
            timelineService.record(patient, TimelineEventType.CORRECTION_REVIEWED, therapist,
                    "纠错复评：已掌握 ✓ " + exerciseName(task),
                    "对比新旧视频后判定患者已真正掌握动作。" + noteSuffix(req.reviewNote()),
                    "correctionTask", task.getId());
        } else {
            task.setStatus(CorrectionStatus.NOT_MASTERED);
            task.setConsecutiveErrors(task.getConsecutiveErrors() + 1);
            timelineService.record(patient, TimelineEventType.CORRECTION_REVIEWED, therapist,
                    "纠错复评：未掌握（连续第 " + task.getConsecutiveErrors() + " 次）" + exerciseName(task),
                    "旧问题仍未纠正，需继续练习。" + noteSuffix(req.reviewNote()),
                    "correctionTask", task.getId());
            if (task.getConsecutiveErrors() >= AUTO_REVIEW_THRESHOLD) {
                autoEscalate(patient, task);
            }
        }
        return correctionTaskRepository.save(task);
    }

    /** 连续未掌握 → 自动追加线下复评 + 高风险预警（建议评估降低训练强度） */
    private void autoEscalate(Patient patient, CorrectionTask task) {
        LocalDate reviewDate = LocalDate.now().plusDays(3);
        if (patient.getNextReviewDate() == null || patient.getNextReviewDate().isAfter(reviewDate)) {
            patient.setNextReviewDate(reviewDate);
            patientRepository.save(patient);
        }
        timelineService.record(patient, TimelineEventType.REVIEW_PLANNED, null,
                "连续 " + task.getConsecutiveErrors() + " 次视频纠错未掌握，系统自动追加线下复评（"
                        + patient.getNextReviewDate() + "）",
                "动作「" + exerciseName(task) + "」居家纠正效果不佳，需线下复评；建议治疗师同时评估是否降低训练强度。",
                "correctionTask", task.getId());
        alertService.createIfAbsent(patient, AlertType.VIDEO_CORRECTION, AlertLevel.HIGH,
                "动作「" + exerciseName(task) + "」连续 " + task.getConsecutiveErrors()
                        + " 次视频纠错未掌握，已自动追加线下复评（" + patient.getNextReviewDate()
                        + "），请评估是否降低训练强度",
                task.getSourceLog().getId());
    }

    private CorrectionTask getAccessibleTask(Long taskId) {
        CorrectionTask task = correctionTaskRepository.findById(taskId)
                .orElseThrow(() -> new BusinessException(404, "纠错任务不存在"));
        patientService.getAccessible(task.getPatient().getId());
        return task;
    }

    private String exerciseName(CorrectionTask task) {
        return task.getPrescriptionItem() != null ? task.getPrescriptionItem().getExerciseName() : "训练动作";
    }

    private String keyPointSummary(List<Dtos.KeyPointTag> keyPoints) {
        StringBuilder sb = new StringBuilder();
        for (Dtos.KeyPointTag tag : keyPoints) {
            String label = tag.code();
            try {
                label = KeyPoint.valueOf(tag.code()).getLabel();
            } catch (Exception ignored) {
            }
            if (sb.length() > 0) {
                sb.append("；");
            }
            sb.append(label);
            if (tag.timestamp() != null && !tag.timestamp().isBlank()) {
                sb.append("（").append(tag.timestamp()).append("）");
            }
            if (tag.note() != null && !tag.note().isBlank()) {
                sb.append("：").append(tag.note());
            }
        }
        return sb.toString();
    }

    private String noteSuffix(String note) {
        return note == null || note.isBlank() ? "" : " 复评备注：" + note;
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }
}
