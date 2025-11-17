package com.carebridge.services;

import com.carebridge.dao.ResidentDAO;
import com.carebridge.dao.UserDAO;
import com.carebridge.models.Guardian;
import com.carebridge.models.Resident;
import com.carebridge.models.User;

import java.util.List;

public class UserService {

    private final UserDAO userDAO;
    private final ResidentDAO residentDAO;

    public UserService(UserDAO userDAO, ResidentDAO residentDAO) {
        this.userDAO = userDAO;
        this.residentDAO = residentDAO;
    }

    public Guardian linkResidents(Long guardianId, List<Long> residentIds) {
        User u = userDAO.findById(guardianId);
        if (!(u instanceof Guardian)) throw new IllegalArgumentException("User is not a Guardian");

        Guardian g = (Guardian) u;
        List<Resident> residents = residentDAO.findAllByIds(residentIds);

        for (Resident r : residents) g.addResident(r);

        userDAO.save(g); // persist changes
        return g;
    }

    public List<Resident> getAllResidents() {
        return residentDAO.findAll();
    }

    public List<Guardian> getAllGuardians() {
        return userDAO.findAllGuardians();
    }
}
