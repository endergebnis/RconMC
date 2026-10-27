package gg.endergebnis.rconmc.player;

/** Whitelist-Zustand eines registrierten Spielers. */
public enum PlayerStatus {
    /** Registriert, aber noch nicht auf der Whitelist des Servers. */
    PENDING,
    /** Per RCON auf die Whitelist gesetzt. */
    WHITELISTED
}
