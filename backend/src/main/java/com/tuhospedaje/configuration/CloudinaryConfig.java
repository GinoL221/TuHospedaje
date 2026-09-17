package com.tuhospedaje.configuration;

import com.cloudinary.Cloudinary;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
public class CloudinaryConfig {

    private final String cloudName;
    private final String apiKey;
    private final String apiSecret;
    private final int timeoutSeconds;

    public CloudinaryConfig(
            @Value("${cloudinary.cloud-name:}") String cloudName,
            @Value("${cloudinary.api-key:}") String apiKey,
            @Value("${cloudinary.api-secret:}") String apiSecret,
            @Value("${cloudinary.timeout:15}") int timeoutSeconds) {
        this.cloudName = cloudName;
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
        this.timeoutSeconds = timeoutSeconds;
    }

    @Bean
    @ConditionalOnProperty(name = "cloudinary.cloud-name")
    public Cloudinary cloudinary() {
        // cloudinary-http5 applies its supported timeout key to connect, response,
        // and request deadlines in seconds.
        return new Cloudinary(cloudinaryConfiguration());
    }

    Map<String, Object> cloudinaryConfiguration() {
        return Map.of(
                "cloud_name", cloudName,
                "api_key", apiKey,
                "api_secret", apiSecret,
                "timeout", timeoutSeconds
        );
    }
}
