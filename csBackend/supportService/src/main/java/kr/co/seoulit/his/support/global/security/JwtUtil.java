package kr.co.seoulit.his.support.global.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import java.nio.charset.StandardCharsets;
import java.security.Key;

public class JwtUtil {
    public static final String DEFAULT_SECRET = "CHANGE_ME_LOCAL_SECRET_CHANGE_ME_LOCAL_SECRET";

    public static Key key(String secret) {
        String s = (secret == null || secret.isBlank()) ? DEFAULT_SECRET : secret;
        return Keys.hmacShaKeyFor(s.getBytes(StandardCharsets.UTF_8));
    }

    public static Claims parse(String token, String secret) {
        return Jwts.parser()
                .verifyWith((javax.crypto.SecretKey) key(secret))
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
