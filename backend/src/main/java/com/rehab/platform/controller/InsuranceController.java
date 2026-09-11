package com.rehab.platform.controller;

import com.rehab.platform.dto.Dtos;
import com.rehab.platform.model.InsuranceSettlement;
import com.rehab.platform.model.OutpatientTreatment;
import com.rehab.platform.service.InsuranceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class InsuranceController {

    private final InsuranceService insuranceService;

    @GetMapping("/api/patients/{patientId}/treatments")
    public List<OutpatientTreatment> treatments(@PathVariable Long patientId) {
        return insuranceService.treatments(patientId);
    }

    @PostMapping("/api/patients/{patientId}/treatments")
    public OutpatientTreatment addTreatment(@PathVariable Long patientId, @Valid @RequestBody Dtos.TreatmentRequest req) {
        return insuranceService.addTreatment(patientId, req);
    }

    @GetMapping("/api/patients/{patientId}/settlements")
    public List<InsuranceSettlement> settlements(@PathVariable Long patientId) {
        return insuranceService.settlements(patientId);
    }

    @PostMapping("/api/patients/{patientId}/settlements")
    public InsuranceSettlement createSettlement(@PathVariable Long patientId, @Valid @RequestBody Dtos.SettlementRequest req) {
        return insuranceService.createSettlement(patientId, req);
    }

    @PostMapping("/api/settlements/{id}/confirm")
    public InsuranceSettlement confirm(@PathVariable Long id) {
        return insuranceService.confirm(id);
    }
}
