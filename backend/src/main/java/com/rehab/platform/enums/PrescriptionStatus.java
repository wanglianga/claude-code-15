package com.rehab.platform.enums;

public enum PrescriptionStatus {
    ACTIVE("执行中"),
    SUPERSEDED("已被新处方替代"),
    COMPLETED("已完成");

    private final String label;

    PrescriptionStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
