package com.rehab.platform.enums;

public enum AlertType {
    MISSED_TRAINING("连续漏练"),
    PAIN_RISE("疼痛升高"),
    COMPENSATION("动作代偿明显"),
    NO_COMPANION("家属无法陪练"),
    DOCTOR_REFERRAL("转诊医生处理");

    private final String label;

    AlertType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
