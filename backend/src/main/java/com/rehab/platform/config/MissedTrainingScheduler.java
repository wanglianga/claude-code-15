package com.rehab.platform.config;

import com.rehab.platform.service.AlertService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 每日早晨扫描连续漏练患者，自动生成预警推送护士随访队列
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MissedTrainingScheduler {

    private final AlertService alertService;

    @Scheduled(cron = "0 0 7 * * *")
    public void scan() {
        log.info("定时任务：扫描连续漏练患者");
        alertService.scanMissedTraining();
    }
}
