package com.rehab.platform.enums;

public enum CorrectionStatus {
    PENDING_CONFIRM("待患者确认"),
    CONFIRMED("已确认，待下次视频"),
    RECHECK("新视频待复评"),
    MASTERED("已掌握"),
    NOT_MASTERED("未掌握，继续纠正");

    private final String label;

    CorrectionStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
