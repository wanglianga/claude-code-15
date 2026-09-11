package com.rehab.platform.controller;

import com.rehab.platform.dto.Dtos;
import com.rehab.platform.model.Prescription;
import com.rehab.platform.model.PrescriptionAdjustment;
import com.rehab.platform.service.PrescriptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class PrescriptionController {

    private final PrescriptionService prescriptionService;

    @GetMapping("/api/patients/{patientId}/prescriptions")
    public List<Prescription> list(@PathVariable Long patientId) {
        return prescriptionService.listByPatient(patientId);
    }

    @PostMapping("/api/patients/{patientId}/prescriptions")
    public Prescription create(@PathVariable Long patientId, @Valid @RequestBody Dtos.PrescriptionRequest req) {
        return prescriptionService.create(patientId, req);
    }

    @GetMapping("/api/patients/{patientId}/prescriptions/active")
    public Prescription active(@PathVariable Long patientId) {
        return prescriptionService.getActive(patientId);
    }

    @GetMapping("/api/prescriptions/{id}")
    public Prescription one(@PathVariable Long id) {
        return prescriptionService.getOne(id);
    }

    @PostMapping("/api/prescriptions/{id}/adjust")
    public PrescriptionAdjustment adjust(@PathVariable Long id, @Valid @RequestBody Dtos.AdjustRequest req) {
        return prescriptionService.adjust(id, req);
    }

    @GetMapping("/api/patients/{patientId}/adjustments")
    public List<PrescriptionAdjustment> adjustments(@PathVariable Long patientId) {
        return prescriptionService.adjustments(patientId);
    }
}
