package com.carebridge.models;

import com.carebridge.enums.Role;
import jakarta.persistence.*;
import lombok.*;
import java.util.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Entity
@Table(name = "users")
@Inheritance(strategy = InheritanceType.JOINED) // Tabel pr. subklasse
public class User {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 255)
    private String username;

    @Column(nullable = false, length = 60) // BCrypt hash
    private String passwordHash;

    @Column(nullable = false, length = 24)
    @Enumerated(EnumType.STRING)
    private Role role;

    @Column(length = 255)
    private String displayName;

    @Column(length = 20)
    private String displayPhone;

    @Column(length = 255)
    private String displayEmail;

    @Column(nullable = false, length = 20)
    private String internalPhone;

    @Column(nullable = false, length = 255)
    private String internalEmail;
}
