package gg.endergebnis.rconmc.discord.command;

import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;
import java.util.function.Supplier;

import gg.endergebnis.rconmc.discord.DiscordProperties;
import gg.endergebnis.rconmc.discord.button.RegisterButton;
import gg.endergebnis.rconmc.player.Player;
import gg.endergebnis.rconmc.whitelist.BatchResult;
import gg.endergebnis.rconmc.whitelist.WhitelistService;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Der Slash-Command {@code /whitelist} mit allen Unterbefehlen. Alle Befehle sind auf Admins beschraenkt
 * ("Server verwalten" oder die konfigurierte Admin-Rolle); die Pruefung passiert im Handler, nicht ueber
 * Discord-Standardrechte, damit auch reine Rollen-Admins den Befehl nutzen koennen.
 */
@Component
public class WhitelistCommandAction {

    private static final Logger log = LoggerFactory.getLogger(WhitelistCommandAction.class);

    public static final String COMMAND = "whitelist";
    public static final String SUB_START = "start";
    public static final String SUB_OPEN = "open";
    public static final String SUB_CLOSE = "close";
    public static final String SUB_OPEN_ALL = "open-all";
    public static final String SUB_CLOSE_ALL = "close-all";
    public static final String SUB_USER = "user";
    public static final String SUB_DCUSER = "dcuser";
    public static final String OPTION_NAME = "name";

    private static final int EMBED_COLOR = 0x2ECC71;

    private final DiscordProperties properties;
    private final WhitelistService whitelistService;
    private final ExecutorService botExecutor;

    public WhitelistCommandAction(
            DiscordProperties properties, WhitelistService whitelistService, ExecutorService botExecutor) {
        this.properties = properties;
        this.whitelistService = whitelistService;
        this.botExecutor = botExecutor;
    }

    /** Die Command-Definition, die beim Start auf dem Zielserver registriert wird. */
    public static SlashCommandData definition() {
        return Commands.slash(COMMAND, "Verwaltung der Minecraft-Whitelist")
                .addSubcommands(
                        new SubcommandData(SUB_START, "Registrierungs-Nachricht posten"),
                        new SubcommandData(SUB_OPEN, "Whitelist öffnen und Pending-Spieler hinzufügen"),
                        new SubcommandData(SUB_CLOSE, "Whitelist schließen"),
                        new SubcommandData(SUB_OPEN_ALL, "Alle Spieler aus der DB zur Whitelist hinzufügen"),
                        new SubcommandData(SUB_CLOSE_ALL, "Alle Spieler aus der DB von der Whitelist entfernen"),
                        new SubcommandData(SUB_USER, "Discord-Nutzer zu einem Minecraft-Namen finden")
                                .addOption(OptionType.STRING, OPTION_NAME, "Minecraft-Name", true),
                        new SubcommandData(SUB_DCUSER, "Minecraft-Namen zu einem Discord-Nutzer finden")
                                .addOption(OptionType.STRING, OPTION_NAME, "Discord-Name", true));
    }

    public void handle(SlashCommandInteractionEvent event) {
        Member member = event.getMember();
        if (event.getGuild() == null || member == null) {
            reply(event, "Dieser Befehl funktioniert nur auf dem Server.");
            return;
        }
        if (!isAdmin(member)) {
            reply(event, "Keine Berechtigung.");
            return;
        }

        String sub = event.getSubcommandName();
        if (sub == null) {
            reply(event, "Unbekannter Unterbefehl.");
            return;
        }
        switch (sub) {
            case SUB_START -> start(event);
            case SUB_OPEN -> batch(event, whitelistService::open,
                    r -> "Whitelist geöffnet. %d/%d Spieler hinzugefügt.".formatted(r.succeeded(), r.total()));
            case SUB_CLOSE -> close(event);
            case SUB_OPEN_ALL -> batch(event, whitelistService::openAll,
                    r -> "%d/%d Spieler zur Whitelist hinzugefügt.".formatted(r.succeeded(), r.total()));
            case SUB_CLOSE_ALL -> batch(event, whitelistService::closeAll,
                    r -> "%d/%d Spieler von der Whitelist entfernt.".formatted(r.succeeded(), r.total()));
            case SUB_USER -> lookupByMinecraftName(event);
            case SUB_DCUSER -> lookupByDiscordName(event);
            default -> reply(event, "Unbekannter Unterbefehl.");
        }
    }

    boolean isAdmin(Member member) {
        if (member.hasPermission(Permission.MANAGE_SERVER)) {
            return true;
        }
        Long adminRoleId = properties.adminRoleId();
        return adminRoleId != null
                && member.getRoles().stream().anyMatch(role -> role.getIdLong() == adminRoleId);
    }

    private void start(SlashCommandInteractionEvent event) {
        TextChannel channel = event.getJDA().getTextChannelById(properties.channelId());
        if (channel == null) {
            reply(event, "Der konfigurierte Registrierungs-Kanal wurde nicht gefunden.");
            return;
        }
        event.deferReply(true).queue();
        channel.sendMessageEmbeds(new EmbedBuilder()
                        .setTitle("Minecraft-Whitelist")
                        .setDescription(properties.registrationMessage())
                        .setColor(EMBED_COLOR)
                        .build())
                .addComponents(ActionRow.of(Button.primary(RegisterButton.BUTTON_ID, "Registrieren")))
                .queue(
                        message -> {
                            whitelistService.rememberRegistrationMessage(message.getIdLong());
                            event.getHook().editOriginal("Registrierungs-Nachricht wurde gepostet.").queue();
                        },
                        failure -> {
                            log.error("Registrierungs-Nachricht konnte nicht gepostet werden", failure);
                            event.getHook()
                                    .editOriginal("Die Registrierungs-Nachricht konnte nicht gepostet werden.")
                                    .queue();
                        });
    }

    private void close(SlashCommandInteractionEvent event) {
        whitelistService.close();
        reply(event, "Whitelist geschlossen. Neue Registrierungen werden als 'pending' gespeichert.");
    }

    /** Sammelaktionen dauern (ein RCON-Aufruf je Spieler): Antwort zurueckstellen, auf dem Executor arbeiten. */
    private void batch(
            SlashCommandInteractionEvent event,
            Supplier<BatchResult> action,
            Function<BatchResult, String> message) {
        event.deferReply(true).queue();
        botExecutor.execute(() -> {
            String reply;
            try {
                reply = message.apply(action.get());
            } catch (RuntimeException e) {
                log.error("Sammelaktion fehlgeschlagen", e);
                reply = "Interner Fehler aufgetreten. Details stehen im Log.";
            }
            event.getHook().editOriginal(reply).queue();
        });
    }

    private void lookupByMinecraftName(SlashCommandInteractionEvent event) {
        String name = event.getOption(OPTION_NAME, option -> option.getAsString());
        if (name == null) {
            reply(event, "Bitte gib einen Minecraft-Namen an.");
            return;
        }
        reply(event, whitelistService.findByMcUsername(name)
                .map(p -> "Minecraft: **%s**\nDiscord: **%s** (`%s`)\nPlatform: %s\nStatus: %s"
                        .formatted(p.mcUsername(), p.discordUsername(), p.discordId(), p.platform(), statusLabel(p)))
                .orElse("Kein Eintrag für Minecraft-Name **%s**.".formatted(name)));
    }

    private void lookupByDiscordName(SlashCommandInteractionEvent event) {
        String name = event.getOption(OPTION_NAME, option -> option.getAsString());
        if (name == null) {
            reply(event, "Bitte gib einen Discord-Namen an.");
            return;
        }
        reply(event, whitelistService.findByDiscordUsername(name)
                .map(p -> "Discord: **%s** (`%s`)\nMinecraft: **%s**\nPlatform: %s\nStatus: %s"
                        .formatted(p.discordUsername(), p.discordId(), p.mcUsername(), p.platform(), statusLabel(p)))
                .orElse("Kein Eintrag für Discord-Namen **%s**.".formatted(name)));
    }

    private static String statusLabel(Player player) {
        return player.status().name().toLowerCase(Locale.ROOT);
    }

    private static void reply(SlashCommandInteractionEvent event, String content) {
        if (event.isAcknowledged()) {
            event.getHook().editOriginal(content).queue();
        } else {
            event.reply(content).setEphemeral(true).queue();
        }
    }
}
