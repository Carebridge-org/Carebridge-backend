package com.carebridge.dao;

import com.carebridge.entities.JournalEntry;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class JournalEntryDAO {
    protected final EntityManagerFactory emf;
    private final Logger logger = LoggerFactory.getLogger(JournalEntryDAO.class);

    public JournalEntryDAO(EntityManagerFactory emf) {
        this.emf = emf;
    }

    public JournalEntry save(JournalEntry journalEntry) {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            em.persist(journalEntry);
            em.getTransaction().commit();
            return journalEntry;
        } catch (Exception e) {
            logger.error("Error persisting JournalEntry to db", e);
            throw new RuntimeException("Error persisting JournalEntry to db. ", e);
        }
    }

    public JournalEntry findById(Object id) {
        try (EntityManager em = emf.createEntityManager()) {
            JournalEntry entry = em.find(JournalEntry.class, id);
            if (entry == null) {
                throw new RuntimeException("JournalEntry not found with ID: " + id);
            }
            return entry;
        } catch (Exception e) {
            logger.error("Error retrieving JournalEntry from db", e);
            throw new RuntimeException("Error retrieving JournalEntry from db. ", e);
        }
    }

    public List<JournalEntry> findAll() {
        try (EntityManager em = emf.createEntityManager()) {
            List<JournalEntry> entries = em.createQuery("SELECT je FROM JournalEntry je", JournalEntry.class)
                    .getResultList();
            if (entries.isEmpty()) {
                throw new EntityNotFoundException("No journal entries found");
            }
            return entries;
        }
    }

    public List<Long> getEntryIdsByJournalId(Long journalId) {
        try (EntityManager em = emf.createEntityManager()) {
            return em.createQuery(
                            "SELECT je.id FROM JournalEntry je WHERE je.journal.id = :journalId",
                            Long.class)
                    .setParameter("journalId", journalId)
                    .getResultList();
        } catch (Exception e) {
            logger.error("Error querying entry IDs by journalId", e);
            throw new RuntimeException("Error querying entry IDs by journalId. ", e);
        }
    }

    public JournalEntry update(JournalEntry journalEntry) {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            JournalEntry merged = em.merge(journalEntry);
            em.getTransaction().commit();
            return merged;
        } catch (Exception e) {
            logger.error("Error updating JournalEntry", e);
            throw new RuntimeException("Error updating JournalEntry. ", e);
        }
    }

    public JournalEntry create(JournalEntry journalEntry) {
        return save(journalEntry);
    }
}
