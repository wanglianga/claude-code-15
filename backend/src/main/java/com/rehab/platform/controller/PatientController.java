package com.rehab.platform.controller;

import com.rehab.platform.dto.Dtos;
import com.rehab.platform.model.Patient;
import com.rehab.platform.service.PatientService;
import com.rehab.platform.service.StatsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/patients")
@RequiredArgsConstructor
public class PatientController {

    private final PatientService patientService;
    private final StatsService statsService;

    @GetMapping
    public List<Patient> list() {
        return patientService.listForCurrentUser();
    }

    @PostMapping
    public Patient create(@Valid @RequestBody Dtos.PatientRequest req) {
        return patientService.create(req);
    }

    @GetMapping("/{id}")
    public Patient detail(@PathVariable Long id) {
        return patientService.getAccessible(id);
    }

    @PutMapping("/{id}")
    public Patient update(@PathVariable Long id, @Valid @RequestBody Dtos.PatientRequest req) {
        return patientService.update(id, req);
    }

    @PutMapping("/{id}/stage")
    public Patient changeStage(@PathVariable Long id, @Valid @RequestBody Dtos.StageRequest req) {
        return patientService.changeStage(id, req);
    }

    /** 患者风险等级 */
    @GetMapping("/{id}/risk")
    public Map<String, Object> risk(@PathVariable Long id) {
        return statsService.riskOf(patientService.getAccessible(id));
    }

    /** 依从性统计 */
    @GetMapping("/{id}/adherence")
    public Map<String, Object> adherence(@PathVariable Long id, @RequestParam(defaultValue = "14") int days) {
        return statsService.adherence(patientService.getAccessible(id), days);
    }

    /** 疼痛曲线 */
    @GetMapping("/{id}/pain-curve")
    public List<Map<String, Object>> painCurve(@PathVariable Long id, @RequestParam(defaultValue = "30") int days) {
        return statsService.painCurve(patientService.getAccessible(id).getId(), days);
    }
}
