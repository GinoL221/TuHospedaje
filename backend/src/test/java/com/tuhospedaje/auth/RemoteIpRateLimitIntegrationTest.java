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
 * Verifies that when Tomcat is configured with native forward headers strategy
 * (RemoteIpValve), per-IP rate limiting correctly attributes requests to the
 * client IP in X-Forwarded-For rather than grouping all clients behind the proxy's IP.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "server.forward-headers-strategy=native",
                "app.auth.rate-limit.enabled=true",
                "app.auth.rate-limit.login-per-ip-per-minute=2",
                "app.auth.rate-limit.login-per-email-per-minute=100",
                "app.auth.rate-limit.register-per-ip-per-minute=100",
                "app.auth.rate-limit.register-per-email-per-minute=100"
        }
)
class RemoteIpRateLimitIntegrationTest extends AbstractIntegrationTest {

    @LocalServerPort
    private int port;

    private final HttpClient client = HttpClient.newHttpClient();

    @Test
    @DisplayName("Trusted proxy: X-Forwarded-For isolates rate limits per client IP")
    void forwardedForHeader_fromTrustedProxy_isolatesRateLimitsPerClientIp() throws Exception {
        URI loginUri = URI.create("http://127.0.0.1:" + port + "/api/auth/login");
        String body = "{\"email\":\"test-ratelimit@example.com\",\"password\":\"wrong-password\"}";

        // Client 1 (203.0.113.1): attempt 1 -> 401
        HttpRequest r1 = HttpRequest.newBuilder(loginUri)
                .header("Content-Type", "application/json")
                .header("X-Forwarded-For", "203.0.113.1")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        assertThat(client.send(r1, HttpResponse.BodyHandlers.ofString()).statusCode())
                .isIn(HttpStatus.UNAUTHORIZED.value(), HttpStatus.BAD_REQUEST.value());

        // Client 1 (203.0.113.1): attempt 2 -> 401
        HttpRequest r2 = HttpRequest.newBuilder(loginUri)
                .header("Content-Type", "application/json")
                .header("X-Forwarded-For", "203.0.113.1")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        assertThat(client.send(r2, HttpResponse.BodyHandlers.ofString()).statusCode())
                .isIn(HttpStatus.UNAUTHORIZED.value(), HttpStatus.BAD_REQUEST.value());

        // Client 1 (203.0.113.1): attempt 3 -> 429 Too Many Requests (quota exceeded)
        HttpRequest r3 = HttpRequest.newBuilder(loginUri)
                .header("Content-Type", "application/json")
                .header("X-Forwarded-For", "203.0.113.1")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        HttpResponse<String> resp3 = client.send(r3, HttpResponse.BodyHandlers.ofString());
        assertThat(resp3.statusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS.value());

        // Client 2 (198.51.100.2): attempt 1 -> must NOT be blocked (401, not 429)
        HttpRequest rClient2 = HttpRequest.newBuilder(loginUri)
                .header("Content-Type", "application/json")
                .header("X-Forwarded-For", "198.51.100.2")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        HttpResponse<String> respClient2 = client.send(rClient2, HttpResponse.BodyHandlers.ofString());
        assertThat(respClient2.statusCode())
                .as("Client 2 should have its own rate-limit quota and not be blocked by Client 1")
                .isIn(HttpStatus.UNAUTHORIZED.value(), HttpStatus.BAD_REQUEST.value());
    }
}
