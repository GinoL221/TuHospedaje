package com.tuhospedaje.configuration;

import com.tuhospedaje.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtService {
    @Value("${app.jwt.secret}")
    private String secretKey;

    @Value("${app.jwt.expiration}")
    private long expirationMillis;

    @Autowired(required = false)
    private Environment environment;

    @PostConstruct
    public void validateSecretKey() {
        if (secretKey == null || secretKey.trim().isEmpty()) {
            throw new IllegalStateException("JWT secret key must not be blank");
        }
        byte[] keyBytes;
        try {
            keyBytes = Decoders.BASE64.decode(secretKey.trim());
        } catch (Exception e) {
            throw new IllegalStateException("JWT secret key must be valid Base64", e);
        }
        if (keyBytes.length < 32) {
            throw new IllegalStateException("JWT secret key must decode to at least 256 bits (32 bytes), but got " + keyBytes.length + " bytes");
        }
        if (environment != null && environment.matchesProfiles("prod")) {
            String decodedString = new String(keyBytes, StandardCharsets.UTF_8).toLowerCase(Locale.ROOT);
            String rawSecret = secretKey.toLowerCase(Locale.ROOT);
            if (rawSecret.contains("change-me") || rawSecret.contains("openssl-rand")
                    || decodedString.contains("change-me") || decodedString.contains("openssl-rand")) {
                throw new IllegalStateException("Production environment cannot use default or placeholder JWT secret");
            }
        }
    }

    public String generateToken(UserDetails userDetails) {
        return generateToken(new HashMap<>(), userDetails);
    }

    public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>(extraClaims);
        if (userDetails instanceof User user) {
            claims.putIfAbsent("tokenVersion", user.getTokenVersion());
        }
        return Jwts.builder()
                .claims(claims)
                .subject(userDetails.getUsername())
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expirationMillis))
                .signWith(getSigningKey())
                .compact();
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public long getExpirationSeconds() {
        return expirationMillis / 1000;
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        if (!username.equals(userDetails.getUsername()) || isTokenExpired(token)) {
            return false;
        }
        if (userDetails instanceof User user) {
            Integer tokenVersion = extractClaim(token, claims -> claims.get("tokenVersion", Integer.class));
            if (tokenVersion != null && tokenVersion != user.getTokenVersion()) {
                return false;
            }
        }
        return true;
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
