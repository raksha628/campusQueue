package com.campusqueue.security;

import com.campusqueue.dto.response.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Spring Security configuration establishing HTTP session authentication,
 * BCrypt password hashing, and role-based URL access controls.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final ObjectMapper objectMapper;

    public SecurityConfig(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint())
                        .accessDeniedHandler(accessDeniedHandler())
                )
                .authorizeHttpRequests(auth -> auth
                        // 1. Public Endpoints
                        .requestMatchers(HttpMethod.GET, "/api/health").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/login", "/api/auth/logout").permitAll()

                        // 2. Authentication Profile Check
                        .requestMatchers(HttpMethod.GET, "/api/auth/me").authenticated()

                        // 3. User Management
                        .requestMatchers(HttpMethod.POST, "/api/users").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/users").hasAnyRole("STAFF", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/users/{id}").authenticated()

                        // 4. Counter Management (ADMIN only for mutating desks)
                        .requestMatchers(HttpMethod.POST, "/api/counters").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/counters/{id}/toggle-status").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/counters/**").authenticated()

                        // 5. Staff Queue Lifecycle Operations (STAFF or ADMIN only)
                        .requestMatchers(HttpMethod.POST, "/api/tickets/counter/{counterId}/call-next").hasAnyRole("STAFF", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/tickets/{id}/call").hasAnyRole("STAFF", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/tickets/{id}/complete").hasAnyRole("STAFF", "ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/tickets/{id}/complete").hasAnyRole("STAFF", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/tickets/{id}/skip").hasAnyRole("STAFF", "ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/tickets/{id}/skip").hasAnyRole("STAFF", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/tickets/counter/{counterId}").hasAnyRole("STAFF", "ADMIN")

                        // 6. Analytics (STAFF or ADMIN only)
                        .requestMatchers("/api/analytics/**").hasAnyRole("STAFF", "ADMIN")

                        // 7. Student & General Ticket Operations (Authenticated + Service Ownership Checks)
                        .requestMatchers(HttpMethod.POST, "/api/tickets").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/tickets/{id}").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/tickets/{id}/cancel").authenticated()
                        .requestMatchers(HttpMethod.PATCH, "/api/tickets/{id}/cancel").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/tickets/counter/{counterId}/status").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/tickets/counter/{counterId}/waiting").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/tickets/counter/{counterId}/current").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/tickets/user/{userId}").authenticated()

                        // 8. Default: Protect all other endpoints
                        .anyRequest().authenticated()
                );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(
                "http://localhost:5173",
                "http://localhost:3000",
                "http://127.0.0.1:5173",
                "http://127.0.0.1:3000"
        ));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, authException) -> {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            ErrorResponse error = new ErrorResponse(
                    HttpStatus.UNAUTHORIZED.value(),
                    "UNAUTHORIZED",
                    "Authentication required. Please log in.",
                    request.getRequestURI()
            );
            response.getWriter().write(objectMapper.writeValueAsString(error));
        };
    }

    @Bean
    public AccessDeniedHandler accessDeniedHandler() {
        return (request, response, accessDeniedException) -> {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            ErrorResponse error = new ErrorResponse(
                    HttpStatus.FORBIDDEN.value(),
                    "FORBIDDEN",
                    accessDeniedException.getMessage() != null && !accessDeniedException.getMessage().isBlank()
                            ? accessDeniedException.getMessage()
                            : "Access denied: Insufficient permissions.",
                    request.getRequestURI()
            );
            response.getWriter().write(objectMapper.writeValueAsString(error));
        };
    }
}
