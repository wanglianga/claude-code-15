package com.rehab.platform.enums;

/**
 * 护士电话评估结论
 */
public enum NurseDecision {
    OBSERVE_RESUME("继续观察，恢复训练"),
    ESCALATE_DOCTOR("转医生复核");

    private final String label;

    NurseDecision(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
