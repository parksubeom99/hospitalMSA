package kr.co.seoulit.his.clinical.global.response;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * NOTE: JwtAuthFilter에서 principal(subject)=loginId 로 세팅합니다.
 */
public final class CurrentUserUtil {

    private CurrentUserUtil() {}

    public static String currentLoginIdOrNull() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return null;
        Object principal = auth.getPrincipal();
        if (principal == null) return null;
        String s = String.valueOf(principal);
        return (s.isBlank() || "anonymousUser".equalsIgnoreCase(s)) ? null : s;
    }
}
