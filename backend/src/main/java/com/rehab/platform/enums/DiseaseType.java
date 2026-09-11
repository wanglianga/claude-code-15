package com.rehab.platform.enums;

public enum DiseaseType {
    STROKE("脑卒中"),
    FRACTURE_POST_OP("骨折术后"),
    NECK_SHOULDER_PAIN("颈肩腰腿痛"),
    CHILD_DEVELOPMENT_DELAY("儿童发育迟缓");

    private final String label;

    DiseaseType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
