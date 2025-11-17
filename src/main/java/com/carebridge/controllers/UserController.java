package com.carebridge.controllers;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.carebridge.dao.ResidentDAO;
import com.carebridge.dao.UserDAO;
import com.carebridge.dto.CreateUserRequest;
import com.carebridge.dto.LinkResidentsDTO;
import com.carebridge.enums.Role;
import com.carebridge.models.Guardian;
import com.carebridge.models.Resident;
import com.carebridge.security.JwtUtil;
import com.carebridge.security.RateLimiter;
import com.carebridge.services.AuditService;
import com.carebridge.services.AuthService;
import com.carebridge.util.HibernateUtil;
import io.javalin.Javalin;
import org.hibernate.SessionFactory;

import java.util.List;
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
        ResidentDAO residentDAO = new ResidentDAO(sf);

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
            String username = ctx.formParam("username");
            String password = ctx.formParam("password");

            if (username == null || password == null) {
                ctx.status(400).json(Map.of("error", "Missing fields"));
                return;
            }

            String key = (ctx.ip() + "|" + username).toLowerCase();
            if (limiter.isBlocked(key)) {
                auditService.log(username, ctx.ip(), null, false, "RATE_LIMIT");
                ctx.status(429).json(Map.of(
                        "error", "Too many attempts. Try again later.",
                        "retryAfterSec", limiter.secondsUntilUnblock(key)
                ));
                return;
            }

            var user = authService.authenticate(username.trim(), password); // AuthService skal også bruge username nu
            if (user == null) {
                limiter.registerFail(key);
                auditService.log(username, ctx.ip(), null, false, "WRONG_CREDS");
                ctx.status(401).json(Map.of("error", "Invalid username or password"));
                return;
            }

            limiter.reset(key);
            auditService.log(username, ctx.ip(), user, true, "OK");

            String token = jwt.create(user.getId(), user.getRole().name());
            ctx.json(Map.of(
                    "token", token,
                    "user", Map.of(
                            "id", user.getId(),
                            "username", user.getUsername(),
                            "role", user.getRole().name()
                    )
            ));
        });



        app.post("/users", ctx -> {
            Long currentUserId = ctx.attribute("userId");
            String currentRole = ctx.attribute("role");

            if (currentRole == null || !(currentRole.equals("ADMIN") || currentRole.equals("MANAGER"))) {
                ctx.status(403).json(Map.of("error", "Unauthorized"));
                return;
            }

            // Læs JSON body som CreateUserRequest
            CreateUserRequest req = ctx.bodyAsClass(CreateUserRequest.class);

            if (req.username == null || req.password == null || req.role == null) {
                ctx.status(400).json(Map.of("error", "username, password and role are required"));
                return;
            }

            try {
                var role = Role.valueOf(req.role.toUpperCase());

                if (authService.findByUsername(req.username) != null) {
                    ctx.status(409).json(Map.of("error", "Username already exists"));
                    return;
                }

                var newUser = authService.registerFull(
                        req.username,
                        req.password,
                        req.displayName,
                        req.displayEmail,
                        req.displayPhone,
                        req.internalEmail,
                        req.internalPhone,
                        role
                );

                auditService.log(
                        newUser.getUsername(),
                        "missing",
                        newUser,
                        true,
                        "CREATED_USER"
                );

                ctx.status(201).json(Map.of(
                        "id", newUser.getId(),
                        "username", newUser.getUsername(),
                        "role", newUser.getRole().name()
                ));

            } catch (IllegalArgumentException ex) {
                ctx.status(400).json(Map.of("error", "Invalid role"));
            } catch (Exception ex) {
                ctx.status(500).json(Map.of("error", "Server error"));
                ex.printStackTrace();
            }
        });



        app.post("/users/{id}/link-residents", ctx -> {
            Long guardianId = Long.parseLong(ctx.pathParam("id"));
            String currentRole = ctx.attribute("role");

            if (currentRole == null || !(currentRole.equals("ADMIN") || currentRole.equals("MANAGER"))) {
                ctx.status(403).json(Map.of("error", "Unauthorized"));
                return;
            }

            LinkResidentsDTO body = ctx.bodyAsClass(LinkResidentsDTO.class);
            List<Long> residentIds = body.getResidentIds();
            if (residentIds == null || residentIds.isEmpty()) {
                ctx.status(400).json(Map.of("error", "residentIds are required"));
                return;
            }

            Guardian guardian = (Guardian) userDAO.findById(guardianId);
            if (guardian == null) {
                ctx.status(404).json(Map.of("error", "Guardian not found"));
                return;
            }

            List<Resident> residents = residentDAO.findAllByIds(residentIds);
            if (residents.isEmpty()) {
                ctx.status(404).json(Map.of("error", "No valid residents found"));
                return;
            }

            residents.forEach(guardian::addResident);
            userDAO.save(guardian);

            auditService.log(
                    ctx.attribute("userId").toString(),
                    ctx.ip(),
                    guardian,
                    true,
                    "LINK_RESIDENTS"
            );

            ctx.json(Map.of(
                    "guardianId", guardian.getId(),
                    "linkedResidents", guardian.getResidents().stream().map(Resident::getId).toList()
            ));
        });

        // Hent alle Guardians
        app.get("/guardians", ctx -> {
            List<Guardian> guardians = userDAO.findAllGuardians();
            ctx.json(guardians.stream().map(g -> Map.of(
                    "id", g.getId(),
                    "username", g.getUsername()
            )).toList());
        });

        // Hent alle Residents
        app.get("/residents", ctx -> {
            List<Resident> residents = residentDAO.findAll();
            ctx.json(residents.stream().map(r -> Map.of(
                    "id", r.getId(),
                    "roomNumber", r.getRoomNumber()
            )).toList());
        });



    }




    private static String env(String k, String def) {
        String v = System.getenv(k);
        return v != null ? v : def;
    }
}
