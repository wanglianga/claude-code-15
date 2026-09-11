package com.rehab.platform.enums;

public enum DiseaseStage {
    NEWLY_DISCHARGED("刚出院"),
    STABLE_TRAINING("稳定训练"),
    RECURRENCE_WARNING("复发预警");

    private final String label;

    DiseaseStage(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
