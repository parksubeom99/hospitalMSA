package kr.co.seoulit.his.support.global.common;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * JWT 필터에서 principal=loginId 로 세팅되므로, 요청 처리 중 현재 사용자의 loginId 를 안전하게 얻습니다.
 */
public final class CurrentUserUtil {

    private CurrentUserUtil() {}

    public static String loginIdOrSystem() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) {
            return "SYSTEM";
        }
        String principal = String.valueOf(auth.getPrincipal());
        return (principal.isBlank() ? "SYSTEM" : principal);
    }
}
