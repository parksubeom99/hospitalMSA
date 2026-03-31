package kr.co.seoulit.his.gateway.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

import static org.springframework.security.config.Customizer.withDefaults;

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
                // [ADDED] .cors(withDefaults()) — Spring Security가 Gateway globalcors 설정을 위임받아 CORS 헤더 추가
                // 없으면 application.yml globalcors 설정이 Security 레이어에서 무시됨
                // → OPTIONS preflight에 Access-Control-Allow-Origin 헤더 미포함 → CORS 오류
                .cors(withDefaults())
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