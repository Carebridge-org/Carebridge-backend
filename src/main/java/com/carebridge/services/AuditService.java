package com.carebridge.services;

import com.carebridge.models.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Minimal audit that satisfies the story: logs success/failure.
 * No DB changes needed; uses Logback (slf4j).
 */
public class AuditService {
    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    public void log(String email, String ip, User user, boolean success, String reason) {
        // No sensitive data. Include userId if known.
        Long userId = (user != null ? user.getId() : null);
        log.info("login_attempt email={} ip={} success={} reason={} userId={}",
                email, ip, success, reason, userId);
    }
}
