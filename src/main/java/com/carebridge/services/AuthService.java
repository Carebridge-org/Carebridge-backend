package com.carebridge.services;

import com.carebridge.dao.UserDAO;
import com.carebridge.models.User;
import org.mindrot.jbcrypt.BCrypt;

public class AuthService {
    private final UserDAO users;
    public AuthService(UserDAO users) { this.users = users; }

    public User authenticate(String email, String rawPassword) {
        User u = users.findByEmail(email);
        if (u == null) return null;
        return BCrypt.checkpw(rawPassword, u.getPasswordHash()) ? u : null;
    }

    public String hash(String raw) { return BCrypt.hashpw(raw, BCrypt.gensalt(12)); }
}
