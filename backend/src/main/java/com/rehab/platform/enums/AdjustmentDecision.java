package com.rehab.platform.enums;

public enum AdjustmentDecision {
    MAINTAIN("维持原处方"),
    REDUCE_INTENSITY("降低训练强度"),
    ADD_OFFLINE_VISIT("追加线下复诊"),
    DOCTOR_REFERRAL("提醒医生介入");

    private final String label;

    AdjustmentDecision(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
