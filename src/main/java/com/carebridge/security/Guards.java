package com.carebridge.security;

import io.javalin.http.Context;

public class Guards {
    public static void requireAuth(Context ctx) {
        if (ctx.attribute("userId") == null) {
            ctx.status(401).json(java.util.Map.of("error","Unauthorized"));
            throw new Halt();
        }
    }
    public static void requireRole(Context ctx, String role) {
        requireAuth(ctx);
        if (!role.equals(ctx.attribute("role"))) {
            ctx.status(403).json(java.util.Map.of("error","Forbidden"));
            throw new Halt();
        }
    }
    public static class Halt extends RuntimeException {}
}
