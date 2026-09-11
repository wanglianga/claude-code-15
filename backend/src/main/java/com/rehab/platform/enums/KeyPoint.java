package com.rehab.platform.enums;

/** 视频纠错关键动作点字典 */
public enum KeyPoint {
    KNEE_ANGLE("膝关节角度"),
    SCAPULAR_COMPENSATION("肩胛代偿"),
    GAIT_DRAGGING("步态拖曳"),
    LUMBAR_HYPEREXTENSION("腰部过伸"),
    SHOULDER_SUBLUXATION("肩关节半脱位"),
    HIP_DROP("骨盆下沉"),
    TRUNK_LEAN("躯干倾斜"),
    OTHER("其他");

    private final String label;

    KeyPoint(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
