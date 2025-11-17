package com.carebridge.controllers;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.carebridge.dao.UserDAO;
import com.carebridge.security.JwtUtil;
import com.carebridge.security.RateLimiter;
import com.carebridge.services.AuditService;
import com.carebridge.services.AuthService;
import com.carebridge.util.HibernateUtil;
import io.javalin.Javalin;
import org.hibernate.SessionFactory;

import java.util.Map;

/**
 * Created from App via: new UserController(app)
 * This wires:
 *  - JWT middleware (ctx.attribute("userId"), ctx.attribute("role"))
 *  - POST /auth/login (email+password -> JWT)
 * Keep your other user routes here too.
 */
public class UserController {

    private final AuthService authService;
    private final AuditService auditService;
    private final JwtUtil jwt;
    private final RateLimiter limiter;

    public UserController(Javalin app) {
        SessionFactory sf = HibernateUtil.getSessionFactory();
        var userDAO = new UserDAO(sf);

        this.authService = new AuthService(userDAO);
        this.auditService = new AuditService();
        this.jwt = new JwtUtil(env("JWT_SECRET", "devsecret"), "carbridge", 900); // 15 min
        this.limiter = new RateLimiter(5, 600, 600); // 5 fails/10min -> block 10min

        // JWT middleware
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

        // Login endpoint
        app.post("/auth/login", ctx -> {
            String email = ctx.formParam("email");
            String password = ctx.formParam("password");
            if (email == null || password == null) {
                ctx.status(400).json(Map.of("error","Missing fields")); return;
            }

            String key = (ctx.ip() + "|" + email).toLowerCase();
            if (limiter.isBlocked(key)) {
                auditService.log(email, ctx.ip(), null, false, "RATE_LIMIT");
                ctx.status(429).json(Map.of(
                        "error","Too many attempts. Try again later.",
                        "retryAfterSec", limiter.secondsUntilUnblock(key)
                ));
                return;
            }

            var user = authService.authenticate(email.trim(), password);
            if (user == null) {
                limiter.registerFail(key);
                auditService.log(email, ctx.ip(), null, false, "WRONG_CREDS");
                ctx.status(401).json(Map.of("error","Invalid email or password"));
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

        //Registering af bruger
        app.post("/auth/register", ctx -> {
            String email = ctx.formParam("email");
            String password = ctx.formParam("password");

            if (email == null || password == null) {
                ctx.status(400).json(Map.of("error","Missing fields"));
                return;
            }

            email = email.trim().toLowerCase();

            //Rate limit på ip(lidt extra sikkerhed mod bots, da vi ikke hoster via cloudflare)
            String key = ("reg|" + ctx.ip()).toLowerCase();
            if (limiter.isBlocked(key)) {
                auditService.log(email, ctx.ip(), null, false, "REG_RATE_LIMIT");
                ctx.status(429).json(Map.of(
                        "error","Too many attempts. Try again later.",
                        "retryAfterSec", limiter.secondsUntilUnblock(key)
                ));
                return;
            }


            //Try catch block så vi kan fange evt fejl
            try {
                var user = authService.register(email, password);
                limiter.reset(key);

                auditService.log(email, ctx.ip(), user, true, "REG_OK");
                String token = jwt.create(user.getId(), user.getRole());
                ctx.status(201).json(Map.of(
                        "token", token,
                        "user", Map.of(
                                "id", user.getId(),
                                "email", user.getEmail(),
                                "role", user.getRole()
                        )
                ));
            } catch (AuthService.EmailExistsException ex) {
                limiter.registerFail(key);
                auditService.log(email, ctx.ip(), null, false, "EMAIL_EXISTS");
                ctx.status(409).json(Map.of("error", "Email already exists"));

            } catch (Exception ex) {
                limiter.registerFail(key);
                auditService.log(email, ctx.ip(), null, false, "REG_ERROR");
                ctx.status(500).json(Map.of("error", "Server error"));
            }
        });
    }




    private static String env(String k, String def) {
        String v = System.getenv(k);
        return v != null ? v : def;
    }
}
