package ru.zont.dsbot.core.listeners;

import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.EventListener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.zont.dsbot.core.GuildContext;
import ru.zont.dsbot.core.ZDSBot;
import ru.zont.dsbot.core.config.ZDSBBasicConfig;
import ru.zont.dsbot.core.config.ZDSBBotConfig;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;

public abstract class GuildListenerAdapter implements EventListener {
    private final GuildContext context;
    private final ZDSBot bot;

    public GuildListenerAdapter(ZDSBot bot, GuildContext context) {
        this.context = context;
        this.bot = bot;
    }

    @Override
    public final void onEvent(@NotNull GenericEvent event) {
        final Class<? extends @NotNull GenericEvent> clazz = event.getClass();
        if (getContext() != null && Arrays.stream(clazz.getMethods()).noneMatch(m -> "getGuild".equals(m.getName())))
            return;

        if (getContext() == null && event instanceof final MessageReceivedEvent e) {
            if (!e.isFromType(ChannelType.PRIVATE)) return;
            onEvent(null, e);
            return;
        } else if (getContext() == null) return;

        try {
            final Object guildObj = clazz.getMethod("getGuild").invoke(event);
            if (guildObj instanceof final Guild guild) {
                if (guild.getId().equals(getContext().getGuildId()))
                    onEvent(guild, event); // Guild object found and this is our GuildContext's guild
            }
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException ignored) { } // Not a guild containing event
    }

    public abstract void onEvent(Guild guild, GenericEvent event);

    @Nullable
    public final GuildContext getContext() {
        return context;
    }

    public final ZDSBot getBot() {
        return bot;
    }

    public final <T extends ZDSBBasicConfig> T getConfig() {
        return getContext() != null ? getContext().getConfig() : getGlobalConfig();
    }

    public final <T extends ZDSBBasicConfig> T getGlobalConfig() {
        return getBot().getGlobalConfig();
    }

    public final <T extends ZDSBBotConfig> T getBotConfig() {
        return getBot().getConfig();
    }

    public final String getPrefix() {
        return getConfig().getPrefix();
    }
}
