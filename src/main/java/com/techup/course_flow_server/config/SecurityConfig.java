package com.techup.course_flow_server.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration(proxyBeanMethods = false)
@EnableWebSecurity
public class SecurityConfig {

    private final SupabaseProperties supabaseProperties;
    private final JwtUserRoleAuthenticationConverter jwtUserRoleAuthenticationConverter;

    public SecurityConfig(
            SupabaseProperties supabaseProperties,
            JwtUserRoleAuthenticationConverter jwtUserRoleAuthenticationConverter) {
        this.supabaseProperties = supabaseProperties;
        this.jwtUserRoleAuthenticationConverter = jwtUserRoleAuthenticationConverter;
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        String jwksUrl = supabaseProperties.url() + "/auth/v1/.well-known/jwks.json";
        return NimbusJwtDecoder
            .withJwkSetUri(jwksUrl)
            .jwsAlgorithm(SignatureAlgorithm.ES256)
            .build();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> {})
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**", "/api/webhooks/**").permitAll()
                // Public catalog only — do NOT use /api/courses/** (would expose /learning & /assignments without JWT
                // and cause NPE on jwt.getSubject() → 500).
                .requestMatchers(HttpMethod.GET,
                        "/api/courses",
                        "/api/courses/published",
                        "/api/courses/search",
                        "/api/courses/category/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/courses/*/modules-with-materials").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/courses/*/materials").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/courses/*").permitAll()
                // Title uniqueness check for admin create form (read-only; create still requires ADMIN)
                .requestMatchers(HttpMethod.GET, "/api/admin/courses/exists").permitAll()
                .requestMatchers("/api/admin/**").hasAuthority("ROLE_ADMIN")
                .anyRequest().authenticated())
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt
                .decoder(jwtDecoder())
                .jwtAuthenticationConverter(jwtUserRoleAuthenticationConverter)));
        return http.build();
    }
}