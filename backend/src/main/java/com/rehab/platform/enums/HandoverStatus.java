package com.rehab.platform.enums;

/**
 * 照护人交接状态
 */
public enum HandoverStatus {
    PENDING_CONFIRM("待新照护人确认"),
    CONFIRMED("交接完成");

    private final String label;

    HandoverStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
