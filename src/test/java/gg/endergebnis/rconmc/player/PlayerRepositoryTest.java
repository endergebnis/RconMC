package gg.endergebnis.rconmc.player;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

/** Prueft das JPA-Mapping gegen eine echte In-Memory-Datenbank. */
@DataJpaTest
class PlayerRepositoryTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    @Autowired
    private PlayerRepository repository;

    private Player save(String discordId, String discordName, String mcName, String uuid) {
        Player player = new Player(discordId, NOW);
        player.register(discordName, mcName, uuid, NOW);
        return repository.saveAndFlush(player);
    }

    @Test
    void lookupsIgnoreCase() {
        save("1", "Alex", "Notch", "uuid-1");

        assertThat(repository.findFirstByMcUsernameIgnoreCase("nOtCh")).isPresent();
        assertThat(repository.findFirstByDiscordUsernameIgnoreCase("ALEX")).isPresent();
        assertThat(repository.findFirstByMcUsernameIgnoreCase("jeb_")).isEmpty();
    }

    @Test
    void detectsUuidUsedByAnotherDiscordUserButNotByTheSameOne() {
        save("1", "alex", "Notch", "uuid-1");

        assertThat(repository.existsByMcUuidAndDiscordIdNot("uuid-1", "2")).isTrue();
        assertThat(repository.existsByMcUuidAndDiscordIdNot("uuid-1", "1")).isFalse();
    }

    @Test
    void reRegisteringTheSameDiscordUserResetsStatusToPending() {
        Player player = save("1", "alex", "Notch", "uuid-1");
        player.status(PlayerStatus.WHITELISTED, NOW);
        repository.saveAndFlush(player);

        Player again = repository.findByDiscordId("1").orElseThrow();
        again.register("alex", "Notch2", "uuid-2", NOW.plusSeconds(60));
        repository.saveAndFlush(again);

        assertThat(repository.findAll()).hasSize(1);
        assertThat(repository.findByDiscordId("1")).hasValueSatisfying(p -> {
            assertThat(p.mcUsername()).isEqualTo("Notch2");
            assertThat(p.status()).isEqualTo(PlayerStatus.PENDING);
        });
    }

    @Test
    void pendingPlayersComeBackInRegistrationOrder() {
        save("1", "a", "First", "u1");
        Player second = save("2", "b", "Second", "u2");
        save("3", "c", "Third", "u3");
        second.status(PlayerStatus.WHITELISTED, NOW);
        repository.saveAndFlush(second);

        assertThat(repository.findByStatusOrderByIdAsc(PlayerStatus.PENDING))
                .extracting(Player::mcUsername)
                .containsExactly("First", "Third");
        assertThat(repository.findAllByOrderByIdAsc()).hasSize(3);
    }
}
