package com.tuhospedaje.configuration;

import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Serves locally stored uploads from {@code /uploads/**} out of the directory
 * configured via {@code tuhospedaje.uploads.directory}.
 *
 * <p>Spring's resource handler resolves paths below the configured directory
 * only: {@code ..} segments (literal or URL-encoded) are rejected before the
 * filesystem is touched — see {@code LocalImageStorageIntegrationTest} for the
 * traversal regression coverage.</p>
 *
 * <p>Segments starting with {@code .} (the write-in-progress {@code .incoming}
 * staging directory, OS droppings like {@code .DS_Store}) are never served: the
 * custom resolver below returns null for them, which the handler answers with 404.</p>
 *
 * <p>The directory must be writable and, in containerized deployments, backed by
 * a persistent volume (see compose.*.yaml) so uploaded images survive restarts.</p>
 */
@Configuration(proxyBeanMethods = false)
public class UploadedImagesWebConfiguration implements WebMvcConfigurer {

    private final Path directory;

    public UploadedImagesWebConfiguration(@Value("${tuhospedaje.uploads.directory}") String directory) {
        this.directory = Path.of(directory);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = directory.toAbsolutePath().normalize().toUri().toString();
        if (!location.endsWith("/")) {
            location += "/";
        }
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(location)
                .setCachePeriod(3600)
                .resourceChain(false)
                .addResolver(new HiddenSegmentRejectingResolver());
    }

    /**
     * Rejects any path containing a dot-prefixed segment, so hidden working
     * directories (like the {@code .incoming} upload staging directory) can never
     * be read back through the public URL, even if a leftover file sits there.
     */
    static final class HiddenSegmentRejectingResolver extends PathResourceResolver {

        @Override
        protected @Nullable Resource getResource(String resourcePath, Resource location) throws IOException {
            for (String segment : resourcePath.split("/")) {
                if (segment.startsWith(".")) {
                    return null;
                }
            }
            return super.getResource(resourcePath, location);
        }
    }
}
