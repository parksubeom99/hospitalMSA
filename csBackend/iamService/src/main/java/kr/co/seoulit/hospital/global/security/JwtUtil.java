package kr.co.seoulit.hospital.global.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;

import io.jsonwebtoken.Claims;

public class JwtUtil {

    public static final String DEFAULT_SECRET = "CHANGE_ME_LOCAL_SECRET_CHANGE_ME_LOCAL_SECRET";

    public static Key key(String secret) {
        String s = (secret == null || secret.isBlank()) ? DEFAULT_SECRET : secret;
        return Keys.hmacShaKeyFor(s.getBytes(StandardCharsets.UTF_8));
    }


    public static Claims parseClaims(String token, String secret) {
        return Jwts.parser()
                .verifyWith((javax.crypto.SecretKey) key(secret))
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public static String createToken(String subject, List<String> roles, Map<String, Object> extraClaims, String secret, long ttlSeconds) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(ttlSeconds);

        var builder = Jwts.builder()
                .subject(subject)
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .claim("roles", roles);

        if (extraClaims != null) {
            extraClaims.forEach(builder::claim);
        }

        return builder.signWith((javax.crypto.SecretKey) key(secret)).compact();
    }
}
