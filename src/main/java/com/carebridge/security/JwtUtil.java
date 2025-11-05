package com.carebridge.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;

import java.time.Instant;
import java.util.Date;

public class JwtUtil {
    private final Algorithm alg;
    private final String issuer;
    private final long ttlSeconds;

    public JwtUtil(String secret, String issuer, long ttlSeconds) {
        this.alg = Algorithm.HMAC256(secret);
        this.issuer = issuer;
        this.ttlSeconds = ttlSeconds;
    }

    public String create(long userId, String role) {
        Instant now = Instant.now();
        return JWT.create()
                .withIssuer(issuer)
                .withIssuedAt(Date.from(now))
                .withExpiresAt(Date.from(now.plusSeconds(ttlSeconds)))
                .withClaim("sub", userId)
                .withClaim("role", role)
                .sign(alg);
    }

    public DecodedJWT verify(String token) {
        return JWT.require(alg).withIssuer(issuer).build().verify(token);
    }
}
