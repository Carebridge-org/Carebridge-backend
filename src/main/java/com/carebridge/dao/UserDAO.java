package com.carebridge.dao;

import com.carebridge.models.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class UserDAO {
    private final EntityManager em;

    public User findByEmail(String email) {
        try {
            return em.createQuery("SELECT u FROM User u WHERE u.email = :e", User.class)
                    .setParameter("e", email)
                    .getSingleResult();
        } catch (NoResultException ex) {
            return null;
        }
    }

    public void save(User u) { em.persist(u); }
}
