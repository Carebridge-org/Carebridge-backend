package com.carebridge.services;


import com.carebridge.dao.UserDAO;
import com.carebridge.dao.ResidentDAO;
import com.carebridge.models.Guardian;
import com.carebridge.models.Resident;
import java.util.List;

public class GuardianService {
    private final UserDAO userDAO;
    private final ResidentDAO residentDAO;

    public GuardianService(UserDAO userDAO, ResidentDAO residentDAO) {
        this.userDAO = userDAO;
        this.residentDAO = residentDAO;
    }

    public Guardian linkResidents(Long guardianId, List<Long> residentIds) {
        Guardian g = (Guardian) userDAO.findById(guardianId);
        List<Resident> residents = residentDAO.findAllByIds(residentIds);

        for (Resident r : residents) g.addResident(r);

        userDAO.save(g); // persist ændringer
        return g;
    }
}