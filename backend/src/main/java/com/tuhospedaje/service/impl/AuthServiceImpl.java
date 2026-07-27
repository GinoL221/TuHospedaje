package com.tuhospedaje.service.impl;

import com.tuhospedaje.configuration.JwtService;
import com.tuhospedaje.dto.auth.AuthResponse;
import com.tuhospedaje.dto.auth.LoginRequest;
import com.tuhospedaje.dto.auth.RegisterRequest;
import com.tuhospedaje.entity.User;
import com.tuhospedaje.enums.RoleEnum;
import com.tuhospedaje.repository.UserRepository;
import com.tuhospedaje.service.AuthService;
import com.tuhospedaje.service.EmailService;
import com.tuhospedaje.service.RefreshSessionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.HashMap;
import java.util.Map;

@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final EmailService emailService;
    // ObjectProvider, NOT a hard constructor dependency (Design ADR-0): RefreshSessionService
    // has no bean at all when app.session.refresh.enabled=false (RefreshSessionConfiguration
    // is @ConditionalOnProperty). A hard dependency here would break ApplicationContext
    // startup with the flag off, defeating the documented rollback/kill-switch.
    private final ObjectProvider<RefreshSessionService> refreshSessions;

    public AuthServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService,
            AuthenticationManager authenticationManager, EmailService emailService,
            ObjectProvider<RefreshSessionService> refreshSessions) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.emailService = emailService;
        this.refreshSessions = refreshSessions;
    }

    @Override
    @Transactional
    public AuthResult register(RegisterRequest request) {
        if (userRepository. findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalArgumentException("El email ya está registrado");
        }

        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(RoleEnum.USER)
                .imageUrl("https://ui-avatars.com/api/?name=" + request.getFirstName() + "+" + request.getLastName())
                .build();

        userRepository.save(user);

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    emailService.sendWelcomeEmail(request);
                } catch (RuntimeException ex) {
                    log.warn("auth.register.welcome_email_failed email={}", request.getEmail());
                }
            }
        });

        return buildAuthResult(user);
    }

    @Override
    // NOT readOnly: buildAuthResult() below now issues a refresh session (a write) when
    // refresh sessions are enabled, and that write must join THIS transaction (same
    // managed User, same boundary the design requires for issue(User) — see class-level
    // note on buildAuthResult). A readOnly outer transaction would propagate readOnly to
    // that nested write (MariaDB honors Connection#setReadOnly), silently breaking login.
    @Transactional
    public AuthResult login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(), request.getPassword()));

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Credenciales inválidas"));

        return buildAuthResult(user);
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse currentUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        return buildAuthResponse(user);
    }

    @Override
    // noRollbackFor mirrors RefreshSessionServiceImpl.rotate()'s own rule: this method is the
    // OWNING transaction reached from the HTTP request (AuthController.refresh() has none of
    // its own), so rotate()'s noRollbackFor is inert on its own — only the owning transaction's
    // rollback rule decides the physical outcome. Without this, a detected reuse's family
    // revocation (written inside rotate() before it throws) gets silently rolled back here,
    // leaving the compromised token's rotated successor valid despite the 401 response.
    @Transactional(noRollbackFor = RefreshSessionService.Rejected.class)
    public AuthResult refresh(String refreshCredential) {
        // Absent bean (flag off) maps to the SAME Rejected/401 as any other invalid
        // refresh attempt (Design ADR-0 + non-disclosing error contract) — no distinct
        // status/body that would let a caller tell "disabled" apart from "expired".
        RefreshSessionService sessions = refreshSessions.getIfAvailable();
        if (sessions == null) {
            throw new RefreshSessionService.Rejected();
        }
        RefreshSessionService.Session session = sessions.rotate(refreshCredential);
        User user = userRepository.findById(session.userId())
                .orElseThrow(RefreshSessionService.Rejected::new);
        String token = jwtService.generateToken(claimsFor(user), user);
        return new AuthResult(buildAuthResponse(user), token, session.refreshCredential());
    }

    @Override
    @Transactional
    public void changePassword(String email, String currentPassword, String newPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new IllegalArgumentException("La contraseña actual es incorrecta");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // Best-effort, ADR-0: RefreshSessionService has no bean when the flag is off, so
        // this simply no-ops rather than fail the password change itself.
        RefreshSessionService sessions = refreshSessions.getIfAvailable();
        if (sessions != null) {
            sessions.revokeAll(user.getId(), "PASSWORD_CHANGE");
        }
    }

    @Override
    @Transactional
    public void logout(String refreshCredential) {
        if (refreshCredential == null) {
            return;
        }
        RefreshSessionService sessions = refreshSessions.getIfAvailable();
        if (sessions == null) {
            return;
        }
        try {
            sessions.revokeCurrent(refreshCredential);
        } catch (RefreshSessionService.Rejected ex) {
            // Already-consumed/unknown/reused credential — logout stays idempotent/204
            // regardless (Design PR3/WU4), so this is deliberately swallowed rather than
            // propagated.
        }
    }

    private AuthResult buildAuthResult(User user) {
        String token = jwtService.generateToken(claimsFor(user), user);
        String refreshCredential = issueRefreshCredential(user);
        return new AuthResult(buildAuthResponse(user), token, refreshCredential);
    }

    private String issueRefreshCredential(User user) {
        RefreshSessionService sessions = refreshSessions.getIfAvailable();
        if (sessions == null) {
            return null;
        }
        // Refresh-session issuance is a best-effort enhancement, not a login precondition:
        // a transient failure here (DB deadlock, pool exhaustion) must degrade to an
        // access-token-only session rather than fail login/register outright.
        try {
            return sessions.issue(user).refreshCredential();
        } catch (RuntimeException ex) {
            log.warn("event=refresh_session.issue_failed user_id={}", user.getId(), ex);
            return null;
        }
    }

    private Map<String, Object> claimsFor(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("firstName", user.getFirstName());
        claims.put("lastName", user.getLastName());
        claims.put("role", user.getRole().name());
        claims.put("imageUrl", user.getImageUrl());
        return claims;
    }

    private AuthResponse buildAuthResponse(User user) {
        return AuthResponse.builder()
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .role(user.getRole().name())
                .imageUrl(user.getImageUrl())
                .build();
    }
}
