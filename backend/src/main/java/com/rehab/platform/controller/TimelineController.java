package com.rehab.platform.controller;

import com.rehab.platform.model.TimelineEvent;
import com.rehab.platform.service.PatientService;
import com.rehab.platform.service.TimelineService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class TimelineController {

    private final TimelineService timelineService;
    private final PatientService patientService;

    @GetMapping("/api/patients/{patientId}/timeline")
    public List<TimelineEvent> timeline(@PathVariable Long patientId) {
        patientService.getAccessible(patientId);
        return timelineService.listByPatient(patientId);
    }
}
