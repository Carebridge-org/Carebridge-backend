package com.carebridge.models;

import jakarta.persistence.*;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Entity @Table(name = "users")
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(nullable = false, length = 60) // BCrypt hash
    private String passwordHash;

    @Column(nullable = false, length = 24) // GUARDIAN|PARENT|CASE_WORKER|PEDAGOGUE|ADMIN
    private String role;
}
