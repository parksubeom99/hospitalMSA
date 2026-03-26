package kr.co.seoulit.his.admin.global.common;

import java.time.LocalDateTime;

/**
 * 공통 응답 Envelope(현업형)
 */
public record ApiResponse<T>(
        boolean success,
        T data,
        ApiError error,
        String traceId,
        LocalDateTime timestamp
) {
    public static <T> ApiResponse<T> ok(T data, String traceId) {
        return new ApiResponse<>(true, data, null, traceId, LocalDateTime.now());
    }

    public static <T> ApiResponse<T> error(ApiError error, String traceId) {
        return new ApiResponse<>(false, null, error, traceId, LocalDateTime.now());
    }
}
