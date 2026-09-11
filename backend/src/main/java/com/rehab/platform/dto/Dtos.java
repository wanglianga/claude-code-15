package com.rehab.platform.dto;

import com.rehab.platform.enums.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 全部请求 DTO（嵌套 record）
 */
public final class Dtos {

    private Dtos() {
    }

    public record LoginRequest(@NotBlank(message = "用户名不能为空") String username,
                               @NotBlank(message = "密码不能为空") String password) {
    }

    public record PatientRequest(
            @NotBlank(message = "姓名不能为空") String name,
            String gender,
            LocalDate birthDate,
            String phone,
            @NotNull(message = "疾病类型不能为空") DiseaseType diseaseType,
            DiseaseStage diseaseStage,
            String diagnosis,
            String caregiverName,
            String caregiverRelation,
            String caregiverPhone,
            String address,
            String insuranceType,
            String insuranceNo,
            LocalDate dischargeDate,
            LocalDate nextReviewDate,
            Long therapistId,
            Long familyUserId) {
    }

    public record StageRequest(@NotNull(message = "阶段不能为空") DiseaseStage stage, String reason) {
    }

    public record AssessmentRequest(
            @NotNull(message = "评估类型不能为空") AssessmentType type,
            String romJson,
            Integer muscleStrength,
            Integer painScore,
            Integer adlScore,
            String gaitVideoPath,
            String notes,
            LocalDate assessmentDate) {
    }

    public record PrescriptionItemRequest(
            @NotBlank(message = "动作名称不能为空") String exerciseName,
            Integer targetSets,
            Integer targetReps,
            Integer frequencyPerDay,
            String contraindication,
            String assistiveDevice,
            Integer painThreshold,
            String familyObservation,
            String demoVideoPath,
            Boolean reimbursable,
            String insuranceCode,
            BigDecimal unitPrice) {
    }

    public record PrescriptionRequest(
            Long assessmentId,
            LocalDate startDate,
            LocalDate endDate,
            LocalDate nextReviewDate,
            Integer painThreshold,
            String notes,
            String adjustReason,
            @NotNull(message = "处方项目不能为空") List<PrescriptionItemRequest> items) {
    }

    public record AdjustRequest(@NotNull(message = "决策类型不能为空") AdjustmentDecision decision,
                                @NotBlank(message = "请填写决策原因") String reason,
                                LocalDate nextReviewDate) {
    }

    public record TrainingLogItem(Long itemId, Boolean done, Integer actualSets, Integer actualReps) {
    }

    public record TrainingLogRequest(
            @NotNull(message = "完成率不能为空") Integer completionRate,
            List<TrainingLogItem> items,
            Integer painBefore,
            Integer painAfter,
            List<String> abnormalPhotos,
            List<String> videoClips,
            String familyNote,
            Boolean companionAvailable,
            Boolean compensationObserved,
            LocalDate logDate) {
    }

    public record FeedbackRequest(@NotBlank(message = "反馈内容不能为空") String feedback) {
    }

    public record FollowupRequest(String method,
                                  @NotBlank(message = "随访内容不能为空") String content,
                                  @NotBlank(message = "随访结果不能为空") String outcome,
                                  String nextAction) {
    }

    public record DoctorOpinionRequest(@NotBlank(message = "处理意见不能为空") String opinion) {
    }

    public record TreatmentRequest(@NotBlank(message = "治疗项目不能为空") String itemName,
                                   String insuranceCode,
                                   Boolean reimbursable,
                                   @NotNull(message = "金额不能为空") BigDecimal amount,
                                   LocalDate treatmentDate,
                                   String note) {
    }

    public record SettlementRequest(@NotNull(message = "开始日期不能为空") LocalDate periodStart,
                                    @NotNull(message = "结束日期不能为空") LocalDate periodEnd) {
    }

    /** 关键动作点标注 */
    public record KeyPointTag(@NotBlank(message = "关键动作点不能为空") String code,
                              String note,
                              String timestamp) {
    }

    /** 治疗师打回视频创建纠错任务 */
    public record CorrectionTaskRequest(Long prescriptionItemId,
                                        @NotNull(message = "请至少标注一个关键动作点") List<KeyPointTag> keyPoints,
                                        @NotBlank(message = "请填写纠错说明") String correctionNote) {
    }

    /** 治疗师复评：是否真正掌握 */
    public record CorrectionReviewRequest(@NotNull(message = "请给出复评结论") Boolean mastered,
                                          String reviewNote) {
    }
}
