package com.carebridge.routes;

import com.carebridge.controllers.JournalEntryController;
import com.carebridge.controllers.UserController;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.apibuilder.EndpointGroup;

import static io.javalin.apibuilder.ApiBuilder.*;

public class Routes
{
    private final JournalEntryController journalEntryController;
    private final UserController userController;
    private final ObjectMapper jsonMapper = new ObjectMapper();

    public Routes(JournalEntryController journalEntryController, UserController userController)
    {
        this.userController = userController;
        this.journalEntryController = journalEntryController;
    }

    public EndpointGroup getRoutes()
    {
        return () ->
        {
            path("/users", UserRoutes.getRoutes());
            path("/journalentries", JournalEntryRoutes.getRoutes());
        };

    }

    private EndpointGroup userRoutes()
    {
        return () ->
        {
            get("/users", userController::getAllUsers);
            post("/users", userController::createUser);
        };
    }

    private EndpointGroup JournalEntryRoutes()
    {
        return () ->
        {
            get("/journals/{journalId}/journal-entries", journalEntryController::findAllEntriesByJournal);
            post("/journals/{journalId}/journal-entries", journalEntryController::createJournalEntry);
            put("/journals/{journalId}/journal-entries/{entryId}", journalEntryController::editJournalEntry);
            get("/journals/{journalId}/journal-entries/{entryId}", journalEntryController::getEntryDetails);
        };
    }
}
