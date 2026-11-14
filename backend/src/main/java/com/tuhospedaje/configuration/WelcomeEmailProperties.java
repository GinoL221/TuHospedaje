package com.tuhospedaje.configuration;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;

@Validated
@Getter
@Setter
@ConfigurationProperties(prefix = "tuhospedaje.email.welcome")
public class WelcomeEmailProperties {

    @NotBlank
    private @Nullable String publicBaseUrl;

    private Duration resendCooldown = Duration.ofMinutes(5);

    @AssertTrue(message = "public-base-url must be a safe absolute HTTPS public URL")
    public boolean isPublicBaseUrlSafe() {
        if (publicBaseUrl == null) {
            return false;
        }
        try {
            URI uri = new URI(publicBaseUrl);
            if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null
                    || uri.getHost().isBlank() || uri.getUserInfo() != null
                    || uri.getQuery() != null || uri.getFragment() != null) {
                return false;
            }
            return !isUnsafeLiteralHost(uri.getHost());
        } catch (URISyntaxException | NullPointerException exception) {
            return false;
        }
    }

    public String getNormalizedPublicBaseUrl() {
        if (publicBaseUrl == null || !isPublicBaseUrlSafe()) {
            throw new IllegalStateException("Welcome public base URL is invalid");
        }
        return publicBaseUrl.replaceAll("/+$", "");
    }

    private boolean isUnsafeLiteralHost(String host) {
        if ("localhost".equalsIgnoreCase(host)) {
            return true;
        }
        if (!host.matches("[0-9.]+") && !host.contains(":")) {
            return false;
        }
        try {
            InetAddress address = InetAddress.getByName(host);
            return address.isAnyLocalAddress() || address.isLoopbackAddress()
                    || address.isLinkLocalAddress() || address.isSiteLocalAddress();
        } catch (Exception exception) {
            return true;
        }
    }
}
