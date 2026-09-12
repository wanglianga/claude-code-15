package com.rehab.platform.service;

import com.rehab.platform.config.BusinessException;
import com.rehab.platform.config.SecurityUtils;
import com.rehab.platform.dto.Dtos;
import com.rehab.platform.enums.Role;
import com.rehab.platform.enums.TimelineEventType;
import com.rehab.platform.model.Patient;
import com.rehab.platform.model.User;
import com.rehab.platform.repository.PatientRepository;
import com.rehab.platform.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
public class PatientService {

    private final PatientRepository patientRepository;
    private final UserRepository userRepository;
    private final TimelineService timelineService;
    private final StatsService statsService;

    private static final AtomicInteger SEQ = new AtomicInteger(100);

    public List<Patient> listForCurrentUser() {
        User user = SecurityUtils.currentUser();
        return switch (user.getRole()) {
            case THERAPIST -> patientRepository.findByTherapistIdOrderByCreatedAtDesc(user.getId());
            case FAMILY -> patientRepository.findByFamilyUserId(user.getId()).map(List::of).orElse(List.of());
            default -> patientRepository.findAllByOrderByCreatedAtDesc();
        };
    }

    public Patient getAccessible(Long id) {
        Patient patient = patientRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "患者不存在"));
        User user = SecurityUtils.currentUser();
        if (user.getRole() == Role.THERAPIST && patient.getTherapist() != null
                && !patient.getTherapist().getId().equals(user.getId())) {
            throw new BusinessException(403, "该患者不属于您负责");
        }
        if (user.getRole() == Role.FAMILY
                && (patient.getFamilyUser() == null || !patient.getFamilyUser().getId().equals(user.getId()))) {
            throw new BusinessException(403, "无权查看该患者");
        }
        return patient;
    }

    @Transactional
    public Patient create(Dtos.PatientRequest req) {
        SecurityUtils.requireRole(Role.THERAPIST, Role.ADMIN);
        Patient p = new Patient();
        apply(p, req);
        p.setPatientNo("P" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy"))
                + String.format("%04d", SEQ.incrementAndGet()));
        Patient saved = patientRepository.save(p);
        timelineService.record(saved, TimelineEventType.PATIENT_CREATED, SecurityUtils.currentUser(),
                "建立患者档案",
                "诊断：" + nullToDash(req.diagnosis()) + "；疾病类型：" + req.diseaseType().getLabel()
                        + "；初始阶段：" + saved.getDiseaseStage().getLabel(),
                "patient", saved.getId());
        return saved;
    }

    @Transactional
    public Patient update(Long id, Dtos.PatientRequest req) {
        SecurityUtils.requireRole(Role.THERAPIST, Role.ADMIN);
        Patient p = getAccessible(id);
        apply(p, req);
        return patientRepository.save(p);
    }

    @Transactional
    public Patient changeStage(Long id, Dtos.StageRequest req) {
        SecurityUtils.requireRole(Role.THERAPIST, Role.ADMIN);
        Patient p = getAccessible(id);
        String old = p.getDiseaseStage().getLabel();
        p.setDiseaseStage(req.stage());
        Patient saved = patientRepository.save(p);
        timelineService.record(saved, TimelineEventType.STAGE_CHANGED, SecurityUtils.currentUser(),
                "疾病阶段调整：" + old + " → " + req.stage().getLabel(),
                "调整原因：" + nullToDash(req.reason()), "patient", saved.getId());
        return saved;
    }

    private void apply(Patient p, Dtos.PatientRequest req) {
        p.setName(req.name());
        p.setGender(req.gender());
        p.setBirthDate(req.birthDate());
        p.setPhone(req.phone());
        p.setDiseaseType(req.diseaseType());
        if (req.diseaseStage() != null) {
            p.setDiseaseStage(req.diseaseStage());
        }
        p.setDiagnosis(req.diagnosis());
        p.setCaregiverName(req.caregiverName());
        p.setCaregiverRelation(req.caregiverRelation());
        p.setCaregiverPhone(req.caregiverPhone());
        p.setAddress(req.address());
        p.setInsuranceType(req.insuranceType());
        p.setInsuranceNo(req.insuranceNo());
        p.setDischargeDate(req.dischargeDate());
        p.setNextReviewDate(req.nextReviewDate());
        if (req.therapistId() != null) {
            User therapist = userRepository.findById(req.therapistId())
                    .orElseThrow(() -> new BusinessException("治疗师不存在"));
            p.setTherapist(therapist);
        } else if (p.getTherapist() == null) {
            p.setTherapist(SecurityUtils.currentUser());
        }
        if (req.familyUserId() != null) {
            p.setFamilyUser(resolveBindableFamilyUser(req.familyUserId(), p.getId()));
        }
    }

    /**
     * 校验并返回可绑定的家属账号：
     * 账号必须存在、为家属角色，且未绑定到其他患者（同一账号重复绑定会导致家属端查询异常）。
     *
     * @param excludePatientId 允许保持绑定的患者（更新自身/交接至本患者时传入），其余情况传 null
     */
    public User resolveBindableFamilyUser(Long familyUserId, Long excludePatientId) {
        User u = userRepository.findById(familyUserId)
                .orElseThrow(() -> new BusinessException(404, "家属账号不存在"));
        if (u.getRole() != Role.FAMILY) {
            throw new BusinessException("账号「" + u.getUsername() + "」不是家属角色，不能绑定为患者家属");
        }
        patientRepository.findByFamilyUserId(familyUserId).ifPresent(existing -> {
            if (excludePatientId == null || !existing.getId().equals(excludePatientId)) {
                throw new BusinessException("账号「" + u.getName() + "（" + u.getUsername()
                        + "）」已绑定患者「" + existing.getName() + "」，不能重复绑定到其他患者");
            }
        });
        return u;
    }

    public Map<String, Object> riskOf(Patient patient) {
        return statsService.riskOf(patient);
    }

    private String nullToDash(String s) {
        return s == null || s.isBlank() ? "—" : s;
    }
}
