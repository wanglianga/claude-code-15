package com.rehab.platform.enums;

/**
 * 疼痛升级处置状态机：
 * 触发（暂停动作）→ 待家属补充 → 护士电话评估 → 医生复核处置 → 风险解除（动作恢复）
 */
public enum EscalationStatus {
    PENDING_FAMILY_INFO("待家属补充"),
    NURSE_ASSESSING("护士评估中"),
    DOCTOR_REVIEW("待医生复核"),
    DISPOSITION_ACTIVE("医生已处置·暂停中"),
    CLEARED("风险已解除");

    private final String label;

    EscalationStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
