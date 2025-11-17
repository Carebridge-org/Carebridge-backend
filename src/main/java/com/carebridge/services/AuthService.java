package com.carebridge.services;

import com.carebridge.dao.UserDAO;
import com.carebridge.models.User;
import org.mindrot.jbcrypt.BCrypt;

public class AuthService {
    private final UserDAO users;


    public static class EmailExistsException extends RuntimeException  {}
    public AuthService(UserDAO users) { this.users = users; }

    public User authenticate(String email, String rawPassword) {
        User u = users.findByEmail(email);
        if (u == null) return null;
        return BCrypt.checkpw(rawPassword, u.getPasswordHash()) ? u : null;
    }

    public User register(String email, String rawPassword) {
        if (users.findByEmail(email) != null) {
            throw new EmailExistsException();
        }

        String hashed =  BCrypt.hashpw(rawPassword, BCrypt.gensalt());

        User u = new User();
        u.setEmail(email);
        u.setPasswordHash(hashed);
        u.setRole("user");

        users.save(u);
        return u;
    }

    public String hash(String raw) { return BCrypt.hashpw(raw, BCrypt.gensalt(12)); }
}
