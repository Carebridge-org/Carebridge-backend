package com.carebridge.controllers;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.carebridge.security.JwtUtil;
import com.carebridge.security.RateLimiter;
import com.carebridge.services.AuditService;
import com.carebridge.services.AuthService;
import io.javalin.Javalin;
import java.util.Map;

public class AuthController {
    public static void register(Javalin app, AuthService auth, JwtUtil jwt, AuditService audit, RateLimiter limiter) {

        app.post("/auth/login", ctx -> {
            String email = ctx.formParam("email");
            String password = ctx.formParam("password");
            if (email == null || password == null) { ctx.status(400).json(Map.of("error","Missing fields")); return; }

            String key = (ctx.ip() + "|" + email).toLowerCase();
            if (limiter.isBlocked(key)) {
                audit.log(email, ctx.ip(), null, false, "RATE_LIMIT");
                ctx.status(429).json(Map.of("error","Too many attempts. Try again later.","retryAfterSec", limiter.secondsUntilUnblock(key)));
                return;
            }

            var user = auth.authenticate(email.trim(), password);
            if (user == null) {
                limiter.registerFail(key);
                audit.log(email, ctx.ip(), null, false, "WRONG_CREDS");
                ctx.status(401).json(Map.of("error","Invalid email or password"));
                return;
            }

            limiter.reset(key);
            audit.log(email, ctx.ip(), user, true, "OK");

            String token = jwt.create(user.getId(), user.getRole());
            // Frontend decides where to go; we include role so they can choose a landing page
            ctx.json(Map.of("token", token, "user", Map.of("id", user.getId(), "email", user.getEmail(), "role", user.getRole())));
        });

        // Minimal JWT middleware: decode token -> ctx attributes
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
    }
}
