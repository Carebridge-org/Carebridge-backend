package com.carebridge.dao;

import com.carebridge.models.Resident;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import java.util.List;

public class ResidentDAO {
    private final SessionFactory sf;

    public ResidentDAO(SessionFactory sf) { this.sf = sf; }

    public List<Resident> findAllByIds(List<Long> ids) {
        try (Session s = sf.openSession()) {
            return s.createQuery("from Resident r where r.id in :ids", Resident.class)
                    .setParameter("ids", ids)
                    .list();
        }
    }

    public List<Resident> findAll() {
        try (Session s = sf.openSession()) {
            return s.createQuery("from Resident", Resident.class).list();
        }
    }

    public Resident findById(Long id) {
        try (Session s = sf.openSession()) {
            return s.get(Resident.class, id);
        }
    }
}
