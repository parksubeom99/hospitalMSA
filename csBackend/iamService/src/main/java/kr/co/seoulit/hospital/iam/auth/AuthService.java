package kr.co.seoulit.hospital.iam.auth;

import io.jsonwebtoken.Claims;
import kr.co.seoulit.hospital.iam.rbac.RbacService;
import kr.co.seoulit.hospital.iam.user.UserAccount;
import kr.co.seoulit.hospital.iam.user.UserAccountRepository;
import kr.co.seoulit.hospital.iam.user.UserRole;
import kr.co.seoulit.hospital.iam.user.UserRoleRepository;
import kr.co.seoulit.hospital.global.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserAccountRepository userRepo;
    private final UserRoleRepository userRoleRepo;
    private final RbacService rbacService;
    private final PasswordEncoder encoder;
    private final RefreshTokenStore refreshTokenStore;

    @Value("${his.jwt.secret:CHANGE_ME_LOCAL_SECRET_CHANGE_ME_LOCAL_SECRET}")
    private String jwtSecret;

    @Value("${his.jwt.access-ttl-seconds:28800}")
    private long accessTtlSeconds;

    @Value("${his.jwt.refresh-ttl-seconds:1209600}")
    private long refreshTtlSeconds;

    public record UserView(String username, String displayName, String role, String staffId) {}
    public record TokenBundle(String accessToken, String refreshToken, String tokenType) {}
    public record LoginResult(TokenBundle token, UserView user, String loginId, String staffId, List<String> roles, List<String> permissions) {}

    public enum FailReason { USER_NOT_FOUND, INACTIVE, BAD_PASSWORD, INVALID_REFRESH_TOKEN }

    public static class AuthFailedException extends RuntimeException {
        private final FailReason reason;
        public AuthFailedException(FailReason reason) {
            super("AUTH_FAILED");
            this.reason = reason;
        }
        public FailReason getReason() { return reason; }
    }

    public LoginResult login(String loginId, String password) {
        UserAccount user = userRepo.findByLoginId(loginId)
                .orElseThrow(() -> new AuthFailedException(FailReason.USER_NOT_FOUND));

        if (!user.isActive()) throw new AuthFailedException(FailReason.INACTIVE);
        if (!encoder.matches(password, user.getPasswordHash())) throw new AuthFailedException(FailReason.BAD_PASSWORD);

        return issueTokensForUser(user);
    }

    public LoginResult refresh(String refreshToken) {
        Claims claims;
        try {
            claims = JwtUtil.parseClaims(refreshToken, jwtSecret);
        } catch (Exception e) {
            throw new AuthFailedException(FailReason.INVALID_REFRESH_TOKEN);
        }

        String tokenType = String.valueOf(claims.get("tokenType"));
        if (!"REFRESH".equals(tokenType)) {
            throw new AuthFailedException(FailReason.INVALID_REFRESH_TOKEN);
        }

        String loginId = claims.getSubject();
        if (!refreshTokenStore.matches(loginId, refreshToken)) {
            throw new AuthFailedException(FailReason.INVALID_REFRESH_TOKEN);
        }

        UserAccount user = userRepo.findByLoginId(loginId)
                .orElseThrow(() -> new AuthFailedException(FailReason.USER_NOT_FOUND));
        if (!user.isActive()) throw new AuthFailedException(FailReason.INACTIVE);

        return issueTokensForUser(user);
    }

    public void logoutByRefreshToken(String refreshToken) {
        try {
            Claims claims = JwtUtil.parseClaims(refreshToken, jwtSecret);
            String tokenType = String.valueOf(claims.get("tokenType"));
            if (!"REFRESH".equals(tokenType)) return;
            refreshTokenStore.delete(claims.getSubject());
        } catch (Exception ignore) {
        }
    }

    private LoginResult issueTokensForUser(UserAccount user) {
        List<String> roles = userRoleRepo.findByUserId(user.getUserId()).stream()
                .map(UserRole::getRoleCode)
                .distinct()
                .toList();

        List<String> perms = rbacService.getPermissionsByRoles(roles);
        String primaryRole = roles.isEmpty() ? "USER" : roles.get(0);

        String accessToken = JwtUtil.createToken(
                user.getLoginId(),
                roles,
                Map.of(
                        "staffId", user.getStaffId(),
                        "scope", "ALL_PATIENTS",
                        "perms", perms,
                        "tokenType", "ACCESS"
                ),
                jwtSecret,
                accessTtlSeconds
        );

        String refreshToken = JwtUtil.createToken(
                user.getLoginId(),
                roles,
                Map.of(
                        "staffId", user.getStaffId(),
                        "tokenType", "REFRESH"
                ),
                jwtSecret,
                refreshTtlSeconds
        );

        refreshTokenStore.save(user.getLoginId(), refreshToken, refreshTtlSeconds);

        TokenBundle tokenBundle = new TokenBundle(accessToken, refreshToken, "Bearer");
        UserView userView = new UserView(user.getLoginId(), user.getLoginId(), primaryRole, user.getStaffId());
        return new LoginResult(tokenBundle, userView, user.getLoginId(), user.getStaffId(), roles, perms);
    }
}
