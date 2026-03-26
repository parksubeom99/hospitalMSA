package kr.co.seoulit.his.admin.global.common;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * 성공 응답도 공통 Envelope로 감싼다(프론트는 data만 언랩).
 * - health/swagger 등은 제외
 * - String 반환은 converter 특성상 제외
 */
@org.springframework.web.bind.annotation.ControllerAdvice(annotations = {RestController.class, Controller.class})
@RequiredArgsConstructor
public class ApiResponseAdvice implements ResponseBodyAdvice<Object> {

    private final HttpServletRequest httpReq;

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  org.springframework.http.server.ServerHttpRequest request,
                                  org.springframework.http.server.ServerHttpResponse response) {

        String path = request.getURI().getPath();
        if (path == null) path = "";
        if (path.startsWith("/actuator") || path.startsWith("/v3/api-docs") || path.startsWith("/swagger") || path.startsWith("/swagger-ui") || path.startsWith("/health")) {
            return body;
        }

        if (body instanceof ApiResponse<?>) return body;
        if (body instanceof String) return body;

        String traceId = "";
        Object attr = httpReq.getAttribute(CorrelationIdFilter.MDC_KEY);
        if (attr != null) traceId = String.valueOf(attr);
        if (traceId.isBlank()) {
            String hdr = httpReq.getHeader(CorrelationIdFilter.HDR_CORR);
            if (hdr != null && !hdr.isBlank()) traceId = hdr;
        }
        if (traceId.isBlank()) {
            String hdr = httpReq.getHeader(CorrelationIdFilter.HDR_TRACE);
            if (hdr != null && !hdr.isBlank()) traceId = hdr;
        }

        return ApiResponse.ok(body, traceId);
    }
}
