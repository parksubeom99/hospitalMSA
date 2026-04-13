package kr.co.seoulit.his.gateway.security;

import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
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
        if (exchange.getRequest().getMethod().name().equals("OPTIONS")) {
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
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        exchange.getResponse().getHeaders().setContentLength(body.length);
        return exchange.getResponse().writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(body)));
    }
}
