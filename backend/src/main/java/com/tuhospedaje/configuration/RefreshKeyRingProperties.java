package com.tuhospedaje.configuration;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Validated
@ConfigurationProperties(prefix = "app.session.key-ring")
public record RefreshKeyRingProperties(
        @NotBlank String activeKeyId,
        @NotEmpty @Valid List<KeyEntry> keyEntries
) {
    public Map<String, String> keys() {
        return keyEntries.stream().collect(Collectors.toUnmodifiableMap(KeyEntry::id, KeyEntry::secret));
    }

    @AssertTrue(message = "active-key-id must reference an entry in key-entries")
    public boolean isActiveKeyConfigured() {
        return activeKeyId != null && keyEntries != null
                && keyEntries.stream().anyMatch(entry -> activeKeyId.equals(entry.id()));
    }

    public record KeyEntry(@NotBlank String id, @NotBlank String secret) {
    }
}
