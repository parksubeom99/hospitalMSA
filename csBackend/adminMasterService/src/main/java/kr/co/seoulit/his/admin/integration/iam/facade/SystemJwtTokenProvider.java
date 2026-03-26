package kr.co.seoulit.his.admin.integration.iam.facade;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;

@Component
public class SystemJwtTokenProvider {

    private final byte[] secretBytes;

    public SystemJwtTokenProvider(@Value("${his.jwt.secret}") String secret) {
        this.secretBytes = secret.getBytes(StandardCharsets.UTF_8);
    }

    public String issueSystemToken() {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(120);

        return Jwts.builder()
                .subject("system")
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .claim("roles", List.of("SYS"))
                .signWith(Keys.hmacShaKeyFor(secretBytes))
                .compact();
    }
}
