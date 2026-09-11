package com.rehab.platform.controller;

import com.rehab.platform.dto.Dtos;
import com.rehab.platform.model.CaregiverHandover;
import com.rehab.platform.service.CaregiverHandoverService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class HandoverController {

    private final CaregiverHandoverService handoverService;

    /** 患者的交接记录 */
    @GetMapping("/api/patients/{patientId}/handovers")
    public List<CaregiverHandover> listByPatient(@PathVariable Long patientId) {
        return handoverService.listByPatient(patientId);
    }

    /** 发起照护人更换（治疗师） */
    @PostMapping("/api/patients/{patientId}/handovers")
    public CaregiverHandover create(@PathVariable Long patientId, @Valid @RequestBody Dtos.HandoverRequest req) {
        return handoverService.create(patientId, req);
    }

    /** 新照护人三项确认（动作注意事项/禁忌风险/器具使用） */
    @PostMapping("/api/handovers/{id}/confirm")
    public CaregiverHandover confirm(@PathVariable Long id, @Valid @RequestBody Dtos.HandoverConfirmRequest req) {
        return handoverService.confirm(id, req);
    }

    /** 治疗师对比：更换前后依从性与疼痛变化 */
    @GetMapping("/api/handovers/{id}/comparison")
    public Map<String, Object> comparison(@PathVariable Long id) {
        return handoverService.comparison(id);
    }

    /** 护士：首周观察期内的新照护人交接 */
    @GetMapping("/api/handovers/first-week")
    public List<Map<String, Object>> firstWeek() {
        return handoverService.firstWeekList();
    }

    /** 护士记录电话指导 */
    @PostMapping("/api/handovers/{id}/nurse-guidance")
    public CaregiverHandover nurseGuidance(@PathVariable Long id, @Valid @RequestBody Dtos.NurseGuidanceRequest req) {
        return handoverService.addNurseGuidance(id, req);
    }
}
