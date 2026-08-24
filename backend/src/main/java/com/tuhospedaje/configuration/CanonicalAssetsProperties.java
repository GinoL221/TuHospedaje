package com.tuhospedaje.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;

/**
 * Root directory for local-only canonical lodging assets.
 *
 * <p>The property is intentionally a filesystem path rather than a classpath
 * resource: the canonical JPEG masters live outside the repository and are
 * mounted/configured only for local development.</p>
 */
@ConfigurationProperties(prefix = "tuhospedaje.canonical-assets")
public record CanonicalAssetsProperties(Path root) {
}
