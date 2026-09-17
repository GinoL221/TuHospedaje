package com.tuhospedaje.configuration;

import com.cloudinary.Cloudinary;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CloudinaryConfigTest {

    @Test
    void cloudinaryUsesTheSupportedFifteenSecondSdkTimeout() {
        CloudinaryConfig config = new CloudinaryConfig("cloud", "key", "secret", 15);

        Map<String, Object> sdkConfiguration = config.cloudinaryConfiguration();
        Cloudinary cloudinary = config.cloudinary();

        assertThat(sdkConfiguration)
                .containsEntry("timeout", 15)
                .hasSize(4)
                .doesNotContainKeys("connect_timeout", "read_timeout");
        assertThat(cloudinary.config.timeout).isEqualTo(15);
    }
}
