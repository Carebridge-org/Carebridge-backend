package com.carebridge.dao;

import com.carebridge.entities.Journal;
import com.carebridge.entities.JournalEntry;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityNotFoundException;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JournalDAO
{
    protected final EntityManagerFactory emf;
    private final Logger logger = LoggerFactory.getLogger(JournalDAO.class);

    public JournalDAO(EntityManagerFactory emf)
    {
        this.emf = emf;
    }


    public Journal save(Journal journal)
    {
        try (EntityManager em = emf.createEntityManager())
        {
            em.getTransaction().begin();
            em.persist(journal);
            em.getTransaction().commit();
            return journal;
        }
        catch (Exception e)
        {
            logger.error("Error persisting object to db", e);
            throw new RuntimeException("Error persisting object to db. ", e);
        }
    }

    public Journal findById(Object id)
    {
        try (EntityManager em = emf.createEntityManager())
        {
            Journal journalEntity = em.find(Journal.class, id);
            if (journalEntity == null)
            {
                throw new RuntimeException("Journal not found with ID: " + id);
            }
            return journalEntity;
        }
        catch (Exception e)
        {
            logger.error("Error retrieving object from db", e);
            throw new RuntimeException("Error retrieving object from db. ", e);
        }
    }


    public List<Journal> findAll()
    {
        try (EntityManager em = emf.createEntityManager())
        {
            List<Journal> journals = em.createQuery("SELECT j FROM Journal j", Journal.class).getResultList();
            if(journals.isEmpty())
            {
                throw new EntityNotFoundException("No journals found");
            }
            return journals;
        }
    }

    public void addEntryToJournal(Journal journal, JournalEntry journalEntry)
    {
        try (EntityManager em = emf.createEntityManager())
        {
            em.getTransaction().begin();
            journal.addEntry(journalEntry);
            em.merge(journal);
            em.getTransaction().commit();
        }
        catch (Exception e)
        {
            logger.error("Error updating journal with new entry", e);
            throw new RuntimeException("Error updating journal with new entry. ", e);
        }
    }
}
