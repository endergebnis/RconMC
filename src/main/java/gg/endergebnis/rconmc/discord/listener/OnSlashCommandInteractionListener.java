package gg.endergebnis.rconmc.discord.listener;

import gg.endergebnis.rconmc.discord.command.WhitelistCommandAction;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.springframework.stereotype.Component;

/** Leitet {@code /whitelist} an {@link WhitelistCommandAction} weiter; andere Commands werden ignoriert. */
@Component
public class OnSlashCommandInteractionListener extends ListenerAdapter {

    private final WhitelistCommandAction whitelistCommand;

    public OnSlashCommandInteractionListener(WhitelistCommandAction whitelistCommand) {
        this.whitelistCommand = whitelistCommand;
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (WhitelistCommandAction.COMMAND.equals(event.getName())) {
            whitelistCommand.handle(event);
        }
    }
}
