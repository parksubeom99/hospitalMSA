package kr.co.seoulit.his.admin.global.common;

import java.time.LocalDateTime;

/**
 * 공통 에러 포맷(현업형): 서비스마다 동일한 구조로 에러를 반환한다.
 */
public record ApiError(
        String code,
        String message,
        String path,
        LocalDateTime timestamp
) {
}
