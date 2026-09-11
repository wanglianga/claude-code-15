package com.rehab.platform.controller;

import com.rehab.platform.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/summary")
    public Map<String, Object> summary() {
        return dashboardService.summary();
    }

    @GetMapping("/risk-board")
    public Map<String, Object> riskBoard() {
        return dashboardService.riskBoard();
    }
}
