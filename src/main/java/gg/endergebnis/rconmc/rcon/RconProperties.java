package gg.endergebnis.rconmc.rcon;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** Verbindungsdaten des Minecraft-RCON. */
@ConfigurationProperties(prefix = "rconmc.rcon")
@Validated
public record RconProperties(
        @NotBlank String host,
        @Min(1) @Max(65535) int port,
        @NotBlank String password) {

    @Override
    public String toString() {
        return "RconProperties[host=" + host + ", port=" + port + ", password=[REDACTED]]";
    }
}
