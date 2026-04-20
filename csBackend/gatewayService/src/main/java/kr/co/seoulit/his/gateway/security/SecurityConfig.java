package kr.co.seoulit.his.gateway.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

// [ADDED] @EnableWebFluxSecurity — WebFlux 환경에서 Security 명시적 활성화
@EnableWebFluxSecurity
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(
            ServerHttpSecurity http,
            JwtPreAuthWebFilter jwtPreAuthWebFilter
    ) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .logout(ServerHttpSecurity.LogoutSpec::disable)
                // [FIXED 2026-04-16] .cors() 비활성화
                // 원인: Spring Security의 cors(withDefaults())는 CorsConfigurationSource Bean을
                //       찾으려 하는데, Spring Cloud Gateway는 globalcors 설정(application.yml)에서
                //       자체 CORS 필터를 생성함. 두 시스템이 충돌하여 OPTIONS가 403 처리됨.
                // 해결: Security의 CORS는 disable -> Gateway globalcors 단독 처리
                .cors(ServerHttpSecurity.CorsSpec::disable)
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .pathMatchers("/actuator/health", "/actuator/info").permitAll()
                        .pathMatchers("/api/auth/**", "/api/audit/**").permitAll()
                        .anyExchange().permitAll()
                )
                .addFilterAt(jwtPreAuthWebFilter, SecurityWebFiltersOrder.AUTHENTICATION)
                .build();
    }
}
