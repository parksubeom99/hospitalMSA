package kr.co.seoulit.his.admin.global.common;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * 현업형 운영 품질: 모든 요청/응답에 CorrelationId를 부여하고 로그/MDC에 연결한다.
 * - 표준 헤더: X-Correlation-Id
 * - 하위 호환: X-Trace-Id
 */
@Component
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String HDR_CORR = "X-Correlation-Id";
    public static final String HDR_TRACE = "X-Trace-Id";
    public static final String MDC_KEY = "correlationId";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String corr = request.getHeader(HDR_CORR);
        if (corr == null || corr.isBlank()) {
            String trace = request.getHeader(HDR_TRACE);
            corr = (trace != null && !trace.isBlank()) ? trace : UUID.randomUUID().toString();
        }

        MDC.put(MDC_KEY, corr);
        request.setAttribute(MDC_KEY, corr);

        response.setHeader(HDR_CORR, corr);
        response.setHeader(HDR_TRACE, corr); // 하위 호환

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_KEY);
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/actuator")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/swagger")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/health");
    }
}
