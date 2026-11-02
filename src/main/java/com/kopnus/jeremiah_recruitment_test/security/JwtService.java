package com.kopnus.jeremiah_recruitment_test.security;

import com.kopnus.jeremiah_recruitment_test.config.JwtProperties;
import com.kopnus.jeremiah_recruitment_test.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtService {

    private static final String USERNAME_CLAIM = "username";

    private final SecretKey key;
    private final Duration expiration;

    public JwtService(JwtProperties properties) {
        // Fails at startup if the secret is shorter than 256 bits
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(properties.secret()));
        this.expiration = properties.expiration();
    }

    public String generateToken(User user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(user.getId()))        // who the token belongs to
                .claim(USERNAME_CLAIM, user.getUsername())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(expiration)))
                .signWith(key)                                // HS256, chosen from the key size
                .compact();
    }

    /**
     * Verifies the signature and expiry, then returns the user inside the token.
     *
     * @throws  if the token is malformed, tampered with or expired
     */
    public AuthenticatedUser parseToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return new AuthenticatedUser(
                Long.valueOf(claims.getSubject()),
                claims.get(USERNAME_CLAIM, String.class));
    }

    public Duration getExpiration() {
        return expiration;
    }
}
