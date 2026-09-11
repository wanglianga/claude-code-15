package com.rehab.platform.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rehab.platform.config.BusinessException;
import com.rehab.platform.config.SecurityUtils;
import com.rehab.platform.dto.Dtos;
import com.rehab.platform.enums.AssessmentType;
import com.rehab.platform.enums.PrescriptionStatus;
import com.rehab.platform.enums.Role;
import com.rehab.platform.enums.TimelineEventType;
import com.rehab.platform.model.*;
import com.rehab.platform.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@RequiredArgsConstructor
public class InsuranceService {

    private final OutpatientTreatmentRepository treatmentRepository;
    private final InsuranceSettlementRepository settlementRepository;
    private final AssessmentRepository assessmentRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final TrainingLogRepository trainingLogRepository;
    private final PatientService patientService;
    private final TimelineService timelineService;
    private final ObjectMapper objectMapper;

    /** 复诊评估单次费用（康复评定，医保编码 340200001） */
    private static final BigDecimal ASSESSMENT_FEE = new BigDecimal("30");
    private static final String ASSESSMENT_CODE = "340200001";

    /** 医保类型 → 报销比例（县域常见政策，可按需调整） */
    private static final Map<String, Double> INSURANCE_RATIOS = Map.of(
            "职工医保", 0.80,
            "城乡居民医保", 0.70,
            "新农合", 0.60,
            "自费", 0.0);

    // ---------- 线下治疗记录 ----------

    public List<OutpatientTreatment> treatments(Long patientId) {
        patientService.getAccessible(patientId);
        return treatmentRepository.findByPatientIdOrderByTreatmentDateDesc(patientId);
    }

    @Transactional
    public OutpatientTreatment addTreatment(Long patientId, Dtos.TreatmentRequest req) {
        SecurityUtils.requireRole(Role.THERAPIST, Role.ADMIN);
        Patient patient = patientService.getAccessible(patientId);
        OutpatientTreatment t = new OutpatientTreatment();
        t.setPatient(patient);
        t.setTherapist(SecurityUtils.currentUser());
        t.setItemName(req.itemName());
        t.setInsuranceCode(req.insuranceCode());
        t.setReimbursable(req.reimbursable() == null || req.reimbursable());
        t.setAmount(req.amount());
        t.setTreatmentDate(req.treatmentDate() == null ? LocalDate.now() : req.treatmentDate());
        t.setNote(req.note());
        OutpatientTreatment saved = treatmentRepository.save(t);
        timelineService.record(patient, TimelineEventType.OUTPATIENT_TREATMENT, SecurityUtils.currentUser(),
                "线下治疗：" + req.itemName() + "（¥" + req.amount() + "）",
                req.note(), "treatment", saved.getId());
        return saved;
    }

    // ---------- 医保结算 ----------

    public List<InsuranceSettlement> settlements(Long patientId) {
        patientService.getAccessible(patientId);
        return settlementRepository.findByPatientIdOrderByCreatedAtDesc(patientId);
    }

    /**
     * 生成结算单：居家训练（按处方执行率折算）+ 复诊评估 + 线下治疗记录
     */
    @Transactional
    public InsuranceSettlement createSettlement(Long patientId, Dtos.SettlementRequest req) {
        SecurityUtils.requireRole(Role.THERAPIST, Role.ADMIN);
        Patient patient = patientService.getAccessible(patientId);
        if (req.periodEnd().isBefore(req.periodStart())) {
            throw new BusinessException("结束日期不能早于开始日期");
        }

        List<Map<String, Object>> details = new ArrayList<>();
        BigDecimal homeTraining = BigDecimal.ZERO;

        // 1. 居家训练：周期内执行中的处方，可报销项目 × 每日次数 × 打卡天数 × 平均完成率
        List<Prescription> prescriptions = prescriptionRepository.findByPatientIdOrderByCreatedAtDesc(patientId);
        List<TrainingLog> logs = trainingLogRepository.findByPatientIdAndLogDateBetweenOrderByLogDateAsc(
                patientId, req.periodStart(), req.periodEnd());
        long loggedDays = logs.size();
        double avgCompletion = logs.stream().mapToInt(TrainingLog::getCompletionRate).average().orElse(0) / 100.0;
        long periodDays = ChronoUnit.DAYS.between(req.periodStart(), req.periodEnd()) + 1;

        for (Prescription p : prescriptions) {
            if (p.getStatus() == PrescriptionStatus.SUPERSEDED || p.getStatus() == PrescriptionStatus.ACTIVE) {
                for (PrescriptionItem item : p.getItems()) {
                    if (Boolean.TRUE.equals(item.getReimbursable()) && item.getUnitPrice() != null
                            && item.getUnitPrice().compareTo(BigDecimal.ZERO) > 0) {
                        int freq = item.getFrequencyPerDay() == null ? 1 : item.getFrequencyPerDay();
                        BigDecimal executed = BigDecimal.valueOf(freq)
                                .multiply(BigDecimal.valueOf(loggedDays))
                                .multiply(BigDecimal.valueOf(avgCompletion));
                        BigDecimal amount = item.getUnitPrice().multiply(executed)
                                .setScale(2, RoundingMode.HALF_UP);
                        if (amount.compareTo(BigDecimal.ZERO) > 0) {
                            homeTraining = homeTraining.add(amount);
                            details.add(detail("居家训练-" + item.getExerciseName(), item.getInsuranceCode(),
                                    "计划 " + (freq * periodDays) + " 次，按依从性折算 "
                                            + executed.setScale(1, RoundingMode.HALF_UP) + " 次 × ¥" + item.getUnitPrice(),
                                    amount, true));
                        }
                    }
                }
            }
        }

        // 2. 复诊评估费用
        List<Assessment> followups = assessmentRepository
                .findByPatientIdAndAssessmentDateBetween(patientId, req.periodStart(), req.periodEnd())
                .stream().filter(a -> a.getType() == AssessmentType.FOLLOWUP).toList();
        BigDecimal assessmentAmount = ASSESSMENT_FEE.multiply(BigDecimal.valueOf(followups.size()));
        if (!followups.isEmpty()) {
            details.add(detail("复诊评估（康复评定）× " + followups.size(), ASSESSMENT_CODE,
                    "周期内完成 " + followups.size() + " 次复诊评估 × ¥" + ASSESSMENT_FEE, assessmentAmount, true));
        }

        // 3. 线下治疗记录
        List<OutpatientTreatment> treatments = treatmentRepository
                .findByPatientIdAndTreatmentDateBetween(patientId, req.periodStart(), req.periodEnd());
        BigDecimal outpatient = BigDecimal.ZERO;
        for (OutpatientTreatment t : treatments) {
            outpatient = outpatient.add(t.getAmount());
            details.add(detail("线下治疗-" + t.getItemName(), t.getInsuranceCode(),
                    t.getTreatmentDate() + " 门诊治疗", t.getAmount(), Boolean.TRUE.equals(t.getReimbursable())));
        }

        BigDecimal total = homeTraining.add(assessmentAmount).add(outpatient);
        double ratio = INSURANCE_RATIOS.getOrDefault(
                patient.getInsuranceType() == null ? "自费" : patient.getInsuranceType(), 0.0);
        // 仅可报销项目参与报销：居家训练 + 复诊评估 + 标记可报销的线下治疗
        BigDecimal outpatientReimbursable = treatments.stream()
                .filter(t -> Boolean.TRUE.equals(t.getReimbursable()))
                .map(OutpatientTreatment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal base = homeTraining.add(assessmentAmount).add(outpatientReimbursable);
        BigDecimal reimbursable = base.multiply(BigDecimal.valueOf(ratio)).setScale(2, RoundingMode.HALF_UP);
        BigDecimal selfPay = total.subtract(reimbursable);

        InsuranceSettlement settlement = new InsuranceSettlement();
        settlement.setPatient(patient);
        settlement.setPeriodStart(req.periodStart());
        settlement.setPeriodEnd(req.periodEnd());
        long plannedDays = ChronoUnit.DAYS.between(req.periodStart(),
                req.periodEnd().isAfter(LocalDate.now()) ? LocalDate.now() : req.periodEnd()) + 1;
        settlement.setAdherencePercent(plannedDays <= 0 ? 0 : (int) Math.min(100, loggedDays * 100 / plannedDays));
        settlement.setHomeTrainingAmount(homeTraining);
        settlement.setAssessmentAmount(assessmentAmount);
        settlement.setOutpatientAmount(outpatient);
        settlement.setTotalAmount(total);
        settlement.setReimbursableAmount(reimbursable);
        settlement.setSelfPayAmount(selfPay);
        settlement.setDetailJson(toJson(details));
        settlement.setCreatedBy(SecurityUtils.currentUser().getName());
        InsuranceSettlement saved = settlementRepository.save(settlement);

        timelineService.record(patient, TimelineEventType.SETTLEMENT_CREATED, SecurityUtils.currentUser(),
                "生成医保结算单（" + req.periodStart() + " ~ " + req.periodEnd() + "）",
                "居家训练 ¥" + homeTraining + " + 复诊评估 ¥" + assessmentAmount + " + 线下治疗 ¥" + outpatient
                        + " = 合计 ¥" + total + "；按「" + patient.getInsuranceType() + "」报销比例 "
                        + (int) (ratio * 100) + "%，报销 ¥" + reimbursable + "，自付 ¥" + selfPay,
                "settlement", saved.getId());
        return saved;
    }

    @Transactional
    public InsuranceSettlement confirm(Long settlementId) {
        SecurityUtils.requireRole(Role.THERAPIST, Role.ADMIN);
        InsuranceSettlement s = settlementRepository.findById(settlementId)
                .orElseThrow(() -> new BusinessException(404, "结算单不存在"));
        s.setStatus(com.rehab.platform.enums.SettlementStatus.CONFIRMED);
        return settlementRepository.save(s);
    }

    private Map<String, Object> detail(String name, String code, String note, BigDecimal amount, boolean reimbursable) {
        Map<String, Object> d = new LinkedHashMap<>();
        d.put("name", name);
        d.put("code", code);
        d.put("note", note);
        d.put("amount", amount);
        d.put("reimbursable", reimbursable);
        return d;
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }
}
