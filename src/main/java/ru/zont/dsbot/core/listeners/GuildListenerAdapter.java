package ru.zont.dsbot.core.listeners;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.hooks.EventListener;
import org.jetbrains.annotations.NotNull;
import ru.zont.dsbot.core.GuildContext;
import ru.zont.dsbot.core.ZDSBot;
import ru.zont.dsbot.core.util.Reflect;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;

public abstract class GuildListenerAdapter implements EventListener {
    private final GuildContext context;

    public GuildListenerAdapter(GuildContext context) {
        this.context = context;
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
}
