package com.rehab.platform.enums;

public enum Role {
    ADMIN("系统管理员"),
    THERAPIST("康复治疗师"),
    NURSE("康复护士"),
    DOCTOR("医生"),
    FAMILY("患者/家属");

    private final String label;

    Role(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
