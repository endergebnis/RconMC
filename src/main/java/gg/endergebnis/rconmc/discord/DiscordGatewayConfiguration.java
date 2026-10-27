package gg.endergebnis.rconmc.discord;

import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import gg.endergebnis.rconmc.discord.command.WhitelistCommandAction;
import gg.endergebnis.rconmc.discord.listener.OnButtonInteractionListener;
import gg.endergebnis.rconmc.discord.listener.OnModalInteractionListener;
import gg.endergebnis.rconmc.discord.listener.OnSlashCommandInteractionListener;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Guild;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Baut die JDA-Gateway-Verbindung, haengt die Listener ein und registriert {@code /whitelist} auf dem
 * konfigurierten Server. Der GUILDS-Intent ist in JDA immer aktiv (kein Enum-Wert); damit kennt der Bot die Rollen
 * des Servers fuer die Admin-Pruefung. Weitere Intents (Member, Nachrichten) werden nicht gebraucht.
 *
 * <p>Der Start blockiert bis {@code READY}; ein falsches Token oder eine unbekannte Guild-ID bricht den
 * Anwendungsstart ab (fail fast).
 */
@Configuration
public class DiscordGatewayConfiguration {

    private static final Logger log = LoggerFactory.getLogger(DiscordGatewayConfiguration.class);

    /** Fuer langsame, blockierende Arbeit (Mojang-API, RCON), damit JDA-Eventthreads frei bleiben. */
    @Bean(destroyMethod = "close")
    ExecutorService botExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }

    @Bean(destroyMethod = "shutdown")
    JDA jda(
            DiscordProperties discordProperties,
            OnButtonInteractionListener buttonListener,
            OnModalInteractionListener modalListener,
            OnSlashCommandInteractionListener slashCommandListener)
            throws InterruptedException {
        JDA jda = JDABuilder.createLight(discordProperties.token(), Collections.emptyList())
                .addEventListeners(buttonListener, modalListener, slashCommandListener)
                .build()
                .awaitReady();

        log.info("Eingeloggt als {}", jda.getSelfUser().getName());

        Guild guild = jda.getGuildById(discordProperties.guildId());
        if (guild == null) {
            jda.shutdownNow();
            throw new IllegalStateException(
                    "Guild " + discordProperties.guildId() + " nicht gefunden - ist der Bot auf dem Server?");
        }
        guild.updateCommands()
                .addCommands(WhitelistCommandAction.definition())
                .queue(
                        commands -> log.info("{} Slash-Command(s) registriert", commands.size()),
                        failure -> log.error("Slash-Commands konnten nicht registriert werden", failure));
        return jda;
    }
}
