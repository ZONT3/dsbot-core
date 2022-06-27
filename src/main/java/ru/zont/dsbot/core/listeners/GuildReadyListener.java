package ru.zont.dsbot.core.listeners;

import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.events.guild.GuildReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import ru.zont.dsbot.core.ZDSBot;
import ru.zont.dsbot.core.commands.CommandListener;

public class GuildReadyListener extends ListenerAdapter {
    private final ZDSBot bot;

    public GuildReadyListener(ZDSBot bot) {
        this.bot = bot;
    }

    @Override
    public void onGuildReady(@NotNull GuildReadyEvent event) {
        bot.updateGuildContext(event.getGuild());
    }

    @Override
    public void onReady(@NotNull ReadyEvent event) {
        bot.getJda().addEventListener(new CommandListener(bot));
    }
}
