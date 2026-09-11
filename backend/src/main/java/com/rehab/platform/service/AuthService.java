package com.rehab.platform.service;

import com.rehab.platform.config.BusinessException;
import com.rehab.platform.config.JwtService;
import com.rehab.platform.dto.Dtos;
import com.rehab.platform.model.User;
import com.rehab.platform.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public Map<String, Object> login(Dtos.LoginRequest request) {
        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new BusinessException(401, "用户名或密码错误"));
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BusinessException(401, "用户名或密码错误");
        }
        Map<String, Object> result = new HashMap<>();
        result.put("token", jwtService.generateToken(user));
        result.put("user", user);
        return result;
    }
}
