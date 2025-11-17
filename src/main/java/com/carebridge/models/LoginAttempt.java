package com.carebridge.models;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Entity @Table(name = "login_attempts", indexes = {
        @Index(name = "idx_login_identifier_time", columnList = "identifier,occurredAt")
})
public class LoginAttempt {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String identifier; // email as typed

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user_id")
    private User user; // may be null

    @Column(nullable = false, length = 64)
    private String ip;

    @Column(nullable = false)
    private boolean success;

    @Column(nullable = false, length = 32)
    private String reason; // OK | WRONG_CREDS | RATE_LIMIT

    @Column(nullable = false)
    private Instant occurredAt;
}
