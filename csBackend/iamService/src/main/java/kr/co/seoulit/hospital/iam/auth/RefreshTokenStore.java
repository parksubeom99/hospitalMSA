package kr.co.seoulit.hospital.iam.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class RefreshTokenStore {

    private final StringRedisTemplate redisTemplate;

    @Value("${his.jwt.refresh.prefix:iam:refresh:}")
    private String keyPrefix;

    public void save(String loginId, String refreshToken, long ttlSeconds) {
        redisTemplate.opsForValue().set(key(loginId), refreshToken, Duration.ofSeconds(ttlSeconds));
    }

    public Optional<String> get(String loginId) {
        return Optional.ofNullable(redisTemplate.opsForValue().get(key(loginId)));
    }

    public void delete(String loginId) {
        redisTemplate.delete(key(loginId));
    }

    public boolean matches(String loginId, String refreshToken) {
        return get(loginId).map(refreshToken::equals).orElse(false);
    }

    private String key(String loginId) {
        return keyPrefix + loginId;
    }
}
