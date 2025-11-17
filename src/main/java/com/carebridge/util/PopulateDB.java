package com.carebridge.util;

import com.carebridge.models.User;
import com.carebridge.enums.Role;
import org.mindrot.jbcrypt.BCrypt;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

public class PopulateDB {
    public static void main(String[] args) {
        SessionFactory sf = HibernateUtil.getSessionFactory();

        try (Session session = sf.openSession()) {
            Transaction tx = session.beginTransaction();

            // Tjek om brugeren allerede findes
            User existing = session.createQuery("from User u where u.username = :u", User.class)
                    .setParameter("u", "admin")
                    .uniqueResult();

            if (existing == null) {
                User admin = new User();
                admin.setUsername("admin");
                admin.setPasswordHash(BCrypt.hashpw("admin123", BCrypt.gensalt()));
                admin.setRole(Role.ADMIN);
                admin.setDisplayName("Administrator");
                admin.setDisplayEmail("admin@example.com");
                admin.setDisplayPhone("12345678");
                admin.setInternalEmail("admin.internal@example.com");
                admin.setInternalPhone("87654321");

                session.persist(admin);
                System.out.println("Admin user created ✅");
            } else {
                System.out.println("Admin user already exists ⚠️");
            }

            tx.commit();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            HibernateUtil.shutdown();
        }
    }
}
