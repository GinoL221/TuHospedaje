package com.tuhospedaje.safety;

import org.junit.jupiter.api.Test;
import org.testcontainers.images.PullPolicy;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.ImageNameSubstitutor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class OfflineImagePolicyTest {

    @Test
    void neverRequestsPullsThroughThisPolicy() {
        var policy = new OfflineImagePolicy.NeverPullPolicy();
        for (String image : new String[]{"mariadb:10.11", "alpine:3.21", "registry.example.test/team/app:1"}) {
            assertThat(policy.shouldPull(DockerImageName.parse(image))).isFalse();
        }
    }

    @Test
    void rejectsImageNamesThroughThisSubstitutor() {
        assertThatThrownBy(() -> new OfflineImagePolicy.DenyImageSubstitutor()
                .apply(DockerImageName.parse("mariadb:10.11")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Testcontainers images are disabled for the safetyunit self-check");
    }

    @Test
    void launcherHooksAreLoadedByTheResolvedTestcontainersVersion() {
        assumeTrue(System.getenv("TESTCONTAINERS_IMAGE_SUBSTITUTOR") != null,
                "Hook activation is checked only by the opt-in launcher");
        assertThat(PullPolicy.defaultPolicy()).isInstanceOf(OfflineImagePolicy.NeverPullPolicy.class);
        assertThatThrownBy(() -> ImageNameSubstitutor.instance()
                .apply(DockerImageName.parse("mariadb:10.11")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Testcontainers images are disabled for the safetyunit self-check");
    }
}
