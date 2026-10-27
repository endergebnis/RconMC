package gg.endergebnis.rconmc.player;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Dauerhafte Verknuepfung zwischen einer Discord- und einer Minecraft-Identitaet. Pro Discord-Nutzer gibt es
 * hoechstens einen Eintrag; eine erneute Registrierung ueberschreibt ihn und setzt ihn auf {@link
 * PlayerStatus#PENDING} zurueck.
 */
@Entity
@Table(name = "players")
public class Player {

    public static final String PLATFORM_JAVA = "java";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String discordId;

    @Column(nullable = false)
    private String discordUsername;

    @Column(nullable = false)
    private String mcUsername;

    private String mcUuid;

    @Column(nullable = false)
    private String platform = PLATFORM_JAVA;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PlayerStatus status = PlayerStatus.PENDING;

    @Column(nullable = false)
    private Instant registeredAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected Player() {}

    public Player(String discordId, Instant now) {
        this.discordId = discordId;
        this.registeredAt = now;
        this.updatedAt = now;
    }

    /** Uebernimmt eine (Neu-)Registrierung; der Spieler ist danach wieder {@code PENDING}. */
    public void register(String discordUsername, String mcUsername, String mcUuid, Instant now) {
        this.discordUsername = discordUsername;
        this.mcUsername = mcUsername;
        this.mcUuid = mcUuid;
        this.status = PlayerStatus.PENDING;
        this.updatedAt = now;
    }

    public void status(PlayerStatus status, Instant now) {
        this.status = status;
        this.updatedAt = now;
    }

    public Long id() {
        return id;
    }

    public String discordId() {
        return discordId;
    }

    public String discordUsername() {
        return discordUsername;
    }

    public String mcUsername() {
        return mcUsername;
    }

    public String mcUuid() {
        return mcUuid;
    }

    public String platform() {
        return platform;
    }

    public PlayerStatus status() {
        return status;
    }

    public Instant registeredAt() {
        return registeredAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }
}
