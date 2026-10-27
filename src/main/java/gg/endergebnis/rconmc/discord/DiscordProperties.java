package gg.endergebnis.rconmc.discord;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Discord-Konfiguration: Bot-Token, Zielserver, Kanal der Registrierungsnachricht und die optionale Admin-Rolle
 * (zusaetzlich zur Berechtigung "Server verwalten").
 */
@ConfigurationProperties(prefix = "rconmc.discord")
@Validated
public record DiscordProperties(
        @NotBlank String token,
        @Positive long guildId,
        @Positive long channelId,
        Long adminRoleId,
        String registrationMessage) {

    public static final String DEFAULT_REGISTRATION_MESSAGE =
            "Klicke auf den Button, um dich mit deinem Minecraft-Namen zu registrieren.";

    public DiscordProperties {
        if (registrationMessage == null || registrationMessage.isBlank()) {
            registrationMessage = DEFAULT_REGISTRATION_MESSAGE;
        }
    }

    @Override
    public String toString() {
        return "DiscordProperties[token=[REDACTED], guildId=" + guildId + ", channelId=" + channelId
                + ", adminRoleId=" + adminRoleId + "]";
    }
}
