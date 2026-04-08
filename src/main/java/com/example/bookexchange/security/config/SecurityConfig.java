package com.example.bookexchange.security.config;

import com.example.bookexchange.common.api.MetadataPaths;
import com.example.bookexchange.common.config.AppProperties;
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
    private final RateLimitFilter rateLimitFilter;
    private final RequestIdFilter requestIdFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final JwtAccessDeniedHandler jwtAccessDeniedHandler;
    private final AppProperties appProperties;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(AuthPaths.AUTH_PATH + "/**").permitAll()
                        .requestMatchers(AdminPaths.ADMIN_PATH + "/**").hasRole("ADMIN")
                        .requestMatchers(BookPaths.BOOK_PATH_USER, BookPaths.BOOK_PATH_USER + "/**").authenticated()
                        .requestMatchers(BookPaths.BOOK_PATH_HISTORY).authenticated()
                        .requestMatchers(HttpMethod.GET, BookPaths.BOOK_PATH_SEARCH).permitAll()
                        .requestMatchers(HttpMethod.GET, BookPaths.BOOK_PATH_BOOK_ID).permitAll()
                        .requestMatchers(MetadataPaths.METADATA_PATH).permitAll()
                        .requestMatchers(
                                "/actuator/health",
                                "/actuator/info",
                                "/actuator/prometheus"
                        ).permitAll()
                        .requestMatchers(
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(requestIdFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(jwtFilter, RequestIdFilter.class)
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
                "X-Request-Id"
        ));
        configuration.setExposedHeaders(List.of("ETag", "X-Request-Id"));
        configuration.setAllowCredentials(false);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
