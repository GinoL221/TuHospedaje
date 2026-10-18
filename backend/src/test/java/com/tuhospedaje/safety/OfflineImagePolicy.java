package com.tuhospedaje.safety;

import org.testcontainers.images.ImagePullPolicy;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.ImageNameSubstitutor;

/** Opt-in Testcontainers hooks for the developer self-check, not access controls. */
public final class OfflineImagePolicy {

    private OfflineImagePolicy() {
    }

    /** Prevents pulls through this policy; it does not prevent cached-image execution. */
    public static final class NeverPullPolicy implements ImagePullPolicy {
        @Override
        public boolean shouldPull(DockerImageName imageName) {
            return false;
        }
    }

    /** Rejects image names routed through this hook, not direct Docker or network access. */
    public static final class DenyImageSubstitutor extends ImageNameSubstitutor {
        @Override
        public DockerImageName apply(DockerImageName imageName) {
            throw new IllegalStateException("Testcontainers images are disabled for the safetyunit self-check");
        }

        @Override
        protected String getDescription() {
            return "safetyunit self-check image rejection";
        }
    }
}
