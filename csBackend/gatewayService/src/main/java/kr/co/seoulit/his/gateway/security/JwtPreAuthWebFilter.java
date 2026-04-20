package kr.co.seoulit.his.gateway.security;

import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.PathContainer;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
public class JwtPreAuthWebFilter implements WebFilter {

    private final String jwtSecret;

    private final List<String> protectedPrefixes = List.of(
            "/api/admin/",
            "/api/master/",
            "/api/emr/",
            "/api/orders/",
            "/api/final-orders/",
            "/api/encounters/",
            "/api/results/",
            "/api/worklist/",
            "/api/lab-results/",
            "/api/radiology-reports/",
            "/api/procedure-reports/",
            "/api/injections/",
            "/api/med-execs/",
            "/api/endoscopy-reports/",
            "/api/support/"
    );

    public JwtPreAuthWebFilter(
            @Value("${his.jwt.secret:CHANGE_ME_LOCAL_SECRET_CHANGE_ME_LOCAL_SECRET}") String jwtSecret
    ) {
        this.jwtSecret = jwtSecret;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        // [ADDED] OPTIONS preflight는 JWT 검증 없이 즉시 통과
        // 이유: 브라우저 CORS preflight는 Authorization 헤더 없이 전송됨
        //       JWT 검증을 통과 못하면 401 반환 → CORS 헤더 없는 401 → 브라우저가 CORS 오류로 오해
        if (HttpMethod.OPTIONS.equals(exchange.getRequest().getMethod())) {
            return chain.filter(exchange);
        }

        String path = exchange.getRequest().getPath().pathWithinApplication().value();
        if (!requiresJwt(path)) {
            return chain.filter(exchange);
        }

        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return unauthorized(exchange, "Authorization Bearer 토큰이 필요합니다.");
        }

        String token = authHeader.substring(7);
        try {
            Claims claims = JwtUtil.parse(token, jwtSecret);
            String subject = String.valueOf(claims.getSubject());
            Object role = claims.get("role");

            ServerWebExchange mutatedExchange = exchange.mutate()
                    .request(builder -> builder.headers(headers -> {
                        headers.set("X-Auth-Subject", subject);
                        if (role != null) {
                            headers.set("X-Auth-Role", String.valueOf(role));
                        }
                    }))
                    .build();

            return chain.filter(mutatedExchange);
        } catch (Exception ex) {
            return unauthorized(exchange, "유효하지 않은 JWT 토큰입니다.");
        }
    }

    private boolean requiresJwt(String path) {
        if (path == null || path.isBlank()) {
            return false;
        }
        PathContainer container = PathContainer.parsePath(path);
        String normalized = container.value();
        return protectedPrefixes.stream().anyMatch(normalized::startsWith);
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        byte[] body = String.format("{\"message\":\"%s\"}", message).getBytes(StandardCharsets.UTF_8);
        // [ADDED] 401 응답에 CORS 헤더 강제 주입
        // 원인: unauthorized()가 response를 직접 작성하면 Spring Gateway globalcors 필터가
        //       개입하지 못해 Access-Control-Allow-Origin 헤더가 누락됨
        //       → 브라우저가 CORS 오류로 오인하여 실제 401 원인을 숨김
        // 해결: 401 응답에도 CORS 헤더를 직접 추가 → 브라우저가 실제 401 원인을 정확히 표시
        String origin = exchange.getRequest().getHeaders().getFirst(HttpHeaders.ORIGIN);
        if (origin != null) {
            exchange.getResponse().getHeaders().set("Access-Control-Allow-Origin", origin);
            exchange.getResponse().getHeaders().set("Access-Control-Allow-Credentials", "true");
        }
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        exchange.getResponse().getHeaders().setContentLength(body.length);
        return exchange.getResponse().writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(body)));
    }
}