package gg.endergebnis.rconmc.player;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PlayerRepository extends JpaRepository<Player, Long> {

    Optional<Player> findByDiscordId(String discordId);

    Optional<Player> findFirstByMcUsernameIgnoreCase(String mcUsername);

    Optional<Player> findFirstByDiscordUsernameIgnoreCase(String discordUsername);

    /** {@code true}, wenn ein <em>anderer</em> Discord-Nutzer diese Minecraft-UUID bereits nutzt. */
    boolean existsByMcUuidAndDiscordIdNot(String mcUuid, String discordId);

    List<Player> findByStatusOrderByIdAsc(PlayerStatus status);

    List<Player> findAllByOrderByIdAsc();
}
