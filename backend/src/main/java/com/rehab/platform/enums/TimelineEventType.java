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
    REVIEW_PLANNED("复诊计划");

    private final String label;

    TimelineEventType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
