package com.carebridge.dao;

import com.carebridge.models.Guardian;
import com.carebridge.models.User;
import org.hibernate.SessionFactory;
import org.hibernate.Session;

import java.util.List;

public class UserDAO {
    private final SessionFactory sf;

    public UserDAO(SessionFactory sf) { this.sf = sf; }


    //finder ud fra username
    public User findByUsername(String username) {
        try (Session s = sf.openSession()) {
            return s.createQuery("from User u where u.username = :u", User.class)
                    .setParameter("u", username)
                    .uniqueResult();
        }
    }



    //Finder ud af internal email
    public User findByInternalEmail(String email) {
        try (Session s = sf.openSession()) {
            return s.createQuery("from User u where u.internalEmail = :e", User.class)
                    .setParameter("e", email)
                    .uniqueResult();
        }
    }

    public User findById(Long id) {
        try (Session s = sf.openSession()) {
            return s.get(User.class, id);
        }
    }

    public List<Guardian> findAllGuardians() {
        try (Session s = sf.openSession()) {
            return s.createQuery("from Guardian", Guardian.class).list();
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
