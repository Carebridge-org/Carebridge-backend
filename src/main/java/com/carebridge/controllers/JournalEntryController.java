package com.carebridge.controllers;

import com.carebridge.config.HibernateConfig;
import com.carebridge.dao.JournalDAO;
import com.carebridge.dao.JournalEntryDAO;
import com.carebridge.dao.UserDAO;
import com.carebridge.dtos.CreateJournalEntryRequestDTO;
import com.carebridge.dtos.EditJournalEntryRequestDTO;
import com.carebridge.dtos.JournalEntryResponseDTO;
import com.carebridge.entities.Journal;
import com.carebridge.entities.JournalEntry;
import com.carebridge.entities.User;
import io.javalin.http.Context;
import jakarta.persistence.EntityManagerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;

public class JournalEntryController {

    private final JournalEntryDAO jeDAO;
    private final JournalDAO jDAO;
    private final UserDAO userDAO;
    private static final Logger logger = LoggerFactory.getLogger(JournalEntryController.class);
    private final static EntityManagerFactory emf = HibernateConfig.getEntityManagerFactory();

    public JournalEntryController(EntityManagerFactory emf) {
        this.jeDAO = new JournalEntryDAO(emf);
        this.jDAO = new JournalDAO(emf);
        this.userDAO = new UserDAO(emf);
    }

    // Finding all entries by a journal ID
    public void findAllEntriesByJournal(Context ctx) {
        try {
            Long journalId = Long.parseLong(ctx.pathParam("journalId"));
            List<Long> ids = jeDAO.getEntryIdsByJournalId(journalId);
            ctx.json(ids);
        } catch (IllegalArgumentException e) {
            ctx.status(400).result(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            ctx.status(500).result("Internal server error");
        }
    }

    // Create a new journal entry (logic moved from service)
    public void createJournalEntry(Context ctx) {
        try {
            Long journalId = Long.parseLong(ctx.pathParam("journalId"));

            CreateJournalEntryRequestDTO requestDTO = ctx.bodyAsClass(CreateJournalEntryRequestDTO.class);
            requestDTO.setJournalId(journalId);
            // TODO: replace hardcoded author with authenticated user ID
            requestDTO.setAuthorUserId(3L);

            // --- 1. Fetch Journal and Author ---
            Journal journal = jDAO.findById(requestDTO.getJournalId());
            if (journal == null) {
                throw new IllegalArgumentException("Journal not found with ID: " + requestDTO.getJournalId());
            }

            User author = userDAO.findById(requestDTO.getAuthorUserId());
            if (author == null) {
                throw new IllegalArgumentException("Author not found with ID: " + requestDTO.getAuthorUserId());
            }

            // --- 2. Validate Required Input ---
            if (requestDTO.getTitle() == null || requestDTO.getTitle().isBlank()) {
                throw new IllegalArgumentException("Title is required.");
            }
            if (requestDTO.getContent() == null || requestDTO.getContent().isBlank()) {
                throw new IllegalArgumentException("Content is required.");
            }
            if (requestDTO.getEntryType() == null) {
                throw new IllegalArgumentException("Entry type is required.");
            }
            if (requestDTO.getRiskAssessment() == null) {
                throw new IllegalArgumentException("Risk assessment is required.");
            }

            // --- 3. Build entity ---
            JournalEntry entry = new JournalEntry();
            entry.setJournal(journal);
            entry.setAuthor(author);
            entry.setTitle(requestDTO.getTitle());
            entry.setContent(requestDTO.getContent());
            entry.setEntryType(requestDTO.getEntryType());
            entry.setRiskAssessment(requestDTO.getRiskAssessment());

            LocalDateTime now = LocalDateTime.now();
            entry.setCreatedAt(now);
            entry.setUpdatedAt(now);
            entry.setEditCloseTime(now.plusHours(24));

            // --- 4. Persist ---
            jeDAO.save(entry);

            // --- 5. Build response DTO ---
            JournalEntryResponseDTO responseDTO = new JournalEntryResponseDTO(
                    entry.getId(),
                    journal.getId(),
                    author.getId(),
                    entry.getTitle(),
                    entry.getContent(),
                    entry.getEntryType(),
                    entry.getRiskAssessment(),
                    entry.getCreatedAt(),
                    entry.getUpdatedAt(),
                    entry.getEditCloseTime()
            );

            ctx.status(201).json(responseDTO);

            // Add entry to journal (only if creation succeeded)
            if (ctx.status().getCode() == 201) {
                jDAO.addEntryToJournal(journalId, responseDTO.getId());
            }

        } catch (IllegalArgumentException e) {
            ctx.status(400).result(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            ctx.status(500).result("Internal server error");
        }
    }

    // Edit entry content (logic moved from service)
    public void editJournalEntry(Context ctx) {
        try {
            Long journalId = Long.parseLong(ctx.pathParam("journalId"));
            Long entryId = Long.parseLong(ctx.pathParam("entryId"));

            EditJournalEntryRequestDTO requestDTO = ctx.bodyAsClass(EditJournalEntryRequestDTO.class);

            Journal journal = jDAO.findById(journalId);
            if (journal == null) {
                throw new IllegalArgumentException("Journal not found with ID: " + journalId);
            }

            JournalEntry entry = jeDAO.findById(entryId);
            if (entry == null) {
                throw new IllegalArgumentException("Journal entry not found with ID: " + entryId);
            }

            if (entry.getJournal() == null || entry.getJournal().getId() == null ||
                    !entry.getJournal().getId().equals(journalId)) {
                throw new IllegalArgumentException("Journal entry does not belong to the specified journal.");
            }

            if (requestDTO.getContent() == null || requestDTO.getContent().isBlank()) {
                throw new IllegalArgumentException("Content is required.");
            }

            LocalDateTime now = LocalDateTime.now();
            if (entry.getEditCloseTime() == null || now.isAfter(entry.getEditCloseTime())) {
                throw new IllegalArgumentException("Edit window has closed for this entry.");
            }

            entry.setContent(requestDTO.getContent());
            entry.setUpdatedAt(now);

            jeDAO.update(entry);

            JournalEntryResponseDTO responseDTO = new JournalEntryResponseDTO(
                    entry.getId(),
                    journal.getId(),
                    entry.getAuthor() != null ? entry.getAuthor().getId() : null,
                    entry.getTitle(),
                    entry.getContent(),
                    entry.getEntryType(),
                    entry.getRiskAssessment(),
                    entry.getCreatedAt(),
                    entry.getUpdatedAt(),
                    entry.getEditCloseTime()
            );

            ctx.status(200).json(responseDTO);

        } catch (IllegalArgumentException e) {
            ctx.status(400).result(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            ctx.status(500).result("Internal server error");
        }
    }

    // Get entry details (logic moved from service)
    public void getEntryDetails(Context ctx) {
        try {
            Long entryId = Long.parseLong(ctx.pathParam("entryId"));
            JournalEntry entry = jeDAO.findById(entryId);

            if (entry == null) {
                throw new IllegalArgumentException("Journal entry with ID " + entryId + " not found");
            }

            JournalEntryResponseDTO dto = new JournalEntryResponseDTO(
                    entry.getId(),
                    entry.getJournal() != null ? entry.getJournal().getId() : null,
                    entry.getAuthor() != null ? entry.getAuthor().getId() : null,
                    entry.getTitle(),
                    entry.getContent(),
                    entry.getEntryType(),
                    entry.getRiskAssessment(),
                    entry.getCreatedAt(),
                    entry.getUpdatedAt(),
                    entry.getEditCloseTime()
            );

            ctx.json(dto);
        } catch (IllegalArgumentException e) {
            ctx.status(400).result(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            ctx.status(500).result("Internal server error");
        }
    }
}
