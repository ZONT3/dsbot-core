package ru.zont.dsbot.core.listeners;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.hooks.EventListener;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.zont.dsbot.core.GuildContext;
import ru.zont.dsbot.core.ZDSBot;
import ru.zont.dsbot.core.config.ZDSBBasicConfig;
import ru.zont.dsbot.core.config.ZDSBBotConfig;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;

public abstract class GuildListenerAdapter implements EventListener {
    protected final Logger log;

    private final GuildContext context;

    public GuildListenerAdapter(GuildContext context) {
        this.context = context;
        log = LoggerFactory.getLogger("%s(%s)".formatted(getClass().getName(), getContext().getGuildNameNormalized()));
    }

    @Override
    public final void onEvent(@NotNull GenericEvent event) {
        final Class<? extends @NotNull GenericEvent> clazz = event.getClass();
        if (Arrays.stream(clazz.getMethods()).noneMatch(m -> "getGuild".equals(m.getName())))
            return;

        try {
            final Object guildObj = clazz.getMethod("getGuild").invoke(event);
            if (guildObj instanceof final Guild guild) {
                if (guild.getId().equals(getContext().getGuildId()))
                    onEvent(guild, event); // Guild object found and this is our GuildContext's guild
            }
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException ignored) { } // Not a guild containing event
    }

    public abstract void onEvent(@NotNull Guild guild, @NotNull GenericEvent event);

    public final GuildContext getContext() {
        return context;
    }

    public final ZDSBot getBot() {
        return context.getBot();
    }

    public final <T extends ZDSBBasicConfig> T getConfig() {
        return getContext().getConfig();
    }

    public final <T extends ZDSBBasicConfig> T getGlobalConfig() {
        return getContext().getGlobalConfig();
    }

    public final <T extends ZDSBBotConfig> T getBotConfig() {
        return getBot().getConfig();
    }

    public final String getPrefix() {
        return getConfig().getPrefix();
    }
}
