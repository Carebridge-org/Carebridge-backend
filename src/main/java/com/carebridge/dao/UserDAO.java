package com.carebridge.dao;

import com.carebridge.models.User;
import org.hibernate.SessionFactory;
import org.hibernate.Session;

public class UserDAO {
    private final SessionFactory sf;

    public UserDAO(SessionFactory sf) { this.sf = sf; }

    public User findByEmail(String email) {
        try (Session s = sf.openSession()) {
            return s.createQuery("from User u where u.email = :e", User.class)
                    .setParameter("e", email)
                    .uniqueResult();
        }
    }

    public void save(User u) {
        try (Session s = sf.openSession()) {
            var tx = s.beginTransaction();
            s.persist(u);
            tx.commit();
        }
    }
}
