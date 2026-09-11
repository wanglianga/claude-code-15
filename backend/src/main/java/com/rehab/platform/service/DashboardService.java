package com.rehab.platform.service;

import com.rehab.platform.config.SecurityUtils;
import com.rehab.platform.enums.AlertStatus;
import com.rehab.platform.enums.DiseaseStage;
import com.rehab.platform.enums.Role;
import com.rehab.platform.model.Alert;
import com.rehab.platform.model.Patient;
import com.rehab.platform.repository.AlertRepository;
import com.rehab.platform.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final PatientRepository patientRepository;
    private final AlertRepository alertRepository;
    private final StatsService statsService;

    /** 工作台汇总数据（按角色） */
    public Map<String, Object> summary() {
        var user = SecurityUtils.currentUser();
        Map<String, Object> result = new LinkedHashMap<>();
        List<Patient> patients = switch (user.getRole()) {
            case THERAPIST -> patientRepository.findByTherapistIdOrderByCreatedAtDesc(user.getId());
            case FAMILY -> patientRepository.findByFamilyUserId(user.getId()).map(List::of).orElse(List.of());
            default -> patientRepository.findAll();
        };

        result.put("patientCount", patients.size());
        result.put("stageCounts", patients.stream().collect(Collectors.groupingBy(
                p -> p.getDiseaseStage().name(), Collectors.counting())));

        LocalDate today = LocalDate.now();
        result.put("upcomingReviews", patients.stream()
                .filter(p -> p.getNextReviewDate() != null
                        && !p.getNextReviewDate().isBefore(today)
                        && !p.getNextReviewDate().isAfter(today.plusDays(7)))
                .map(p -> Map.of("id", p.getId(), "name", p.getName(),
                        "nextReviewDate", p.getNextReviewDate().toString(),
                        "diseaseType", p.getDiseaseType().getLabel()))
                .sorted(Comparator.comparing(m -> (String) m.get("nextReviewDate")))
                .toList());

        List<Alert> openAlerts = alertRepository.findByStatusInOrderByCreatedAtDesc(
                List.of(AlertStatus.PENDING, AlertStatus.FOLLOWING, AlertStatus.ESCALATED));
        result.put("openAlertCount", openAlerts.size());
        result.put("pendingAlertCount", openAlerts.stream().filter(a -> a.getStatus() == AlertStatus.PENDING).count());
        result.put("escalatedCount", openAlerts.stream().filter(a -> a.getStatus() == AlertStatus.ESCALATED).count());

        // 患者卡片（含风险）
        List<Map<String, Object>> cards = patients.stream().map(p -> {
            Map<String, Object> risk = statsService.riskOf(p);
            Map<String, Object> card = new LinkedHashMap<String, Object>();
            card.put("id", p.getId());
            card.put("name", p.getName());
            card.put("patientNo", p.getPatientNo());
            card.put("diseaseType", p.getDiseaseType().getLabel());
            card.put("diseaseStage", p.getDiseaseStage().name());
            card.put("diseaseStageLabel", p.getDiseaseStage().getLabel());
            card.put("nextReviewDate", p.getNextReviewDate() == null ? null : p.getNextReviewDate().toString());
            card.put("dischargeDate", p.getDischargeDate() == null ? null : p.getDischargeDate().toString());
            card.put("risk", risk);
            return card;
        }).toList();
        result.put("patients", cards);
        return result;
    }

    /** 按疾病阶段分组的风险看板 */
    public Map<String, Object> riskBoard() {
        SecurityUtils.requireRole(Role.THERAPIST, Role.NURSE, Role.DOCTOR, Role.ADMIN);
        Map<String, Object> board = new LinkedHashMap<>();
        for (DiseaseStage stage : DiseaseStage.values()) {
            List<Map<String, Object>> list = patientRepository.findByDiseaseStage(stage).stream().map(p -> {
                Map<String, Object> risk = statsService.riskOf(p);
                Map<String, Object> card = new LinkedHashMap<String, Object>();
                card.put("id", p.getId());
                card.put("name", p.getName());
                card.put("patientNo", p.getPatientNo());
                card.put("diseaseType", p.getDiseaseType().getLabel());
                card.put("therapist", p.getTherapist() == null ? null : p.getTherapist().getName());
                card.put("nextReviewDate", p.getNextReviewDate() == null ? null : p.getNextReviewDate().toString());
                card.put("dischargeDate", p.getDischargeDate() == null ? null : p.getDischargeDate().toString());
                card.put("risk", risk);
                return card;
            }).toList();
            board.put(stage.name(), list);
        }
        return board;
    }
}
