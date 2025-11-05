package com.carebridge.services;

import com.carebridge.dao.LoginAttemptDAO;
import com.carebridge.models.LoginAttempt;
import com.carebridge.models.User;
import jakarta.persistence.EntityManager;

import java.time.Instant;

public class AuditService {
    private final LoginAttemptDAO dao; private final EntityManager em;
    public AuditService(LoginAttemptDAO dao, EntityManager em) { this.dao = dao; this.em = em; }

    public void log(String email, String ip, User user, boolean success, String reason) {
        var entry = LoginAttempt.builder()
                .identifier(email).ip(ip).user(user)
                .success(success).reason(reason).occurredAt(Instant.now())
                .build();
        em.getTransaction().begin(); dao.save(entry); em.getTransaction().commit();
    }
}
