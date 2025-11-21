package com.carebridge.services;

import com.carebridge.dao.UserDAO;
import com.carebridge.enums.Role;
import com.carebridge.models.CareWorker;
import com.carebridge.models.Guardian;
import com.carebridge.models.Resident;
import com.carebridge.models.User;
import org.mindrot.jbcrypt.BCrypt;

public class AuthService {
    private final UserDAO users;


    public static class UsernameExistsException extends RuntimeException {}
    public static class EmailExistsException extends RuntimeException {}
    public AuthService(UserDAO users) { this.users = users; }

    public User authenticate(String username, String rawPassword) {
        User u = users.findByUsername(username);
        if (u == null) return null;
        return BCrypt.checkpw(rawPassword, u.getPasswordHash()) ? u : null;
    }

    public User findByUsername(String username) {
        return users.findByUsername(username);
    }

    public User registerFull(
            String username,
            String rawPassword,
            String displayName,
            String displayEmail,
            String displayPhone,
            String internalEmail,
            String internalPhone,
            Role role
    ) {

        if (users.findByUsername(username) != null) {
            throw new EmailExistsException();
        }

        User u;

        switch (role) {
            case GUARDIAN:
                u = new Guardian();
                break;

            case CAREWORKER:
                u = new CareWorker(); // hvis du har flere klasser
                break;
            default:
                u = new User();
                break;
        }

        u.setUsername(username);
        u.setPasswordHash(hash(rawPassword));
        u.setDisplayName(displayName);
        u.setDisplayEmail(displayEmail);
        u.setDisplayPhone(displayPhone);
        u.setInternalEmail(internalEmail);
        u.setInternalPhone(internalPhone);
        u.setRole(role);

        users.save(u);
        return u;
    }






    public String hash(String raw) { return BCrypt.hashpw(raw, BCrypt.gensalt(12)); }
}
