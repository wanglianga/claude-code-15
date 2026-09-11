package com.rehab.platform.enums;

public enum AssessmentType {
    INITIAL("初次评估"),
    FOLLOWUP("复诊评估");

    private final String label;

    AssessmentType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
