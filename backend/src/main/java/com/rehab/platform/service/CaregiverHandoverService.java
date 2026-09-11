package com.rehab.platform.service;

import com.rehab.platform.config.BusinessException;
import com.rehab.platform.config.SecurityUtils;
import com.rehab.platform.dto.Dtos;
import com.rehab.platform.enums.HandoverStatus;
import com.rehab.platform.enums.Role;
import com.rehab.platform.enums.TimelineEventType;
import com.rehab.platform.model.CaregiverHandover;
import com.rehab.platform.model.Patient;
import com.rehab.platform.model.TrainingLog;
import com.rehab.platform.model.User;
import com.rehab.platform.repository.CaregiverHandoverRepository;
import com.rehab.platform.repository.PatientRepository;
import com.rehab.platform.repository.TrainingLogRepository;
import com.rehab.platform.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 照护人更换交接：
 * 治疗师发起 → 新照护人完成 动作注意事项/禁忌风险/器具使用 三项确认（确认前禁止打卡）→
 * 旧照护人反馈记录保留 → 治疗师对比更换前后依从性与疼痛 → 首周新照护人反馈重点标记，护士电话指导。
 */
@Service
@RequiredArgsConstructor
public class CaregiverHandoverService {

    private final CaregiverHandoverRepository handoverRepository;
    private final PatientRepository patientRepository;
    private final UserRepository userRepository;
    private final TrainingLogRepository trainingLogRepository;
    private final PatientService patientService;
    private final TimelineService timelineService;

    // ---------- 发起交接（治疗师） ----------

    @Transactional
    public CaregiverHandover create(Long patientId, Dtos.HandoverRequest req) {
        SecurityUtils.requireRole(Role.THERAPIST, Role.ADMIN);
        Patient patient = patientService.getAccessible(patientId);
        handoverRepository.findFirstByPatientIdAndStatusOrderByCreatedAtDesc(patientId, HandoverStatus.PENDING_CONFIRM)
                .ifPresent(h -> {
                    throw new BusinessException("已有一单待确认的交接（新照护人：" + h.getNewCaregiverName() + "），请先完成或取消");
                });

        CaregiverHandover h = new CaregiverHandover();
        h.setPatient(patient);
        // 旧照护人快照（其历史反馈记录仍保留在患者档案，按提交人可追溯）
        h.setOldCaregiverName(patient.getCaregiverName());
        h.setOldCaregiverRelation(patient.getCaregiverRelation());
        if (patient.getFamilyUser() != null) {
            h.setOldFamilyUserId(patient.getFamilyUser().getId());
            h.setOldFamilyUserName(patient.getFamilyUser().getName());
        }
        h.setNewCaregiverName(req.newCaregiverName());
        h.setNewCaregiverRelation(req.newCaregiverRelation());
        h.setNewCaregiverPhone(req.newCaregiverPhone());
        h.setReason(req.reason());
        h.setCreatedBy(SecurityUtils.currentUser().getName());

        // 切换患者档案照护人与绑定账号（新照护人需登录完成三项确认）
        patient.setCaregiverName(req.newCaregiverName());
        patient.setCaregiverRelation(req.newCaregiverRelation());
        patient.setCaregiverPhone(req.newCaregiverPhone());
        if (req.newFamilyUserId() != null) {
            User newUser = userRepository.findById(req.newFamilyUserId())
                    .orElseThrow(() -> new BusinessException(404, "新照护人账号不存在"));
            if (newUser.getRole() != Role.FAMILY) {
                throw new BusinessException("只能绑定家属角色账号");
            }
            h.setNewFamilyUser(newUser);
            patient.setFamilyUser(newUser);
        } else {
            h.setNewFamilyUser(patient.getFamilyUser());
        }
        patientRepository.save(patient);
        CaregiverHandover saved = handoverRepository.save(h);

        timelineService.record(patient, TimelineEventType.CAREGIVER_HANDOVER_CREATED, SecurityUtils.currentUser(),
                "照护人更换：" + nullToDash(h.getOldCaregiverName()) + " → " + req.newCaregiverName(),
                "更换原因：" + nullToDash(req.reason())
                        + "。新照护人须完成动作注意事项、禁忌风险、器具使用三项确认后才能打卡；旧照护人历史反馈记录保留可查。",
                "handover", saved.getId());
        return saved;
    }

    // ---------- 新照护人三项确认 ----------

    @Transactional
    public CaregiverHandover confirm(Long id, Dtos.HandoverConfirmRequest req) {
        CaregiverHandover h = getOne(id);
        User user = SecurityUtils.currentUser();
        boolean proxy = false;
        if (user.getRole() == Role.FAMILY) {
            patientService.getAccessible(h.getPatient().getId());
        } else {
            SecurityUtils.requireRole(Role.THERAPIST, Role.ADMIN);
            proxy = true;
        }
        if (h.getStatus() != HandoverStatus.PENDING_CONFIRM) {
            throw new BusinessException("该交接已完成确认");
        }
        if (!Boolean.TRUE.equals(req.precautions()) || !Boolean.TRUE.equals(req.contraindications())
                || !Boolean.TRUE.equals(req.devices())) {
            throw new BusinessException("请逐项完成动作注意事项、禁忌风险、器具使用三项确认");
        }
        h.setPrecautionsConfirmed(true);
        h.setContraindicationsConfirmed(true);
        h.setDevicesConfirmed(true);
        h.setConfirmedBy(user.getName() + (proxy ? "（代确认）" : ""));
        h.setConfirmedAt(LocalDateTime.now());
        h.setFirstWeekEnd(LocalDate.now().plusDays(7));
        h.setStatus(HandoverStatus.CONFIRMED);
        CaregiverHandover saved = handoverRepository.save(h);

        timelineService.record(h.getPatient(), TimelineEventType.CAREGIVER_HANDOVER_CONFIRMED, user,
                "新照护人确认完成：" + h.getNewCaregiverName(),
                "已逐项确认动作注意事项、禁忌风险、器具使用。交接完成，首周（至 " + saved.getFirstWeekEnd()
                        + "）新照护人反馈将重点标记，护士关注是否需要额外电话指导。",
                "handover", saved.getId());
        return saved;
    }

    // ---------- 查询 ----------

    public List<CaregiverHandover> listByPatient(Long patientId) {
        patientService.getAccessible(patientId);
        return handoverRepository.findByPatientIdOrderByCreatedAtDesc(patientId);
    }

    public CaregiverHandover pendingForPatient(Long patientId) {
        return handoverRepository
                .findFirstByPatientIdAndStatusOrderByCreatedAtDesc(patientId, HandoverStatus.PENDING_CONFIRM)
                .orElse(null);
    }

    /** 首周观察期内的最新交接（用于家属端首周标记） */
    public CaregiverHandover activeFirstWeek(Long patientId) {
        return handoverRepository.findByPatientIdOrderByCreatedAtDesc(patientId).stream()
                .filter(h -> h.getStatus() == HandoverStatus.CONFIRMED
                        && h.getFirstWeekEnd() != null && !h.getFirstWeekEnd().isBefore(LocalDate.now()))
                .findFirst()
                .orElse(null);
    }

    public CaregiverHandover getOne(Long id) {
        return handoverRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "交接单不存在"));
    }

    // ---------- 治疗师对比：更换前后依从性与疼痛 ----------

    public Map<String, Object> comparison(Long handoverId) {
        CaregiverHandover h = getOne(handoverId);
        patientService.getAccessible(h.getPatient().getId());
        if (h.getStatus() != HandoverStatus.CONFIRMED) {
            throw new BusinessException("交接尚未确认完成，暂无更换后数据可对比");
        }
        LocalDate boundary = h.getCreatedAt().toLocalDate();
        LocalDate afterStart = h.getConfirmedAt().toLocalDate();
        Map<String, Object> before = windowStats(h.getPatient().getId(), boundary.minusDays(7), boundary.minusDays(1));
        Map<String, Object> after = windowStats(h.getPatient().getId(), afterStart, afterStart.plusDays(6));

        double completionDelta = ((Number) after.get("avgCompletion")).doubleValue()
                - ((Number) before.get("avgCompletion")).doubleValue();
        double adherenceDelta = ((Number) after.get("adherencePercent")).doubleValue()
                - ((Number) before.get("adherencePercent")).doubleValue();
        double painDelta = ((Number) after.get("avgPainAfter")).doubleValue()
                - ((Number) before.get("avgPainAfter")).doubleValue();

        String hint;
        if (((Number) after.get("loggedDays")).intValue() == 0) {
            hint = "新照护人首周暂无打卡数据，建议护士电话确认陪练安排是否落地";
        } else if (completionDelta <= -20 || adherenceDelta <= -30 || painDelta >= 1.5) {
            hint = "更换照护人后数据明显下滑（完成率 " + fmt(completionDelta) + "、依从性 " + fmt(adherenceDelta)
                    + "、疼痛 " + fmt(painDelta) + "），而更换前趋势平稳 → 倾向「陪练理解偏差」，"
                    + "建议护士对新照护人电话指导动作要点，治疗师可安排视频纠错重点复核";
        } else {
            hint = "更换前后数据接近 → 倾向患者自身状态延续，与照护人更换关系不大；若疼痛持续升高，请考虑患者病情变化";
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("handover", h);
        result.put("before", before);
        result.put("after", after);
        result.put("hint", hint);
        return result;
    }

    private Map<String, Object> windowStats(Long patientId, LocalDate start, LocalDate end) {
        List<TrainingLog> logs = trainingLogRepository
                .findByPatientIdAndLogDateBetweenOrderByLogDateAsc(patientId, start, end);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("start", start.toString());
        m.put("end", end.toString());
        m.put("loggedDays", logs.size());
        m.put("adherencePercent", Math.min(100, (int) Math.round(logs.size() * 100.0 / 7)));
        m.put("avgCompletion", (int) Math.round(logs.stream().mapToInt(TrainingLog::getCompletionRate).average().orElse(0)));
        m.put("avgPainAfter", Math.round(logs.stream()
                .filter(l -> l.getPainAfter() != null).mapToInt(TrainingLog::getPainAfter).average().orElse(0) * 10.0) / 10.0);
        // 主要提交人（判断该窗口反馈来自哪位照护人）
        String mainSubmitter = logs.stream()
                .filter(l -> l.getSubmittedBy() != null)
                .collect(Collectors.groupingBy(l -> l.getSubmittedBy().getName(), Collectors.counting()))
                .entrySet().stream().max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey).orElse("-");
        m.put("mainSubmitter", mainSubmitter);
        return m;
    }

    // ---------- 护士：首周观察与电话指导 ----------

    /** 首周观察期内的新照护人交接（含首周反馈概况） */
    public List<Map<String, Object>> firstWeekList() {
        SecurityUtils.requireRole(Role.NURSE, Role.DOCTOR, Role.THERAPIST, Role.ADMIN);
        List<CaregiverHandover> list = handoverRepository
                .findByStatusAndFirstWeekEndGreaterThanEqualOrderByConfirmedAtDesc(HandoverStatus.CONFIRMED, LocalDate.now());
        List<Map<String, Object>> result = new ArrayList<>();
        for (CaregiverHandover h : list) {
            Map<String, Object> stats = windowStats(h.getPatient().getId(),
                    h.getConfirmedAt().toLocalDate(), h.getFirstWeekEnd());
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("handover", h);
            m.put("firstWeekStats", stats);
            result.add(m);
        }
        return result;
    }

    @Transactional
    public CaregiverHandover addNurseGuidance(Long id, Dtos.NurseGuidanceRequest req) {
        SecurityUtils.requireRole(Role.NURSE, Role.ADMIN);
        CaregiverHandover h = getOne(id);
        User nurse = SecurityUtils.currentUser();
        h.setNurseGuidanceNote(req.note());
        h.setNurseGuidanceBy(nurse.getName());
        h.setNurseGuidanceAt(LocalDateTime.now());
        CaregiverHandover saved = handoverRepository.save(h);
        timelineService.record(h.getPatient(), TimelineEventType.CAREGIVER_GUIDANCE, nurse,
                "护士电话指导新照护人（" + h.getNewCaregiverName() + "）",
                req.note(), "handover", saved.getId());
        return saved;
    }

    private String nullToDash(String v) {
        return v == null || v.isBlank() ? "—" : v;
    }

    private String fmt(double v) {
        return (v > 0 ? "+" : "") + Math.round(v * 10) / 10.0;
    }
}
