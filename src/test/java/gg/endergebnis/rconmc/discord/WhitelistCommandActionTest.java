package gg.endergebnis.rconmc.discord;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.concurrent.ExecutorService;

import gg.endergebnis.rconmc.discord.command.WhitelistCommandAction;
import gg.endergebnis.rconmc.discord.listener.OnSlashCommandInteractionListener;
import gg.endergebnis.rconmc.whitelist.WhitelistService;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;

/** Treibt den Slash-Command-Listener mit Mockito-Mocks der JDA-Typen, ohne Gateway-Verbindung. */
class WhitelistCommandActionTest {

    private static final long ADMIN_ROLE = 4711L;

    private final WhitelistService service = mock(WhitelistService.class);
    private final ExecutorService executor = mock(ExecutorService.class);
    private final DiscordProperties properties = new DiscordProperties("t", 1L, 2L, ADMIN_ROLE, null);
    private final OnSlashCommandInteractionListener listener =
            new OnSlashCommandInteractionListener(new WhitelistCommandAction(properties, service, executor));

    private SlashCommandInteractionEvent event(String sub, Member member) {
        SlashCommandInteractionEvent event = mock(SlashCommandInteractionEvent.class);
        when(event.getName()).thenReturn(WhitelistCommandAction.COMMAND);
        when(event.getSubcommandName()).thenReturn(sub);
        when(event.getGuild()).thenReturn(mock(Guild.class));
        when(event.getMember()).thenReturn(member);
        when(event.reply(anyString())).thenReturn(mock(ReplyCallbackAction.class, Answers.RETURNS_SELF));
        return event;
    }

    @Test
    void ignoresOtherCommands() {
        SlashCommandInteractionEvent event = mock(SlashCommandInteractionEvent.class);
        when(event.getName()).thenReturn("other");

        listener.onSlashCommandInteraction(event);

        verifyNoInteractions(service);
    }

    @Test
    void rejectsMembersWithoutAdminRightsAndTouchesNothing() {
        Member member = mock(Member.class);
        when(member.hasPermission(Permission.MANAGE_SERVER)).thenReturn(false);
        when(member.getRoles()).thenReturn(List.of());
        SlashCommandInteractionEvent event = event(WhitelistCommandAction.SUB_CLOSE, member);

        listener.onSlashCommandInteraction(event);

        verify(event).reply("Keine Berechtigung.");
        verify(service, never()).close();
    }

    @Test
    void membersWithTheConfiguredAdminRoleMayRunCommands() {
        Role role = mock(Role.class);
        when(role.getIdLong()).thenReturn(ADMIN_ROLE);
        Member member = mock(Member.class);
        when(member.hasPermission(Permission.MANAGE_SERVER)).thenReturn(false);
        when(member.getRoles()).thenReturn(List.of(role));
        SlashCommandInteractionEvent event = event(WhitelistCommandAction.SUB_CLOSE, member);

        listener.onSlashCommandInteraction(event);

        verify(service).close();
    }

    @Test
    void membersWithManageServerMayRunCommands() {
        Member member = mock(Member.class);
        when(member.hasPermission(Permission.MANAGE_SERVER)).thenReturn(true);
        SlashCommandInteractionEvent event = event(WhitelistCommandAction.SUB_CLOSE, member);

        listener.onSlashCommandInteraction(event);

        verify(service).close();
    }
}
