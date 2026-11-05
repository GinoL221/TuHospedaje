package com.tuhospedaje.auth;

import com.tuhospedaje.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that when a caller is not in Tomcat's internal-proxies allow-list,
 * X-Forwarded-For headers are ignored and cannot be used to spoof client IPs
 * or bypass rate limits.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "server.forward-headers-strategy=native",
                // Only trust 10.x.x.x addresses; 127.0.0.1 is treated as untrusted caller
                "server.tomcat.remoteip.internal-proxies=10\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}",
                "app.auth.rate-limit.enabled=true",
                "app.auth.rate-limit.login-per-ip-per-minute=2",
                "app.auth.rate-limit.login-per-email-per-minute=100",
                "app.auth.rate-limit.register-per-ip-per-minute=100",
                "app.auth.rate-limit.register-per-email-per-minute=100"
        }
)
class UntrustedRemoteIpRateLimitIntegrationTest extends AbstractIntegrationTest {

    @LocalServerPort
    private int port;

    private final HttpClient client = HttpClient.newHttpClient();

    @Test
    @DisplayName("Untrusted caller cannot spoof client IP via X-Forwarded-For")
    void untrustedCaller_cannotSpoofClientIpViaForwardedFor() throws Exception {
        URI loginUri = URI.create("http://127.0.0.1:" + port + "/api/auth/login");
        String body = "{\"email\":\"test-spoof@example.com\",\"password\":\"wrong-password\"}";

        // Attempt 1 from 127.0.0.1 claiming to be 203.0.113.1
        HttpRequest r1 = HttpRequest.newBuilder(loginUri)
                .header("Content-Type", "application/json")
                .header("X-Forwarded-For", "203.0.113.1")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        assertThat(client.send(r1, HttpResponse.BodyHandlers.ofString()).statusCode())
                .isIn(HttpStatus.UNAUTHORIZED.value(), HttpStatus.BAD_REQUEST.value());

        // Attempt 2 from 127.0.0.1 claiming to be 203.0.113.2
        HttpRequest r2 = HttpRequest.newBuilder(loginUri)
                .header("Content-Type", "application/json")
                .header("X-Forwarded-For", "203.0.113.2")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        assertThat(client.send(r2, HttpResponse.BodyHandlers.ofString()).statusCode())
                .isIn(HttpStatus.UNAUTHORIZED.value(), HttpStatus.BAD_REQUEST.value());

        // Attempt 3 from 127.0.0.1 claiming to be 203.0.113.3:
        // Because 127.0.0.1 is NOT in internal-proxies, Tomcat ignores X-Forwarded-For
        // and attributes all requests to 127.0.0.1. Thus, attempt 3 hits the IP limit (429)!
        HttpRequest r3 = HttpRequest.newBuilder(loginUri)
                .header("Content-Type", "application/json")
                .header("X-Forwarded-For", "203.0.113.3")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        HttpResponse<String> resp3 = client.send(r3, HttpResponse.BodyHandlers.ofString());
        assertThat(resp3.statusCode())
                .as("Rotating X-Forwarded-For from an untrusted origin must not evade the rate limit")
                .isEqualTo(HttpStatus.TOO_MANY_REQUESTS.value());
    }
}
