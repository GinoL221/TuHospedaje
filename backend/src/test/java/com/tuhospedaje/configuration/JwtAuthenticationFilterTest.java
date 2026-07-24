package com.tuhospedaje.configuration;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * ADR-6 (Design, issue #55): a disabled user's still-valid, unexpired JWT must stop
 * authenticating on the very next request (admin-disable / password-change cutoff).
 * {@link JwtAuthenticationFilter} already loads {@link UserDetails} to validate the JWT
 * subject, so the enabled check reuses it with zero extra DB round-trip.
 */
@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    private static final String JWT = "structurally.valid.jwt";

    @Mock private JwtService jwtService;
    @Mock private UserDetailsService userDetailsService;
    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;
    @Mock private FilterChain filterChain;
    @Mock private UserDetails userDetails;

    private JwtAuthenticationFilter filter;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void doesNotAuthenticateWhenUserDetailsAreDisabledDespiteAStructurallyValidJwt() throws Exception {
        filter = new JwtAuthenticationFilter(jwtService, userDetailsService);
        when(request.getCookies()).thenReturn(new Cookie[]{new Cookie("ACCESS_TOKEN", JWT)});
        when(jwtService.extractUsername(JWT)).thenReturn("disabled-user@test.com");
        when(userDetailsService.loadUserByUsername("disabled-user@test.com")).thenReturn(userDetails);
        when(jwtService.isTokenValid(JWT, userDetails)).thenReturn(true);
        when(userDetails.isEnabled()).thenReturn(false);

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void authenticatesWhenUserDetailsAreEnabledAndJwtIsValid() throws Exception {
        filter = new JwtAuthenticationFilter(jwtService, userDetailsService);
        when(request.getCookies()).thenReturn(new Cookie[]{new Cookie("ACCESS_TOKEN", JWT)});
        when(jwtService.extractUsername(JWT)).thenReturn("enabled-user@test.com");
        when(userDetailsService.loadUserByUsername("enabled-user@test.com")).thenReturn(userDetails);
        when(jwtService.isTokenValid(JWT, userDetails)).thenReturn(true);
        when(userDetails.isEnabled()).thenReturn(true);
        Collection<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
        doReturn(authorities).when(userDetails).getAuthorities();

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        verify(filterChain).doFilter(request, response);
    }
}
