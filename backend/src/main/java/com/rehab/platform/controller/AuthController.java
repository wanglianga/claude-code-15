package com.rehab.platform.controller;

import com.rehab.platform.config.SecurityUtils;
import com.rehab.platform.dto.Dtos;
import com.rehab.platform.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public Map<String, Object> login(@Valid @RequestBody Dtos.LoginRequest request) {
        return authService.login(request);
    }

    @GetMapping("/me")
    public Object me() {
        return SecurityUtils.currentUser();
    }
}
