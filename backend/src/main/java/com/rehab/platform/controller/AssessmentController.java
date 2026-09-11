package com.rehab.platform.controller;

import com.rehab.platform.dto.Dtos;
import com.rehab.platform.model.Assessment;
import com.rehab.platform.service.AssessmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/patients/{patientId}/assessments")
@RequiredArgsConstructor
public class AssessmentController {

    private final AssessmentService assessmentService;

    @GetMapping
    public List<Assessment> list(@PathVariable Long patientId) {
        return assessmentService.listByPatient(patientId);
    }

    @PostMapping
    public Assessment create(@PathVariable Long patientId, @Valid @RequestBody Dtos.AssessmentRequest req) {
        return assessmentService.create(patientId, req);
    }
}
