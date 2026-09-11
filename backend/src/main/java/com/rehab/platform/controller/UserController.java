package com.rehab.platform.controller;

import com.rehab.platform.enums.Role;
import com.rehab.platform.model.User;
import com.rehab.platform.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;

    /** 按角色查询用户（如家属账号列表、治疗师列表） */
    @GetMapping
    public List<User> byRole(@RequestParam Role role) {
        return userRepository.findByRole(role);
    }
}
