package com.tuhospedaje.service.impl;

import com.tuhospedaje.configuration.SessionProperties;
import com.tuhospedaje.entity.RefreshToken;
import com.tuhospedaje.entity.RefreshTokenFamily;
import com.tuhospedaje.entity.SessionSecurityEvent;
import com.tuhospedaje.entity.User;
import com.tuhospedaje.repository.RefreshTokenFamilyRepository;
import com.tuhospedaje.repository.RefreshTokenRepository;
import com.tuhospedaje.repository.SessionSecurityEventRepository;
import com.tuhospedaje.security.RefreshTokenHasher;
import com.tuhospedaje.service.RefreshSessionService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.function.Supplier;

public class RefreshSessionServiceImpl implements RefreshSessionService {
    private static final Logger log = LoggerFactory.getLogger(RefreshSessionServiceImpl.class);

    private final RefreshTokenFamilyRepository families;
    private final RefreshTokenRepository tokens;
    private final SessionSecurityEventRepository events;
    private final RefreshTokenHasher hasher;
    private final SessionProperties properties;
    private final Supplier<Clock> clock;
    private final EntityManager entityManager;

    public RefreshSessionServiceImpl(RefreshTokenFamilyRepository families, RefreshTokenRepository tokens,
                                     SessionSecurityEventRepository events, RefreshTokenHasher hasher,
                                     SessionProperties properties, Supplier<Clock> clock,
                                     EntityManager entityManager) {
        this.families = families;
        this.tokens = tokens;
        this.events = events;
        this.hasher = hasher;
        this.properties = properties;
        this.clock = clock;
        this.entityManager = entityManager;
    }

    @Override
    // noRollbackFor: issue() is called from inside AuthServiceImpl.login()/register()'s own
    // transaction (default REQUIRED propagation) as a best-effort enhancement — the caller
    // (issueRefreshCredential()) fully catches any failure and never lets it propagate further.
    // Without this rule, issue()'s own advice would still mark the SHARED transaction
    // rollback-only on any RuntimeException before the caller's catch ever runs, so
    // login()/register() would still fail with UnexpectedRollbackException at commit despite
    // catching nothing itself. (Propagation.NESTED/REQUIRES_NEW were considered and rejected:
    // NESTED needs JDBC savepoint support HibernateJpaDialect doesn't provide, and REQUIRES_NEW
    // would open a second physical transaction that can't see register()'s not-yet-committed
    // User row, breaking the family->user FK.
    //
    // Trade-off: both RefreshTokenFamily/RefreshToken use GenerationType.IDENTITY, so
    // families.save(family) below is an immediate physical INSERT, not deferred to flush.
    // hasher.generate() runs BEFORE that insert specifically so its realistic failure modes
    // (unknown/misconfigured active key id, Mac/JCE failure) can't leave an orphaned family
    // row — a failure there now persists nothing at all. tokens.save() immediately following
    // families.save() on the same connection/transaction remains a narrow residual risk (an
    // orphaned family row with no token if that specific insert fails), accepted as a rare
    // DB-level failure rather than a realistic app-level one.
    @Transactional(noRollbackFor = RuntimeException.class)
    public Session issue(User user) {
        Instant now = now();
        RefreshTokenFamily family = new RefreshTokenFamily();
        family.setFamilyUuid(UUID.randomUUID());
        family.setUser(user);
        family.setCurrentGeneration(0);
        family.setIssuedAt(now);
        family.setAbsoluteExpiresAt(now.plus(properties.refresh().absoluteLifetime()));
        RefreshTokenHasher.GeneratedCredential credential = hasher.generate();
        families.save(family);
        tokens.save(token(family, 0, credential, null, now));
        return new Session(family.getId(), credential.presentedValue(), family.getAbsoluteExpiresAt(), user.getId());
    }

    @Override
    @Transactional(noRollbackFor = Rejected.class)
    public Session rotate(String refreshCredential) {
        RefreshToken token = lockedToken(refreshCredential);
        RefreshTokenFamily family = token.getFamily();
        Instant now = now();
        if (token.getConsumedAt() != null) {
            RefreshToken successor = tokens.findByPredecessorId(token.getId()).orElse(null);
            if (isEligibleRetry(token, successor, family, now)) {
                RefreshTokenHasher.GeneratedCredential credential = hasher.deriveSuccessor(
                        refreshCredential, family.getFamilyUuid(), successor.getGeneration(), successor.getHmacKeyId());
                return new Session(family.getId(), credential.presentedValue(), family.getAbsoluteExpiresAt(), family.getUser().getId());
            }
            revokeFamily(family, now, FamilyRevocation.REUSE);
            throw new Rejected();
        }
        if (!isEligibleForRotation(token, family, now)) {
            throw new Rejected();
        }
        long generation = family.getCurrentGeneration() + 1;
        RefreshTokenHasher.GeneratedCredential successor = hasher.deriveSuccessor(
                refreshCredential, family.getFamilyUuid(), generation, hasher.activeKeyId());
        token.setConsumedAt(now);
        token.setLastPresentedAt(now);
        tokens.save(token(family, generation, successor, token, now));
        family.setCurrentGeneration(generation);
        family.setLastRotatedAt(now);
        family.setLastSeenAt(now);
        return new Session(family.getId(), successor.presentedValue(), family.getAbsoluteExpiresAt(), family.getUser().getId());
    }

    @Override
    @Transactional(noRollbackFor = Rejected.class)
    public void revokeCurrent(String refreshCredential) {
        RefreshToken token = lockedToken(refreshCredential);
        if (token.getConsumedAt() != null) {
            revokeFamily(token.getFamily(), now(), FamilyRevocation.REUSE);
            throw new Rejected();
        }
        revokeFamily(token.getFamily(), now(), FamilyRevocation.LOGOUT);
    }

    @Override
    @Transactional
    public void revokeAll(long userId, String reason) {
        Instant now = now();
        int revokedTokens = tokens.revokeActiveTokensForUser(userId, now);
        int revokedFamilies = families.revokeActiveFamiliesForUser(userId, now, reason);
        log.info("event=refresh_session.mass_revoked user_id={} reason={} active_tokens_revoked={} active_families_revoked={}",
                userId, safeReason(reason), revokedTokens, revokedFamilies);
    }

    private RefreshToken lockedToken(String refreshCredential) {
        RefreshTokenHasher.Digest digest;
        try {
            digest = hasher.digest(refreshCredential);
        } catch (IllegalArgumentException exception) {
            throw new Rejected();
        }
        RefreshToken candidate = tokens.findByHmacKeyIdAndTokenHmac(digest.keyId(), digest.digest())
                .orElseThrow(Rejected::new);
        RefreshTokenFamily family = families.findByIdForUpdate(candidate.getFamily().getId()).orElseThrow(Rejected::new);
        entityManager.refresh(candidate, LockModeType.PESSIMISTIC_WRITE);
        if (!candidate.getFamily().getId().equals(family.getId())) throw new Rejected();
        return candidate;
    }

    private RefreshToken token(RefreshTokenFamily family, long generation, RefreshTokenHasher.GeneratedCredential credential,
                               RefreshToken predecessor, Instant now) {
        RefreshToken token = new RefreshToken();
        token.setFamily(family);
        token.setGeneration(generation);
        token.setTokenHmac(credential.digest());
        token.setHmacKeyId(credential.keyId());
        token.setPredecessor(predecessor);
        token.setIssuedAt(now);
        token.setExpiresAt(family.getAbsoluteExpiresAt());
        return token;
    }

    private boolean isEligibleForRotation(RefreshToken token, RefreshTokenFamily family, Instant now) {
        boolean familyIsActive = family.getRevokedAt() == null;
        boolean familyIsWithinAbsoluteLifetime = family.getAbsoluteExpiresAt().isAfter(now);
        boolean tokenIsActive = token.getRevokedAt() == null;
        boolean tokenIsUnexpired = token.getExpiresAt().isAfter(now);
        boolean tokenIsCurrentGeneration = token.getGeneration() == family.getCurrentGeneration();
        boolean userIsEnabled = family.getUser().isEnabled();
        return familyIsActive && familyIsWithinAbsoluteLifetime && tokenIsActive && tokenIsUnexpired
                && tokenIsCurrentGeneration && userIsEnabled;
    }

    private boolean isEligibleRetry(RefreshToken predecessor, RefreshToken successor,
                                    RefreshTokenFamily family, Instant now) {
        return successor != null
                && now.isBefore(predecessor.getConsumedAt().plus(properties.refresh().retryGrace()))
                && successor.getGeneration() == predecessor.getGeneration() + 1
                && successor.getGeneration() == family.getCurrentGeneration()
                && family.getRevokedAt() == null
                && family.getAbsoluteExpiresAt().isAfter(now)
                && family.getUser().isEnabled()
                && successor.getRevokedAt() == null
                && successor.getExpiresAt().isAfter(now);
    }

    private void revokeFamily(RefreshTokenFamily family, Instant now, FamilyRevocation revocation) {
        boolean revoked = false;
        if (family.getRevokedAt() == null) {
            family.setRevokedAt(now);
            family.setRevocationReason(revocation.name());
            if (revocation == FamilyRevocation.REUSE) {
                family.setReuseDetectedAt(now);
            }
            tokens.revokeAllForFamily(family.getId(), now);
            revoked = true;
        }
        if (revocation == FamilyRevocation.REUSE
                && !events.existsByFamilyIdAndEventType(family.getId(), SessionSecurityEvent.Type.REFRESH_REUSE)) {
            SessionSecurityEvent event = new SessionSecurityEvent();
            event.setUser(family.getUser());
            event.setFamily(family);
            event.setEventType(SessionSecurityEvent.Type.REFRESH_REUSE);
            event.setOccurredAt(now);
            event.setDeliveryState(SessionSecurityEvent.DeliveryState.PENDING);
            events.save(event);
            log.warn("event=refresh_session.reuse_detected family_id={} user_id={} delivery_state=PENDING",
                    family.getId(), family.getUser().getId());
        } else if (revocation == FamilyRevocation.LOGOUT) {
            log.info("event=refresh_session.family_revoked family_id={} user_id={} reason=LOGOUT revoked={}",
                    family.getId(), family.getUser().getId(), revoked);
        }
    }

    private String safeReason(String reason) {
        if (reason == null) return "OTHER";
        return switch (reason) {
            case "LOGOUT", "LOGOUT_ALL", "ADMIN", "REUSE" -> reason;
            default -> "OTHER";
        };
    }

    private enum FamilyRevocation {
        LOGOUT,
        REUSE
    }

    private Instant now() {
        return Instant.now(clock.get()).truncatedTo(ChronoUnit.MICROS);
    }
}
