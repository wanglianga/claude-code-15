package com.rehab.platform.enums;

public enum TimelineEventType {
    PATIENT_CREATED("建立档案"),
    ASSESSMENT_RECORDED("评估记录"),
    PRESCRIPTION_CREATED("开具处方"),
    PRESCRIPTION_ADJUSTED("处方调整决策"),
    TRAINING_LOG_SUBMITTED("居家训练打卡"),
    VIDEO_CORRECTED("视频纠错"),
    ALERT_CREATED("风险预警"),
    ALERT_ASSIGNED("护士接单"),
    NURSE_FOLLOWUP("护士电话随访"),
    ALERT_ESCALATED("转诊医生"),
    DOCTOR_OPINION("医生处理意见"),
    ALERT_RESOLVED("预警解除"),
    OUTPATIENT_TREATMENT("线下治疗记录"),
    SETTLEMENT_CREATED("医保结算"),
    STAGE_CHANGED("疾病阶段调整"),
    REVIEW_PLANNED("复诊计划"),
    CORRECTION_CREATED("视频打回纠错"),
    CORRECTION_CONFIRMED("确认观看纠错"),
    CORRECTION_RECHECK("复评视频已提交"),
    CORRECTION_REVIEWED("纠错复评结论"),
    PAIN_ESCALATION_CREATED("疼痛升级·暂停动作"),
    ESCALATION_FAMILY_REPORT("家属补充症状"),
    ESCALATION_NURSE_ASSESSMENT("护士电话评估"),
    ESCALATION_DOCTOR_DISPOSITION("医生处置结论"),
    ESCALATION_CLEARED("风险解除·恢复训练"),
    CAREGIVER_HANDOVER_CREATED("照护人更换"),
    CAREGIVER_HANDOVER_CONFIRMED("新照护人确认"),
    CAREGIVER_GUIDANCE("护士电话指导");

    private final String label;

    TimelineEventType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
