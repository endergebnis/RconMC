package gg.endergebnis.rconmc.whitelist;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import gg.endergebnis.rconmc.player.BotState;
import gg.endergebnis.rconmc.player.BotStateRepository;
import gg.endergebnis.rconmc.player.Player;
import gg.endergebnis.rconmc.player.PlayerRepository;
import gg.endergebnis.rconmc.player.PlayerStatus;
import gg.endergebnis.rconmc.rcon.RconClient;
import gg.endergebnis.rconmc.rcon.RconException;
import gg.endergebnis.rconmc.verify.AccountVerifier;
import gg.endergebnis.rconmc.verify.VerificationException;
import gg.endergebnis.rconmc.verify.VerifiedAccount;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Die gesamte Fachlogik des Bots: Registrierung (Mojang-Pruefung, Eindeutigkeit, Speichern, ggf. sofortiges
 * {@code whitelist add}) und die Admin-Sammelaktionen. Die Discord-Schicht ruft nur diese Klasse auf und kennt
 * weder RCON noch Datenbank.
 */
@Service
public class WhitelistService {

    private static final Logger log = LoggerFactory.getLogger(WhitelistService.class);

    static final String STATE_WHITELIST_STATUS = "whitelist_status";
    static final String STATE_MESSAGE_ID = "whitelist_message_id";
    private static final int RCON_RETRIES = 3;

    private final PlayerRepository players;
    private final BotStateRepository state;
    private final AccountVerifier verifier;
    private final RconClient rcon;

    public WhitelistService(
            PlayerRepository players, BotStateRepository state, AccountVerifier verifier, RconClient rcon) {
        this.players = players;
        this.state = state;
        this.verifier = verifier;
        this.rcon = rcon;
    }

    // ---- Registrierung -------------------------------------------------------------------------------

    public RegistrationResult register(String discordId, String discordUsername, String mcName) {
        Optional<VerifiedAccount> verified;
        try {
            verified = verifier.verify(mcName);
        } catch (VerificationException e) {
            log.error("Verifizierung fehlgeschlagen: {}", e.getMessage(), e);
            return new RegistrationResult.VerificationFailed();
        }
        if (verified.isEmpty()) {
            return new RegistrationResult.NotFound();
        }
        VerifiedAccount account = verified.get();

        if (players.existsByMcUuidAndDiscordIdNot(account.uuid(), discordId)) {
            return new RegistrationResult.Conflict();
        }

        Instant now = Instant.now();
        Player player = players.findByDiscordId(discordId).orElseGet(() -> new Player(discordId, now));
        player.register(discordUsername, account.name(), account.uuid(), now);
        players.save(player);

        if (whitelistStatus() != WhitelistStatus.OPEN) {
            return new RegistrationResult.Pending(account.name());
        }
        try {
            rcon.execute("whitelist add " + account.name(), RCON_RETRIES);
        } catch (RconException e) {
            log.error("RCON fehlgeschlagen fuer {}: {}", account.name(), e.getMessage());
            return new RegistrationResult.WhitelistFailed(account.name());
        }
        player.status(PlayerStatus.WHITELISTED, Instant.now());
        players.save(player);
        return new RegistrationResult.Whitelisted(account.name());
    }

    // ---- Admin-Aktionen ------------------------------------------------------------------------------

    /** Oeffnet die Whitelist und setzt alle {@code PENDING}-Spieler per RCON darauf. */
    public BatchResult open() {
        setWhitelistStatus(WhitelistStatus.OPEN);
        return batch(players.findByStatusOrderByIdAsc(PlayerStatus.PENDING), false);
    }

    /** Schliesst die Whitelist: neue Registrierungen werden nur noch vorgemerkt. */
    public void close() {
        setWhitelistStatus(WhitelistStatus.CLOSED);
    }

    /** Setzt alle Spieler aus der Datenbank auf die Whitelist. */
    public BatchResult openAll() {
        return batch(players.findAllByOrderByIdAsc(), false);
    }

    /** Entfernt alle Spieler aus der Datenbank von der Whitelist (Status danach {@code PENDING}). */
    public BatchResult closeAll() {
        return batch(players.findAllByOrderByIdAsc(), true);
    }

    /**
     * Fuehrt fuer alle Spieler {@code whitelist add} bzw. {@code whitelist remove} aus; einzelne Fehler werden
     * geloggt und uebersprungen.
     */
    private BatchResult batch(List<Player> batch, boolean remove) {
        PlayerStatus target = remove ? PlayerStatus.PENDING : PlayerStatus.WHITELISTED;
        int ok = 0;
        for (Player player : batch) {
            String command = (remove ? "whitelist remove " : "whitelist add ") + player.mcUsername();
            try {
                rcon.execute(command, RCON_RETRIES);
            } catch (RconException e) {
                log.error("RCON fehlgeschlagen fuer {}: {}", player.mcUsername(), e.getMessage());
                continue;
            }
            try {
                player.status(target, Instant.now());
                players.save(player);
                ok++;
            } catch (RuntimeException e) {
                log.error("DB-Update fehlgeschlagen fuer {}: {}", player.mcUsername(), e.getMessage(), e);
            }
        }
        return new BatchResult(ok, batch.size());
    }

    // ---- Abfragen ------------------------------------------------------------------------------------

    public Optional<Player> findByMcUsername(String mcUsername) {
        return players.findFirstByMcUsernameIgnoreCase(mcUsername);
    }

    public Optional<Player> findByDiscordUsername(String discordUsername) {
        return players.findFirstByDiscordUsernameIgnoreCase(discordUsername);
    }

    // ---- Bot-Zustand ---------------------------------------------------------------------------------

    public WhitelistStatus whitelistStatus() {
        return state.findById(STATE_WHITELIST_STATUS)
                .map(entry -> WhitelistStatus.valueOf(entry.content()))
                .orElse(WhitelistStatus.CLOSED);
    }

    public void rememberRegistrationMessage(long messageId) {
        state.save(new BotState(STATE_MESSAGE_ID, Long.toString(messageId)));
    }

    private void setWhitelistStatus(WhitelistStatus status) {
        state.save(new BotState(STATE_WHITELIST_STATUS, status.name()));
    }
}
