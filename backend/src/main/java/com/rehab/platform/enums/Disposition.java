package com.rehab.platform.enums;

/**
 * 医生处置方式（疼痛升级后处方调整方向）
 */
public enum Disposition {
    REST("休息"),
    ICE("冰敷"),
    IMAGING("影像检查"),
    OUTPATIENT("门诊复诊");

    private final String label;

    Disposition(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
