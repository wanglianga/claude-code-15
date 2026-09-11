package com.rehab.platform.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.rehab.platform.enums.Role;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String username;

    @JsonIgnore
    @Column(nullable = false)
    private String password;

    @Column(nullable = false, length = 64)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    /** 职称，如 主管治疗师 / 主治医师 */
    @Column(length = 64)
    private String title;

    @Column(length = 32)
    private String phone;

    private LocalDateTime createdAt = LocalDateTime.now();
}
