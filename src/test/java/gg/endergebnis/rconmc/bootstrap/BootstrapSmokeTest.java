package gg.endergebnis.rconmc.bootstrap;

import static org.assertj.core.api.Assertions.assertThat;

import gg.endergebnis.rconmc.RconMcApplication;
import gg.endergebnis.rconmc.discord.DiscordProperties;
import gg.endergebnis.rconmc.rcon.RconProperties;
import gg.endergebnis.rconmc.whitelist.WhitelistService;
import net.dv8tion.jda.api.JDA;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(
        classes = RconMcApplication.class,
        properties = {
            "rconmc.discord.token=test-discord-token",
            "rconmc.discord.guild-id=1",
            "rconmc.discord.channel-id=2",
            "rconmc.discord.admin-role-id=",
            "rconmc.discord.registration-message=",
            "rconmc.rcon.host=127.0.0.1",
            "rconmc.rcon.port=25575",
            "rconmc.rcon.password=pw",
            "spring.datasource.url=jdbc:h2:mem:smoke;DB_CLOSE_DELAY=-1"
        })
class BootstrapSmokeTest {

    // Ersetzt die echte JDA-Verbindung, damit der Test keinen Discord-Login versucht.
    @MockitoBean
    private JDA jda;

    @Autowired
    private DiscordProperties discordProperties;

    @Autowired
    private RconProperties rconProperties;

    @Autowired
    private WhitelistService whitelistService;

    @Test
    void contextStartsWithBoundConfigurationAndWiredService() {
        assertThat(discordProperties.guildId()).isEqualTo(1L);
        assertThat(discordProperties.adminRoleId()).isNull();
        assertThat(discordProperties.registrationMessage()).isEqualTo(DiscordProperties.DEFAULT_REGISTRATION_MESSAGE);
        assertThat(rconProperties.port()).isEqualTo(25575);
        assertThat(discordProperties.toString()).doesNotContain("test-discord-token");
        assertThat(whitelistService.whitelistStatus()).isNotNull();
    }
}
