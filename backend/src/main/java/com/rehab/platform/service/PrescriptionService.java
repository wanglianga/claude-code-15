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

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PrescriptionService {

    private final PrescriptionRepository prescriptionRepository;
    private final AssessmentRepository assessmentRepository;
    private final PrescriptionAdjustmentRepository adjustmentRepository;
    private final PatientService patientService;
    private final TimelineService timelineService;
    private final PatientRepository patientRepository;

    public List<Prescription> listByPatient(Long patientId) {
        patientService.getAccessible(patientId);
        return prescriptionRepository.findByPatientIdOrderByCreatedAtDesc(patientId);
    }

    public Prescription getActive(Long patientId) {
        patientService.getAccessible(patientId);
        return prescriptionRepository.findByPatientIdAndStatus(patientId, PrescriptionStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(404, "当前没有执行中的处方"));
    }

    public Prescription getOne(Long id) {
        Prescription p = prescriptionRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "处方不存在"));
        patientService.getAccessible(p.getPatient().getId());
        return p;
    }

    /** 开具新处方（自动生成阶段号，旧处方标记为被替代） */
    @Transactional
    public Prescription create(Long patientId, Dtos.PrescriptionRequest req) {
        SecurityUtils.requireRole(Role.THERAPIST, Role.ADMIN);
        Patient patient = patientService.getAccessible(patientId);

        Prescription prescription = new Prescription();
        prescription.setPatient(patient);
        prescription.setTherapist(SecurityUtils.currentUser());
        if (req.assessmentId() != null) {
            assessmentRepository.findById(req.assessmentId()).ifPresent(prescription::setAssessment);
        }
        int phase = prescriptionRepository.findByPatientIdOrderByCreatedAtDesc(patientId).stream()
                .mapToInt(Prescription::getPhase).max().orElse(0) + 1;
        prescription.setPhase(phase);
        prescription.setStartDate(req.startDate() == null ? LocalDate.now() : req.startDate());
        prescription.setEndDate(req.endDate());
        prescription.setNextReviewDate(req.nextReviewDate());
        prescription.setPainThreshold(req.painThreshold() == null ? 6 : req.painThreshold());
        prescription.setNotes(req.notes());
        prescription.setAdjustReason(req.adjustReason());

        if (req.items() == null || req.items().isEmpty()) {
            throw new BusinessException("处方至少需要一个训练项目");
        }
        for (Dtos.PrescriptionItemRequest itemReq : req.items()) {
            PrescriptionItem item = new PrescriptionItem();
            item.setExerciseName(itemReq.exerciseName());
            item.setTargetSets(itemReq.targetSets());
            item.setTargetReps(itemReq.targetReps());
            item.setFrequencyPerDay(itemReq.frequencyPerDay() == null ? 1 : itemReq.frequencyPerDay());
            item.setContraindication(itemReq.contraindication());
            item.setAssistiveDevice(itemReq.assistiveDevice());
            item.setPainThreshold(itemReq.painThreshold());
            item.setFamilyObservation(itemReq.familyObservation());
            item.setDemoVideoPath(itemReq.demoVideoPath());
            item.setReimbursable(Boolean.TRUE.equals(itemReq.reimbursable()));
            item.setInsuranceCode(itemReq.insuranceCode());
            item.setUnitPrice(itemReq.unitPrice() == null ? java.math.BigDecimal.ZERO : itemReq.unitPrice());
            prescription.addItem(item);
        }

        // 旧处方标记为被替代
        prescriptionRepository.findByPatientIdAndStatus(patientId, PrescriptionStatus.ACTIVE)
                .ifPresent(old -> {
                    old.setStatus(PrescriptionStatus.SUPERSEDED);
                    prescriptionRepository.save(old);
                });

        Prescription saved = prescriptionRepository.save(prescription);

        // 同步患者复诊计划
        if (req.nextReviewDate() != null) {
            patient.setNextReviewDate(req.nextReviewDate());
            patientRepository.save(patient);
        }

        timelineService.record(patient, TimelineEventType.PRESCRIPTION_CREATED, SecurityUtils.currentUser(),
                "开具第 " + phase + " 阶段居家训练处方（" + saved.getItems().size() + " 个训练项目）",
                "生成原因：" + (req.adjustReason() == null || req.adjustReason().isBlank() ? "按阶段计划开具" : req.adjustReason())
                        + (req.nextReviewDate() != null ? "；计划复诊日期：" + req.nextReviewDate() : ""),
                "prescription", saved.getId());
        return saved;
    }

    /**
     * 处方调整决策：维持 / 降低强度 / 追加线下复诊 / 提醒医生介入
     */
    @Transactional
    public PrescriptionAdjustment adjust(Long prescriptionId, Dtos.AdjustRequest req) {
        SecurityUtils.requireRole(Role.THERAPIST, Role.ADMIN);
        Prescription prescription = getOne(prescriptionId);
        Patient patient = prescription.getPatient();

        PrescriptionAdjustment adjustment = new PrescriptionAdjustment();
        adjustment.setPatient(patient);
        adjustment.setPrescription(prescription);
        adjustment.setTherapist(SecurityUtils.currentUser());
        adjustment.setDecision(req.decision());
        adjustment.setReason(req.reason());
        PrescriptionAdjustment saved = adjustmentRepository.save(adjustment);

        switch (req.decision()) {
            case MAINTAIN -> timelineService.record(patient, TimelineEventType.PRESCRIPTION_ADJUSTED,
                    SecurityUtils.currentUser(), "处方决策：维持原处方",
                    "依据：" + req.reason(), "adjustment", saved.getId());
            case REDUCE_INTENSITY -> timelineService.record(patient, TimelineEventType.PRESCRIPTION_ADJUSTED,
                    SecurityUtils.currentUser(), "处方决策：降低训练强度（请开具新阶段处方生效）",
                    "依据：" + req.reason(), "adjustment", saved.getId());
            case ADD_OFFLINE_VISIT -> {
                if (req.nextReviewDate() != null) {
                    patient.setNextReviewDate(req.nextReviewDate());
                    patientRepository.save(patient);
                }
                timelineService.record(patient, TimelineEventType.REVIEW_PLANNED,
                        SecurityUtils.currentUser(),
                        "追加线下复诊" + (req.nextReviewDate() != null ? "，定于 " + req.nextReviewDate() : ""),
                        "依据：" + req.reason(), "adjustment", saved.getId());
            }
            case DOCTOR_REFERRAL -> timelineService.record(patient, TimelineEventType.PRESCRIPTION_ADJUSTED,
                    SecurityUtils.currentUser(), "处方决策：提醒医生介入",
                    "依据：" + req.reason(), "adjustment", saved.getId());
        }
        return saved;
    }

    public List<PrescriptionAdjustment> adjustments(Long patientId) {
        patientService.getAccessible(patientId);
        return adjustmentRepository.findByPatientIdOrderByCreatedAtDesc(patientId);
    }
}
