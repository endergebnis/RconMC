package gg.endergebnis.rconmc.whitelist;

/** Ausgang einer Registrierung ueber das Discord-Modal. */
public sealed interface RegistrationResult {

    /** Der Name existiert nicht (oder hat kein gueltiges Format). */
    record NotFound() implements RegistrationResult {}

    /** Die Pruefung gegen Mojang ist fehlgeschlagen. */
    record VerificationFailed() implements RegistrationResult {}

    /** Der Minecraft-Account gehoert bereits einem anderen Discord-Nutzer. */
    record Conflict() implements RegistrationResult {}

    /** Gespeichert; Whitelist ist geschlossen, der Spieler wartet auf {@code /whitelist open}. */
    record Pending(String mcName) implements RegistrationResult {}

    /** Gespeichert und sofort per RCON auf die Whitelist gesetzt. */
    record Whitelisted(String mcName) implements RegistrationResult {}

    /** Gespeichert, aber der RCON-Aufruf ist fehlgeschlagen. */
    record WhitelistFailed(String mcName) implements RegistrationResult {}
}
