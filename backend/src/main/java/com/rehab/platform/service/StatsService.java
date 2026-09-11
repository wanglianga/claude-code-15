package com.rehab.platform.service;

import com.rehab.platform.enums.AlertStatus;
import com.rehab.platform.enums.PrescriptionStatus;
import com.rehab.platform.model.*;
import com.rehab.platform.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 依从性 / 疼痛曲线 / 连续漏练 / 风险等级 计算
 */
@Service
@RequiredArgsConstructor
public class StatsService {

    private final TrainingLogRepository trainingLogRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final AlertRepository alertRepository;

    private static final List<AlertStatus> OPEN_STATUSES =
            List.of(AlertStatus.PENDING, AlertStatus.FOLLOWING, AlertStatus.ESCALATED);

    /** 依从性统计：近 days 天 */
    public Map<String, Object> adherence(Patient patient, int days) {
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(days - 1L);
        return adherenceBetween(patient, start, end);
    }

    /** 依从性统计：指定区间 */
    public Map<String, Object> adherenceBetween(Patient patient, LocalDate start, LocalDate end) {
        Optional<Prescription> active = prescriptionRepository.findByPatientIdAndStatus(patient.getId(), PrescriptionStatus.ACTIVE);
        LocalDate effectiveStart = start;
        if (active.isPresent() && active.get().getStartDate() != null && active.get().getStartDate().isAfter(start)) {
            effectiveStart = active.get().getStartDate();
        }
        LocalDate effectiveEnd = end.isAfter(LocalDate.now()) ? LocalDate.now() : end;
        long plannedDays = effectiveEnd.isBefore(effectiveStart) ? 0
                : java.time.temporal.ChronoUnit.DAYS.between(effectiveStart, effectiveEnd) + 1;

        List<TrainingLog> logs = trainingLogRepository
                .findByPatientIdAndLogDateBetweenOrderByLogDateAsc(patient.getId(), effectiveStart, effectiveEnd);
        long loggedDays = logs.size();
        double avgCompletion = logs.stream().mapToInt(TrainingLog::getCompletionRate).average().orElse(0);
        int adherencePercent = plannedDays == 0 ? 0 : (int) Math.round(loggedDays * 100.0 / plannedDays);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("plannedDays", plannedDays);
        result.put("loggedDays", loggedDays);
        result.put("adherencePercent", Math.min(adherencePercent, 100));
        result.put("avgCompletion", (int) Math.round(avgCompletion));
        result.put("periodStart", effectiveStart);
        result.put("periodEnd", effectiveEnd);
        return result;
    }

    /** 疼痛曲线数据 */
    public List<Map<String, Object>> painCurve(Long patientId, int days) {
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(days - 1L);
        return trainingLogRepository.findByPatientIdAndLogDateBetweenOrderByLogDateAsc(patientId, start, end)
                .stream()
                .map(log -> {
                    Map<String, Object> point = new LinkedHashMap<String, Object>();
                    point.put("date", log.getLogDate().toString());
                    point.put("painBefore", log.getPainBefore());
                    point.put("painAfter", log.getPainAfter());
                    point.put("completionRate", log.getCompletionRate());
                    return point;
                })
                .collect(Collectors.toList());
    }

    /** 连续漏练天数（从今天/昨天往前数没有打卡的天数） */
    public int consecutiveMissedDays(Patient patient) {
        Optional<Prescription> active = prescriptionRepository.findByPatientIdAndStatus(patient.getId(), PrescriptionStatus.ACTIVE);
        if (active.isEmpty()) {
            return 0;
        }
        LocalDate cursor = LocalDate.now();
        // 今天还没到最后打卡时间，从昨天开始算
        cursor = cursor.minusDays(1);
        int missed = 0;
        LocalDate prescriptionStart = active.get().getStartDate() == null
                ? cursor.minusDays(30) : active.get().getStartDate();
        while (!cursor.isBefore(prescriptionStart) && missed < 30) {
            boolean logged = trainingLogRepository.findByPatientIdAndLogDate(patient.getId(), cursor).isPresent();
            if (logged) {
                break;
            }
            missed++;
            cursor = cursor.minusDays(1);
        }
        return missed;
    }

    /** 患者风险等级与原因 */
    public Map<String, Object> riskOf(Patient patient) {
        List<Alert> openAlerts = alertRepository.findByPatientIdAndStatusIn(patient.getId(), OPEN_STATUSES);
        List<String> reasons = new ArrayList<>();
        int score = 0;

        long highAlerts = openAlerts.stream().filter(a -> a.getLevel() == com.rehab.platform.enums.AlertLevel.HIGH).count();
        if (highAlerts > 0) {
            score += 3;
            reasons.add("存在 " + highAlerts + " 条高级别未处理预警");
        }
        long mediumAlerts = openAlerts.stream().filter(a -> a.getLevel() == com.rehab.platform.enums.AlertLevel.MEDIUM).count();
        if (mediumAlerts > 0) {
            score += mediumAlerts;
            reasons.add("存在 " + mediumAlerts + " 条中级别未处理预警");
        }

        int missed = consecutiveMissedDays(patient);
        if (missed >= 3) {
            score += 3;
            reasons.add("已连续漏练 " + missed + " 天");
        } else if (missed >= 2) {
            score += 1;
            reasons.add("已连续漏练 " + missed + " 天");
        }

        Map<String, Object> adherence = adherence(patient, 7);
        int adherencePercent = ((Number) adherence.get("adherencePercent")).intValue();
        long plannedDays = ((Number) adherence.get("plannedDays")).longValue();
        if (plannedDays > 0 && adherencePercent < 60) {
            score += 2;
            reasons.add("近 7 天训练依从性仅 " + adherencePercent + "%");
        }

        List<Map<String, Object>> curve = painCurve(patient.getId(), 7);
        OptionalInt maxPain = curve.stream()
                .map(p -> (Integer) p.get("painAfter"))
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .max();
        if (maxPain.isPresent() && maxPain.getAsInt() >= 7) {
            score += 2;
            reasons.add("近 7 天最高训练后疼痛达 " + maxPain.getAsInt() + " 分");
        }

        String level = score >= 4 ? "HIGH" : (score >= 2 ? "MEDIUM" : "LOW");
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("level", level);
        result.put("score", score);
        result.put("reasons", reasons);
        result.put("missedDays", missed);
        result.put("adherence7d", adherencePercent);
        result.put("openAlerts", openAlerts.size());
        return result;
    }
}
