package com.rehab.platform.controller;

import com.rehab.platform.dto.Dtos;
import com.rehab.platform.enums.EscalationStatus;
import com.rehab.platform.model.PainEscalation;
import com.rehab.platform.service.PainEscalationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class PainEscalationController {

    private final PainEscalationService escalationService;

    /** 医护端：疼痛升级处置单列表（可按状态筛选） */
    @GetMapping("/api/escalations")
    public List<PainEscalation> list(@RequestParam(required = false) EscalationStatus status) {
        return escalationService.listAll(status);
    }

    /** 患者的升级处置记录 */
    @GetMapping("/api/patients/{patientId}/escalations")
    public List<PainEscalation> listByPatient(@PathVariable Long patientId) {
        return escalationService.listByPatient(patientId);
    }

    /** 家属补充症状/用药/是否摔倒（护士可代登记） */
    @PostMapping("/api/escalations/{id}/family-report")
    public PainEscalation familyReport(@PathVariable Long id, @Valid @RequestBody Dtos.FamilyReportRequest req) {
        return escalationService.familyReport(id, req);
    }

    /** 护士电话评估（继续观察恢复 / 转医生复核） */
    @PostMapping("/api/escalations/{id}/nurse-assessment")
    public PainEscalation nurseAssessment(@PathVariable Long id, @Valid @RequestBody Dtos.NurseAssessmentRequest req) {
        return escalationService.nurseAssessment(id, req);
    }

    /** 医生复核处置（休息/冰敷/影像检查/门诊复诊） */
    @PostMapping("/api/escalations/{id}/doctor-disposition")
    public PainEscalation doctorDisposition(@PathVariable Long id, @Valid @RequestBody Dtos.DoctorDispositionRequest req) {
        return escalationService.doctorDisposition(id, req);
    }

    /** 医生解除风险，相关动作恢复每日任务 */
    @PostMapping("/api/escalations/{id}/clear")
    public PainEscalation clear(@PathVariable Long id, @RequestBody(required = false) Dtos.ClearRiskRequest req) {
        return escalationService.clear(id, req);
    }
}
