package ru.zont.dsbot.core.listeners;

import net.dv8tion.jda.api.events.guild.GuildReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import ru.zont.dsbot.core.ZDSBot;

public class GuildContextListener extends ListenerAdapter {
    private final ZDSBot bot;

    public GuildContextListener(ZDSBot bot) {
        this.bot = bot;
    }

    @Override
    public void onGuildReady(@NotNull GuildReadyEvent event) {
        bot.updateGuildContext(event.getGuild());
    }
}
