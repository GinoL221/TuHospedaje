package com.tuhospedaje.email.outbox;

import com.tuhospedaje.configuration.WelcomeEmailProperties;
import com.tuhospedaje.dto.email.EmailMessage;
import com.tuhospedaje.service.WelcomeEmailRenderer;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WelcomeEmailRendererTest {

    @Test
    void rendersSpanishWelcomeWithEscapedNameAndNormalizedLoginLink() {
        WelcomeEmailRenderer renderer = new WelcomeEmailRenderer(properties("https://app.example/"));

        EmailMessage message = renderer.render(42L, "ana@example.com", "Ana <script>alert(1)</script>");

        assertThat(message.to()).isEqualTo("ana@example.com");
        assertThat(message.emailType()).isEqualTo("WELCOME");
        assertThat(message.aggregateId()).isEqualTo("42");
        assertThat(message.subject()).isEqualTo("¡Bienvenido a TuHospedaje!");
        assertThat(message.htmlBody())
                .contains("Bienvenido", "Gracias por registrarte")
                .contains("href=\"https://app.example/login\"")
                .contains("Ana &lt;script&gt;alert(1)&lt;/script&gt;")
                .doesNotContain("<script>", "localhost", "Welcome", "Thanks");
    }

    @Test
    void preservesPublicBasePathWhileAddingOneLoginSegment() {
        WelcomeEmailRenderer renderer = new WelcomeEmailRenderer(properties("https://app.example/portal"));

        EmailMessage message = renderer.render(7L, "lucia@example.com", "Lucía");

        assertThat(message.htmlBody()).contains("href=\"https://app.example/portal/login\"");
    }

    @Test
    void rejectsMissingBlankLocalhostAndNonHttpsPublicBaseUrls() {
        assertThat(properties(null).isPublicBaseUrlSafe()).isFalse();
        assertThat(properties("").isPublicBaseUrlSafe()).isFalse();
        assertThat(properties("https://localhost").isPublicBaseUrlSafe()).isFalse();
        assertThat(properties("http://app.example").isPublicBaseUrlSafe()).isFalse();
        assertThatThrownBy(() -> new WelcomeEmailRenderer(properties("https://127.0.0.1"))
                .render(1L, "ana@example.com", "Ana"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Welcome public base URL is invalid");
    }

    private WelcomeEmailProperties properties(String publicBaseUrl) {
        WelcomeEmailProperties properties = new WelcomeEmailProperties();
        properties.setPublicBaseUrl(publicBaseUrl);
        return properties;
    }
}
