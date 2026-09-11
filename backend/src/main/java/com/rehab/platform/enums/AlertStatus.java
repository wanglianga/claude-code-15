package com.rehab.platform.enums;

public enum AlertStatus {
    PENDING("待随访"),
    FOLLOWING("随访中"),
    ESCALATED("已转医生"),
    RESOLVED("已解决");

    private final String label;

    AlertStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
