package com.rehab.platform.controller;

import com.rehab.platform.dto.Dtos;
import com.rehab.platform.enums.AlertStatus;
import com.rehab.platform.model.Alert;
import com.rehab.platform.model.DoctorIntervention;
import com.rehab.platform.model.NurseFollowup;
import com.rehab.platform.service.AlertService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
public class AlertController {

    private final AlertService alertService;

    @GetMapping
    public List<Alert> list(@RequestParam(required = false) AlertStatus status) {
        return alertService.list(status);
    }

    @GetMapping("/{id}")
    public Alert one(@PathVariable Long id) {
        return alertService.getOne(id);
    }

    /** 护士接单 */
    @PostMapping("/{id}/assign")
    public Alert assign(@PathVariable Long id) {
        return alertService.assign(id);
    }

    /** 护士电话随访 */
    @PostMapping("/{id}/followups")
    public NurseFollowup followup(@PathVariable Long id, @Valid @RequestBody Dtos.FollowupRequest req) {
        return alertService.addFollowup(id, req);
    }

    @GetMapping("/{id}/followups")
    public List<NurseFollowup> followups(@PathVariable Long id) {
        return alertService.followups(id);
    }

    /** 转医生 */
    @PostMapping("/{id}/escalate")
    public Alert escalate(@PathVariable Long id) {
        return alertService.escalate(id);
    }

    /** 医生处理意见 */
    @PostMapping("/{id}/doctor-opinion")
    public DoctorIntervention doctorOpinion(@PathVariable Long id, @Valid @RequestBody Dtos.DoctorOpinionRequest req) {
        return alertService.doctorOpinion(id, req);
    }

    /** 解除预警 */
    @PostMapping("/{id}/resolve")
    public Alert resolve(@PathVariable Long id) {
        return alertService.resolve(id);
    }

    /** 手动触发漏练扫描（演示用） */
    @PostMapping("/scan-missed")
    public String scanMissed() {
        alertService.scanMissedTraining();
        return "扫描完成";
    }
}
