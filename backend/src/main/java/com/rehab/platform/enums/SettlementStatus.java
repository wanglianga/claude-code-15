package com.rehab.platform.enums;

public enum SettlementStatus {
    DRAFT("草稿"),
    CONFIRMED("已确认");

    private final String label;

    SettlementStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
