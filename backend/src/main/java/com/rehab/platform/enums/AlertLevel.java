package com.rehab.platform.enums;

public enum AlertLevel {
    LOW("低"),
    MEDIUM("中"),
    HIGH("高");

    private final String label;

    AlertLevel(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
