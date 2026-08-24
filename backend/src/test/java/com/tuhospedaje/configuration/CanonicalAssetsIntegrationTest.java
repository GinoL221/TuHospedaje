package com.tuhospedaje.configuration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@Import(TestcontainersConfiguration.class)
class CanonicalAssetsIntegrationTest {

    private static final Path ASSET_ROOT = createFixtureRoot();

    @Autowired
    private MockMvc mockMvc;

    @DynamicPropertySource
    static void canonicalAssetProperties(DynamicPropertyRegistry registry) {
        registry.add("tuhospedaje.canonical-assets.root", ASSET_ROOT::toString);
    }

    @Test
    void servesCanonicalMasterFromConfiguredExternalRoot() throws Exception {
        mockMvc.perform(get("/canonical-lodging-images/lodging-test/masters/fixture.jpg"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.IMAGE_JPEG));
    }

    private static Path createFixtureRoot() {
        try {
            Path root = Files.createTempDirectory("tuhospedaje-canonical-assets-");
            Path master = root.resolve("lodging-test/masters/fixture.jpg");
            Files.createDirectories(master.getParent());
            Files.write(master, Base64.getDecoder().decode(
                    "/9j/4AAQSkZJRgABAQAAAQABAAD/2wBDAP//////////////////////////////////////////////////////////////////////////////////////2wBDAf//////////////////////////////////////////////////////////////////////////////////////wAARCAABAAEDASIAAhEBAxEB/8QAFQABAQAAAAAAAAAAAAAAAAAAAAX/xAAUEAEAAAAAAAAAAAAAAAAAAAAA/9oADAMBAAIQAxAAAAH/xAAUEAEAAAAAAAAAAAAAAAAAAAAA/9oACAEBAAEFAqf/xAAUEQEAAAAAAAAAAAAAAAAAAAAA/9oACAEDAQE/AYf/xAAUEQEAAAAAAAAAAAAAAAAAAAAA/9oACAECAQE/AYf/xAAUEAEAAAAAAAAAAAAAAAAAAAAA/9oADAMBAAIAAwAAAB//xAAUEAEAAAAAAAAAAAAAAAAAAAAA/9oACAEBAwE/EF//xAAUEQEAAAAAAAAAAAAAAAAAAAAA/9oACAECAwE/EF//2gAMAwEAAgADAAAAEP/EABQRAQAAAAAAAAAAAAAAAAAAABD/2gAIAQMBAT8QH//EABQRAQAAAAAAAAAAAAAAAAAAABD/2gAIAQIBAT8QH//EABQQAQAAAAAAAAAAAAAAAAAAABD/2gAIAQEAAT8QH//Z"));
            return root;
        } catch (IOException ex) {
            throw new ExceptionInInitializerError(ex);
        }
    }
}
