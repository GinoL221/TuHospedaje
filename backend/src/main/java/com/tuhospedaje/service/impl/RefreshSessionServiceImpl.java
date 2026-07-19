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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.function.Supplier;

@Service
public class RefreshSessionServiceImpl implements RefreshSessionService {
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
    @Transactional
    public Session issue(User user) {
        Instant now = now();
        RefreshTokenFamily family = new RefreshTokenFamily();
        family.setFamilyUuid(UUID.randomUUID());
        family.setUser(user);
        family.setCurrentGeneration(0);
        family.setIssuedAt(now);
        family.setAbsoluteExpiresAt(now.plus(properties.refresh().absoluteLifetime()));
        families.save(family);
        RefreshTokenHasher.GeneratedCredential credential = hasher.generate();
        tokens.save(token(family, 0, credential, null, now));
        return new Session(family.getId(), credential.presentedValue(), family.getAbsoluteExpiresAt());
    }

    @Override
    @Transactional(noRollbackFor = Rejected.class)
    public Session rotate(String refreshCredential) {
        RefreshToken token = lockedToken(refreshCredential);
        RefreshTokenFamily family = token.getFamily();
        Instant now = now();
        if (token.getConsumedAt() != null) {
            revokeFamily(family, now, "REUSE", true);
            throw new Rejected();
        }
        if (family.getRevokedAt() != null || !family.getAbsoluteExpiresAt().isAfter(now)
                || token.getRevokedAt() != null || !token.getExpiresAt().isAfter(now)
                || token.getGeneration() != family.getCurrentGeneration() || !family.getUser().isEnabled()) {
            throw new Rejected();
        }
        RefreshTokenHasher.GeneratedCredential successor = hasher.generate();
        token.setConsumedAt(now);
        token.setLastPresentedAt(now);
        long generation = family.getCurrentGeneration() + 1;
        tokens.save(token(family, generation, successor, token, now));
        family.setCurrentGeneration(generation);
        family.setLastRotatedAt(now);
        family.setLastSeenAt(now);
        return new Session(family.getId(), successor.presentedValue(), family.getAbsoluteExpiresAt());
    }

    @Override
    @Transactional
    public void revokeCurrent(String refreshCredential) {
        RefreshToken token = lockedToken(refreshCredential);
        revokeFamily(token.getFamily(), now(), "LOGOUT", false);
    }

    @Override
    @Transactional
    public void revokeAll(long userId, String reason) {
        Instant now = now();
        tokens.revokeActiveTokensForUser(userId, now);
        families.revokeActiveFamiliesForUser(userId, now, reason);
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

    private void revokeFamily(RefreshTokenFamily family, Instant now, String reason, boolean reuse) {
        if (family.getRevokedAt() == null) {
            family.setRevokedAt(now);
            family.setRevocationReason(reason);
            if (reuse) {
                family.setReuseDetectedAt(now);
            }
            tokens.revokeAllForFamily(family.getId(), now);
        }
        if (reuse && !events.existsByFamilyIdAndEventType(family.getId(), "REFRESH_REUSE")) {
            SessionSecurityEvent event = new SessionSecurityEvent();
            event.setUser(family.getUser());
            event.setFamily(family);
            event.setEventType("REFRESH_REUSE");
            event.setOccurredAt(now);
            event.setDeliveryState("PENDING");
            events.save(event);
        }
    }

    private Instant now() {
        return Instant.now(clock.get()).truncatedTo(ChronoUnit.MICROS);
    }
}
