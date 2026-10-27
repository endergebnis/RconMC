package gg.endergebnis.rconmc.player;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Einfacher Key-Value-Speicher fuer Bot-Zustand (Whitelist offen/geschlossen, ID der Registrierungsnachricht). */
@Entity
@Table(name = "bot_state")
public class BotState {

    @Id
    private String name;

    @Column(nullable = false)
    private String content;

    protected BotState() {}

    public BotState(String name, String content) {
        this.name = name;
        this.content = content;
    }

    public String name() {
        return name;
    }

    public String content() {
        return content;
    }
}
