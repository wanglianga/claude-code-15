package com.rehab.platform.controller;

import com.rehab.platform.dto.Dtos;
import com.rehab.platform.enums.KeyPoint;
import com.rehab.platform.model.CorrectionTask;
import com.rehab.platform.service.CorrectionTaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class CorrectionTaskController {

    private final CorrectionTaskService correctionTaskService;

    /** 关键动作点字典 */
    @GetMapping("/api/key-points")
    public List<Map<String, String>> keyPoints() {
        return Arrays.stream(KeyPoint.values())
                .map(k -> Map.of("code", k.name(), "label", k.getLabel()))
                .toList();
    }

    @GetMapping("/api/patients/{patientId}/correction-tasks")
    public List<CorrectionTask> list(@PathVariable Long patientId) {
        return correctionTaskService.listByPatient(patientId);
    }

    /** 待确认的纠错任务（家属端打卡闸门） */
    @GetMapping("/api/patients/{patientId}/correction-tasks/pending")
    public List<CorrectionTask> pending(@PathVariable Long patientId) {
        return correctionTaskService.pendingConfirmations(patientId);
    }

    /** 治疗师打回视频 */
    @PostMapping("/api/training-logs/{logId}/correction-tasks")
    public CorrectionTask create(@PathVariable Long logId, @Valid @RequestBody Dtos.CorrectionTaskRequest req) {
        return correctionTaskService.create(logId, req);
    }

    /** 患者/家属确认已观看 */
    @PostMapping("/api/correction-tasks/{id}/confirm")
    public CorrectionTask confirm(@PathVariable Long id) {
        return correctionTaskService.confirm(id);
    }

    /** 治疗师复评（对比新旧视频） */
    @PostMapping("/api/correction-tasks/{id}/review")
    public CorrectionTask review(@PathVariable Long id, @Valid @RequestBody Dtos.CorrectionReviewRequest req) {
        return correctionTaskService.review(id, req);
    }
}
