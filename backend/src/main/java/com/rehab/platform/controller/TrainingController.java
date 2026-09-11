package com.rehab.platform.controller;

import com.rehab.platform.dto.Dtos;
import com.rehab.platform.model.TrainingLog;
import com.rehab.platform.service.TrainingLogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class TrainingController {

    private final TrainingLogService trainingLogService;

    /** 今日训练任务（家属端） */
    @GetMapping("/api/patients/{patientId}/today-tasks")
    public Map<String, Object> todayTasks(@PathVariable Long patientId) {
        return trainingLogService.todayTasks(patientId);
    }

    /** 提交每日打卡 */
    @PostMapping("/api/patients/{patientId}/training-logs")
    public TrainingLog submit(@PathVariable Long patientId, @Valid @RequestBody Dtos.TrainingLogRequest req) {
        return trainingLogService.submit(patientId, req);
    }

    @GetMapping("/api/patients/{patientId}/training-logs")
    public List<TrainingLog> list(@PathVariable Long patientId, @RequestParam(required = false) Integer days) {
        return trainingLogService.listByPatient(patientId, days);
    }

    /** 视频纠错记录 */
    @GetMapping("/api/patients/{patientId}/corrections")
    public List<TrainingLog> corrections(@PathVariable Long patientId) {
        return trainingLogService.corrections(patientId);
    }

    /** 治疗师写纠错反馈 */
    @PostMapping("/api/training-logs/{logId}/feedback")
    public TrainingLog feedback(@PathVariable Long logId, @Valid @RequestBody Dtos.FeedbackRequest req) {
        return trainingLogService.feedback(logId, req);
    }
}
