package kr.co.seoulit.hospital.global.security;

import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

public class KeysUtil {
    public static SecretKey hmac(String secret) {
        String s = (secret == null || secret.isBlank()) ? JwtUtil.DEFAULT_SECRET : secret;
        return Keys.hmacShaKeyFor(s.getBytes(StandardCharsets.UTF_8));
    }
}
