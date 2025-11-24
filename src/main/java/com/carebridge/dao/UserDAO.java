package com.carebridge.dao;

import com.carebridge.entities.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class UserDAO {
    protected final EntityManagerFactory emf;
    private final Logger logger = LoggerFactory.getLogger(UserDAO.class);

    public UserDAO(EntityManagerFactory emf) {
        this.emf = emf;
    }

    public User save(User user) {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            em.persist(user);
            em.getTransaction().commit();
            return user;
        } catch (Exception e) {
            logger.error("Error persisting User to db", e);
            throw new RuntimeException("Error persisting User to db. ", e);
        }
    }

    public User findById(Long id) {
        try (EntityManager em = emf.createEntityManager()) {
            User user = em.find(User.class, id);
            if (user == null) {
                throw new RuntimeException("User not found with ID: " + id);
            }
            return user;
        } catch (Exception e) {
            logger.error("Error retrieving User from db", e);
            throw new RuntimeException("Error retrieving User from db. ", e);
        }
    }

    public List<User> findAll() {
        try (EntityManager em = emf.createEntityManager()) {
            List<User> users = em.createQuery("SELECT u FROM User u", User.class).getResultList();
            if (users.isEmpty()) {
                throw new EntityNotFoundException("No users found");
            }
            return users;
        } catch (Exception e) {
            logger.error("Error retrieving all Users from db", e);
            throw new RuntimeException("Error retrieving all Users from db. ", e);
        }
    }


    public User create(User user) {
        return save(user);
    }
}
