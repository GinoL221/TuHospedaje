package com.tuhospedaje.safety;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OfflineLauncherContractTest {

    @Test
    void profileIsOptInAndSelectsOnlyTheSelfCheckTests() throws Exception {
        String pom = Files.readString(Path.of("pom.xml"));
        int start = pom.indexOf("<id>safetyunit</id>");
        assertThat(start).isGreaterThanOrEqualTo(0);
        String profile = pom.substring(start, pom.indexOf("</profile>", start));
        assertThat(profile).contains("OfflineImagePolicyTest.java", "OfflineLauncherContractTest.java",
                "<failIfNoTests>true</failIfNoTests>", "<forkCount>1</forkCount>",
                "${safetyunit.build}", "${safetyunit.home}", "${safetyunit.tmp}", "<skip>true</skip>");
        assertThat(profile).doesNotContain("<activation>", "*IntegrationTest", "<dependencies>");
    }
}
