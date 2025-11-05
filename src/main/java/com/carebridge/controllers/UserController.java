package com.carebridge.controllers;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.carebridge.dao.LoginAttemptDAO;
import com.carebridge.dao.UserDAO;
import com.carebridge.models.User;
import com.carebridge.security.JwtUtil;
import com.carebridge.security.RateLimiter;
import com.carebridge.services.AuditService;
import com.carebridge.services.AuthService;
import com.carebridge.util.HibernateUtil;
import io.javalin.Javalin;

import jakarta.persistence.EntityManager;
import java.util.Map;

/**
 * NOTE: Your App creates only this controller. We keep that contract.
 * This controller now ALSO wires:
 *  - JWT middleware (ctx.attribute("userId"), ctx.attribute("role"))
 *  - POST /auth/login  (email+password -> JWT)
 *
 * Keep your existing user routes below; nothing else in App changes.
 */
public class UserController {

    private final AuthService authService;
    private final AuditService auditService;
    private final JwtUtil jwt;
    private final RateLimiter limiter;

    public UserController(Javalin app) {
        // --- minimal wiring using your HibernateUtil ---
        EntityManager em = HibernateUtil.getEntityManagerFactory().createEntityManager();
        var userDAO = new UserDAO(em);
        var attemptDAO = new LoginAttemptDAO(em);

        this.authService = new AuthService(userDAO);
        this.auditService = new AuditService(attemptDAO, em);
        this.jwt = new JwtUtil(env("JWT_SECRET", "devsecret"), "carbridge", 900); // 15 min
        this.limiter = new RateLimiter(5, 600, 600); // 5 fails/10min -> block 10min

        // --- JWT middleware: decode Authorization header into ctx attributes ---
        app.before(ctx -> {
            String authz = ctx.header("Authorization");
            if (authz != null && authz.startsWith("Bearer ")) {
                try {
                    DecodedJWT d = jwt.verify(authz.substring(7));
                    ctx.attribute("userId", d.getClaim("sub").asLong());
                    ctx.attribute("role", d.getClaim("role").asString());
                } catch (Exception ignored) {}
            }
        });

        // --- Login endpoint required by the story ---
        app.post("/auth/login", ctx -> {
            String email = ctx.formParam("email");
            String password = ctx.formParam("password");
            if (email == null || password == null) {
                ctx.status(400).json(Map.of("error", "Missing fields"));
                return;
            }

            String key = (ctx.ip() + "|" + email).toLowerCase();
            if (limiter.isBlocked(key)) {
                auditService.log(email, ctx.ip(), null, false, "RATE_LIMIT");
                ctx.status(429).json(Map.of(
                        "error", "Too many attempts. Try again later.",
                        "retryAfterSec", limiter.secondsUntilUnblock(key)
                ));
                return;
            }

            User user = authService.authenticate(email.trim(), password);
            if (user == null) {
                limiter.registerFail(key);
                auditService.log(email, ctx.ip(), null, false, "WRONG_CREDS");
                ctx.status(401).json(Map.of("error", "Invalid email or password"));
                return;
            }

            limiter.reset(key);
            auditService.log(email, ctx.ip(), user, true, "OK");

            String token = jwt.create(user.getId(), user.getRole());
            ctx.json(Map.of(
                    "token", token,
                    "user", Map.of("id", user.getId(), "email", user.getEmail(), "role", user.getRole())
            ));
        });

        // --- YOUR EXISTING USER ROUTES CAN STAY BELOW ---
        // e.g. app.get("/users", ctx -> { ... });
        // Use ctx.attribute("role") or "userId" and/or a Guards helper for role checks.
    }

    private static String env(String k, String def) {
        String v = System.getenv(k);
        return v != null ? v : def;
    }
}
