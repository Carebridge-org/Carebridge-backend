package com.carebridge.dao;

import com.carebridge.models.LoginAttempt;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class LoginAttemptDAO {
    private final EntityManager em;
    public void save(LoginAttempt a) { em.persist(a); }
}
