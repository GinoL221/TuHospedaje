package com.tuhospedaje.configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Serves the external canonical masters in the local {@code dev} profile.
 *
 * <p>Spring's resource handler resolves paths below the configured directory
 * and applies its normal resource/path traversal checks. The handler is not
 * enabled in production, where canonical assets must not be exposed from a
 * developer workstation filesystem.</p>
 */
@Configuration(proxyBeanMethods = false)
@Profile("dev")
public final class CanonicalAssetsWebConfiguration implements WebMvcConfigurer {

    private final CanonicalAssetsProperties properties;

    public CanonicalAssetsWebConfiguration(CanonicalAssetsProperties properties) {
        this.properties = properties;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = properties.root().toAbsolutePath().normalize().toUri().toString();
        if (!location.endsWith("/")) {
            location += "/";
        }
        registry.addResourceHandler("/canonical-lodging-images/**")
                .addResourceLocations(location)
                .setCachePeriod(3600);
    }
}
