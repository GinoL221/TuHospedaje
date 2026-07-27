package com.tuhospedaje.configuration;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.session.NullAuthenticatedSessionStrategy;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final CorsProperties corsProperties;

    @Bean
    public CsrfTokenRepository csrfTokenRepository() {
        return CookieCsrfTokenRepository.withHttpOnlyFalse();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf
                        .csrfTokenRepository(csrfTokenRepository())
                        .csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler())
                        // /api/auth/refresh is CSRF-exempt for the same reason login/register are:
                        // it is an unauthenticated-context entry point (a valid session may have
                        // just expired) whose real credential is the httpOnly REFRESH_TOKEN cookie
                        // itself, not the CSRF-protected session. A cross-site request cannot read
                        // that httpOnly cookie to forge a call, and the credential only ever
                        // advances one generation per legitimate use — so exempting it does NOT
                        // reopen the PR #60 NullAuthenticatedSessionStrategy rotation race below;
                        // see AuthCsrfLifecycleIntegrationTest's regression coverage.
                        .ignoringRequestMatchers("/api/auth/login", "/api/auth/register", "/api/auth/refresh")
                        // Without this, CsrfConfigurer defaults to a CsrfAuthenticationStrategy that
                        // clears and regenerates the XSRF-TOKEN cookie every time SessionManagementFilter
                        // sees a newly-authenticated request. That guard exists to prevent session
                        // fixation, but this app is stateless (SessionCreationPolicy.STATELESS) — there is
                        // no HttpSession to remember "already handled", so SessionManagementFilter reruns
                        // it on every single authenticated request, not just at login. That rotation races
                        // against concurrent requests firing right after login (e.g. the SPA's parallel
                        // categories/lodgings/favorites calls on the home page), so whichever rotation
                        // lands last in the browser can leave the cookie out of sync with the
                        // X-XSRF-TOKEN header the frontend already read, causing sporadic 403s on the next
                        // unsafe request (logout). A null strategy is correct here: there is no session to
                        // fixate against in a stateless app.
                        .sessionAuthenticationStrategy(new NullAuthenticatedSessionStrategy())
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/api/auth/login", "/api/auth/register", "/api/auth/logout", "/api/auth/refresh").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/auth/csrf").authenticated()
                        .requestMatchers("/api/auth/me").authenticated()
                        // Unlike /api/auth/refresh, this call has an established session and stays
                        // CSRF-protected — it is NOT in the ignoringRequestMatchers list above.
                        .requestMatchers("/api/auth/password").authenticated()
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/webjars/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/categories/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/features/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/policies/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/lodgings/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/ratings/**").permitAll()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(exceptions -> exceptions
                        // /api/reservations/** and /api/auth/me return 401 for unauthenticated
                        // requests (Spec Requirement 7: /me must 401, not the generic 403).
                        // All other protected endpoints return 403 to match test expectations.
                        .defaultAuthenticationEntryPointFor(
                                new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                                PathPatternRequestMatcher.withDefaults().matcher("/api/reservations/**")
                        )
                        .defaultAuthenticationEntryPointFor(
                                new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                                PathPatternRequestMatcher.withDefaults().matcher("/api/auth/me")
                        )
                        .defaultAuthenticationEntryPointFor(
                                new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                                PathPatternRequestMatcher.withDefaults().matcher("/api/auth/csrf")
                        )
                        .defaultAuthenticationEntryPointFor(
                                new HttpStatusEntryPoint(HttpStatus.FORBIDDEN),
                                PathPatternRequestMatcher.withDefaults().matcher("/**")
                        )
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()));
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(corsProperties.allowedOrigins());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
