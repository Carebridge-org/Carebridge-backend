package com.carebridge.dao;

import com.carebridge.entities.JournalEntry;

public interface IJournalEntryDAO
{
    JournalEntry save(JournalEntry entry);
    JournalEntry findById(Long id);

}
