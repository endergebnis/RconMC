package gg.endergebnis.rconmc.rcon;

/** Verbindungs-, Protokoll- oder Authentifizierungsfehler bei der RCON-Kommunikation. */
public class RconException extends RuntimeException {

    public RconException(String message) {
        super(message);
    }

    public RconException(String message, Throwable cause) {
        super(message, cause);
    }
}
