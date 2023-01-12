package ru.zont.dsbot.core.listeners;

import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.EventListener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.zont.dsbot.core.ErrorReporter;
import ru.zont.dsbot.core.GuildContext;
import ru.zont.dsbot.core.ZDSBot;
import ru.zont.dsbot.core.config.ZDSBBasicConfig;
import ru.zont.dsbot.core.config.ZDSBBotConfig;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public abstract class GuildListenerAdapter implements EventListener {
    private static final Logger log = LoggerFactory.getLogger(GuildListenerAdapter.class);
    public static final List<String> ALLOW_ALL_GUILDS = Collections.emptyList();

    public static final ExecutorService INIT_EXECUTOR_SERVICE = Executors.newCachedThreadPool();

    private final GuildContext context;
    private final ZDSBot bot;

    public GuildListenerAdapter(ZDSBot bot, GuildContext context) {
        this.context = context;
        this.bot = bot;
    }

    public boolean init(Guild guild) {
        return true;
    }

    public abstract void onEvent(Guild guild, GenericEvent event);

    public Set<Class<? extends GenericEvent>> getTypes() {
        return null;
    }

    public List<String> getAllowedGuilds() {
        return ALLOW_ALL_GUILDS;
    }

    public boolean allowGlobal() {
        return false;
    }

    public boolean doIgnoreEvents() {
        return false;
    }

    @Override
    public final void onEvent(@NotNull GenericEvent event) {
        if (doIgnoreEvents()) return;

        final Class<? extends @NotNull GenericEvent> clazz = event.getClass();
        if (getContext() != null && Arrays.stream(clazz.getMethods()).noneMatch(m -> "getGuild".equals(m.getName())))
            return;

        final Set<Class<? extends GenericEvent>> types = getTypes();
        if (types != null && !types.contains(event.getClass()))
            if (types.stream().noneMatch(e -> e.isInstance(event)))
                return;

        if (getContext() == null && event instanceof final MessageReceivedEvent e)
            if (!e.isFromType(ChannelType.PRIVATE))
                return;

        try {
            final Object guildObj = clazz.getMethod("getGuild").invoke(event);
            if (guildObj instanceof final Guild guild) {
                if (getContext() != null && guild.getId().equals(getContext().getGuildId())) {
                    // Guild object found and this is our GuildContext's guild
                    onEvent(guild, event);
                }
                return;
            }
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException ignored) { } // Not a guild containing event

        if (getContext() == null)
            onEvent(null, event);
    }

    public final void onEachGuild(Consumer<GuildContext> action) {
        getBot().getGuildContexts().forEach(action);
    }

    @Nullable
    public final GuildContext getContext() {
        return context;
    }

    public final ZDSBot getBot() {
        return bot;
    }

    public final ErrorReporter getErrorReporter() {
        if (getContext() != null)
            return getContext().getErrorReporter();
        return getBot().getErrorReporter();
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

    public final String formatLog(String str, Object... args) {
        return ZDSBot.formatLog(getContext(), str, args);
    }

    public static void initAllListeners(List<GuildListenerAdapter> list, Consumer<GuildListenerAdapter> onSuccess) {
        for (GuildListenerAdapter listener: list) {
            final GuildContext context = listener.getContext();
            INIT_EXECUTOR_SERVICE.submit(() -> {
                try {
                    if (listener.init(context == null ? null : context.getGuild())) {
                        log.info(listener.formatLog("GuildListener init done: %s", listener.getClass().getName()));
                        onSuccess.accept(listener);
                    } else
                        log.warn(listener.formatLog("GuildListener init silently failed: %s", listener.getClass().getName()));
                } catch (Exception e) {
                    log.error(listener.formatLog("Cannot init GuildListener: %s", listener.getClass().getName()), e);
                }
            });
        }
    }
}
