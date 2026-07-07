package com.example.bookexchange.security.config;

import com.example.bookexchange.common.api.MetadataPaths;
import com.example.bookexchange.common.config.AppProperties;
import com.example.bookexchange.common.demoaccess.DemoAccessFilter;
import com.example.bookexchange.common.demoaccess.DemoAccessPaths;
import com.example.bookexchange.common.demoaccess.DemoOriginGuardFilter;
import com.example.bookexchange.common.demoaccounts.DemoAccountsPaths;
import com.example.bookexchange.common.demoemail.DemoEmailSandboxPaths;
import com.example.bookexchange.common.demoemail.DemoEmailSandboxService;
import com.example.bookexchange.common.demoreset.DemoMaintenanceFilter;
import com.example.bookexchange.admin.api.AdminPaths;
import com.example.bookexchange.auth.api.AuthPaths;
import com.example.bookexchange.book.api.BookPaths;
import com.example.bookexchange.security.auth.JwtAuthenticationEntryPoint;
import com.example.bookexchange.security.filter.JwtFilter;
import com.example.bookexchange.security.filter.RateLimitFilter;
import com.example.bookexchange.security.filter.RequestIdFilter;
import com.example.bookexchange.security.handler.JwtAccessDeniedHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtFilter jwtFilter;
    private final DemoMaintenanceFilter demoMaintenanceFilter;
    private final DemoOriginGuardFilter demoOriginGuardFilter;
    private final DemoAccessFilter demoAccessFilter;
    private final RateLimitFilter rateLimitFilter;
    private final RequestIdFilter requestIdFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final JwtAccessDeniedHandler jwtAccessDeniedHandler;
    private final AppProperties appProperties;

    @Bean
    @Order(0)
    public SecurityFilterChain actuatorFilterChain(HttpSecurity http) {
        return http
                .securityMatcher(pathMatcher("/actuator/**", withApiBasePath("/actuator/**")))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(pathMatcher(
                                "/actuator/health",
                                "/actuator/health/**",
                                withApiBasePath("/actuator/health"),
                                withApiBasePath("/actuator/health/**")
                        )).permitAll()
                        .anyRequest().denyAll()
                )
                .build();
    }

    @Bean
    @Order(1)
    public SecurityFilterChain filterChain(HttpSecurity http) {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(pathMatcher("/error", withApiBasePath("/error"))).permitAll()
                        .requestMatchers(pathMatcher(
                                "/actuator/health",
                                "/actuator/health/**",
                                withApiBasePath("/actuator/health"),
                                withApiBasePath("/actuator/health/**")
                        )).permitAll()
                        .requestMatchers(pathMatcher("/actuator/**", withApiBasePath("/actuator/**"))).denyAll()
                        .requestMatchers(DemoAccessPaths.DEMO_ACCESS_VERIFY_PATH).permitAll()
                        .requestMatchers(AuthPaths.AUTH_PATH + "/**").permitAll()
                        .requestMatchers(AdminPaths.ADMIN_PATH + "/**").hasRole("ADMIN")
                        .requestMatchers(BookPaths.BOOK_PATH_USER, BookPaths.BOOK_PATH_USER + "/**").authenticated()
                        .requestMatchers(BookPaths.BOOK_PATH_HISTORY).authenticated()
                        .requestMatchers(HttpMethod.GET, BookPaths.BOOK_PATH_SEARCH).permitAll()
                        .requestMatchers(HttpMethod.GET, BookPaths.BOOK_PATH_BOOK_ID).permitAll()
                        .requestMatchers(MetadataPaths.METADATA_PATH, MetadataPaths.METADATA_PATH + "/**").permitAll()
                        .requestMatchers(DemoAccountsPaths.DEMO_ACCOUNTS_PATH).permitAll()
                        .requestMatchers(DemoEmailSandboxPaths.DEMO_EMAIL_SANDBOX_PATH + "/**").permitAll()
                        .requestMatchers(
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(requestIdFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(demoMaintenanceFilter, RequestIdFilter.class)
                .addFilterAfter(demoOriginGuardFilter, DemoMaintenanceFilter.class)
                .addFilterAfter(demoAccessFilter, DemoOriginGuardFilter.class)
                .addFilterAfter(jwtFilter, DemoAccessFilter.class)
                .addFilterAfter(rateLimitFilter, JwtFilter.class)
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                        .accessDeniedHandler(jwtAccessDeniedHandler)
                )
                .build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(appProperties.getCorsAllowedOrigins());
        configuration.setAllowedMethods(List.of("GET", "POST", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of(
                "Authorization",
                "Content-Type",
                "If-Match",
                "Accept-Language",
                "X-Request-Id",
                DemoEmailSandboxService.REQUEST_HEADER_NAME
        ));
        configuration.setExposedHeaders(List.of("ETag", "X-Request-Id", DemoEmailSandboxService.REQUEST_HEADER_NAME));
        configuration.setAllowCredentials(false);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    private String withApiBasePath(String path) {
        String basePath = appProperties.getBaseApiPath();

        if (basePath == null || basePath.isBlank() || "/".equals(basePath)) {
            return path;
        }

        String normalizedBasePath = basePath.startsWith("/") ? basePath : "/" + basePath;

        if (normalizedBasePath.endsWith("/")) {
            normalizedBasePath = normalizedBasePath.substring(0, normalizedBasePath.length() - 1);
        }

        return normalizedBasePath + path;
    }

    private RequestMatcher pathMatcher(String... patterns) {
        return request -> {
            String path = request.getRequestURI();
            String contextPath = request.getContextPath();

            if (contextPath != null && !contextPath.isBlank() && path.startsWith(contextPath)) {
                path = path.substring(contextPath.length());
            }

            for (String pattern : patterns) {
                if (matchesPattern(path, pattern)) {
                    return true;
                }
            }

            return false;
        };
    }

    private boolean matchesPattern(String path, String pattern) {
        if (pattern.endsWith("/**")) {
            String prefix = pattern.substring(0, pattern.length() - 3);

            return path.equals(prefix) || path.startsWith(prefix + "/");
        }

        return path.equals(pattern);
    }
}
