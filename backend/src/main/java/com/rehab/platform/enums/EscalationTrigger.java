package com.rehab.platform.enums;

/**
 * 疼痛升级触发原因
 */
public enum EscalationTrigger {
    PAIN_OVER_THRESHOLD("训练后疼痛超阈值"),
    SWELLING_NUMBNESS("出现肿胀/麻木"),
    NIGHT_PAIN_WORSE("夜间疼痛加重");

    private final String label;

    EscalationTrigger(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
