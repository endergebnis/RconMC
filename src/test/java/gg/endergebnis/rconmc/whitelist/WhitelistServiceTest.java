package gg.endergebnis.rconmc.whitelist;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class WhitelistServiceTest {

    private final PlayerRepository players = mock(PlayerRepository.class);
    private final BotStateRepository state = mock(BotStateRepository.class);
    private final AccountVerifier verifier = mock(AccountVerifier.class);
    private final RconClient rcon = mock(RconClient.class);
    private final WhitelistService service = new WhitelistService(players, state, verifier, rcon);

    private void whitelistIs(WhitelistStatus status) {
        when(state.findById(WhitelistService.STATE_WHITELIST_STATUS))
                .thenReturn(Optional.of(new BotState(WhitelistService.STATE_WHITELIST_STATUS, status.name())));
    }

    private static Player player(String discordId, String mcName, PlayerStatus status) {
        Instant now = Instant.now();
        Player player = new Player(discordId, now);
        player.register("dc-" + discordId, mcName, "uuid-" + discordId, now);
        player.status(status, now);
        return player;
    }

    @Test
    void unknownNameIsNotFoundAndNothingIsStored() {
        when(verifier.verify("ghost")).thenReturn(Optional.empty());

        assertThat(service.register("1", "alex", "ghost")).isInstanceOf(RegistrationResult.NotFound.class);
        verify(players, never()).save(any());
    }

    @Test
    void verifierFailureIsReportedAsVerificationFailed() {
        when(verifier.verify("notch")).thenThrow(new VerificationException("down"));

        assertThat(service.register("1", "alex", "notch")).isInstanceOf(RegistrationResult.VerificationFailed.class);
        verify(players, never()).save(any());
    }

    @Test
    void accountOfAnotherDiscordUserIsAConflict() {
        when(verifier.verify("notch")).thenReturn(Optional.of(new VerifiedAccount("Notch", "u1")));
        when(players.existsByMcUuidAndDiscordIdNot("u1", "1")).thenReturn(true);

        assertThat(service.register("1", "alex", "notch")).isInstanceOf(RegistrationResult.Conflict.class);
        verify(players, never()).save(any());
    }

    @Test
    void closedWhitelistStoresThePlayerAsPendingWithoutTouchingRcon() {
        when(verifier.verify("notch")).thenReturn(Optional.of(new VerifiedAccount("Notch", "u1")));
        when(players.findByDiscordId("1")).thenReturn(Optional.empty());
        whitelistIs(WhitelistStatus.CLOSED);

        assertThat(service.register("1", "alex", "notch")).isEqualTo(new RegistrationResult.Pending("Notch"));

        ArgumentCaptor<Player> saved = ArgumentCaptor.forClass(Player.class);
        verify(players).save(saved.capture());
        assertThat(saved.getValue().mcUsername()).isEqualTo("Notch");
        assertThat(saved.getValue().status()).isEqualTo(PlayerStatus.PENDING);
        verify(rcon, never()).execute(anyString(), anyInt());
    }

    @Test
    void missingStateRowMeansClosed() {
        when(state.findById(anyString())).thenReturn(Optional.empty());

        assertThat(service.whitelistStatus()).isEqualTo(WhitelistStatus.CLOSED);
    }

    @Test
    void openWhitelistAddsThePlayerViaRconAndMarksHimWhitelisted() {
        when(verifier.verify("notch")).thenReturn(Optional.of(new VerifiedAccount("Notch", "u1")));
        when(players.findByDiscordId("1")).thenReturn(Optional.empty());
        whitelistIs(WhitelistStatus.OPEN);

        assertThat(service.register("1", "alex", "notch")).isEqualTo(new RegistrationResult.Whitelisted("Notch"));

        verify(rcon).execute(eq("whitelist add Notch"), anyInt());
        ArgumentCaptor<Player> saved = ArgumentCaptor.forClass(Player.class);
        verify(players, org.mockito.Mockito.times(2)).save(saved.capture());
        assertThat(saved.getValue().status()).isEqualTo(PlayerStatus.WHITELISTED);
    }

    @Test
    void rconFailureKeepsThePlayerPendingAndReportsIt() {
        when(verifier.verify("notch")).thenReturn(Optional.of(new VerifiedAccount("Notch", "u1")));
        when(players.findByDiscordId("1")).thenReturn(Optional.empty());
        whitelistIs(WhitelistStatus.OPEN);
        when(rcon.execute(anyString(), anyInt())).thenThrow(new RconException("down"));

        assertThat(service.register("1", "alex", "notch")).isEqualTo(new RegistrationResult.WhitelistFailed("Notch"));

        verify(players).save(any()); // nur das initiale Speichern als PENDING
    }

    @Test
    void openAddsAllPendingPlayersAndSkipsRconFailures() {
        Player a = player("1", "Alpha", PlayerStatus.PENDING);
        Player b = player("2", "Beta", PlayerStatus.PENDING);
        when(players.findByStatusOrderByIdAsc(PlayerStatus.PENDING)).thenReturn(List.of(a, b));
        doThrow(new RconException("boom")).when(rcon).execute(eq("whitelist add Alpha"), anyInt());

        BatchResult result = service.open();

        assertThat(result).isEqualTo(new BatchResult(1, 2));
        assertThat(a.status()).isEqualTo(PlayerStatus.PENDING);
        assertThat(b.status()).isEqualTo(PlayerStatus.WHITELISTED);
        ArgumentCaptor<BotState> saved = ArgumentCaptor.forClass(BotState.class);
        verify(state).save(saved.capture());
        assertThat(saved.getValue().content()).isEqualTo("OPEN");
    }

    @Test
    void closeAllRemovesEveryoneAndResetsThemToPending() {
        Player a = player("1", "Alpha", PlayerStatus.WHITELISTED);
        when(players.findAllByOrderByIdAsc()).thenReturn(List.of(a));

        assertThat(service.closeAll()).isEqualTo(new BatchResult(1, 1));

        verify(rcon).execute(eq("whitelist remove Alpha"), anyInt());
        assertThat(a.status()).isEqualTo(PlayerStatus.PENDING);
    }
}
