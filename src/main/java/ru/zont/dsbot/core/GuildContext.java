package ru.zont.dsbot.core;

import com.ibm.icu.text.Transliterator;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.zont.dsbot.core.config.ZDSBBasicConfig;
import ru.zont.dsbot.core.commands.CommandAdapter;
import ru.zont.dsbot.core.listeners.GuildListenerAdapter;
import ru.zont.dsbot.core.util.Reflect;
import ru.zont.dsbot.core.util.ResponseTarget;

import java.util.HashMap;
import java.util.LinkedList;

public class GuildContext {
    private static final Logger log = LoggerFactory.getLogger(GuildContext.class);

    private final ZDSBot bot;
    private final String guildId;
    private final String guildName;

    private final HashMap<String, CommandAdapter> commands = new HashMap<>();
    private final LinkedList<GuildListenerAdapter> listeners = new LinkedList<>();
    private final ErrorReporter errorReporter;

    public GuildContext(ZDSBot bot, Guild guild) {
        this.bot = bot;
        guildId = guild.getId();
        guildName = guild.getName();

        for (Class<? extends CommandAdapter> klass : bot.getCommandAdapters()) {
            final CommandAdapter instance = Reflect.commonsNewInstance(klass,
                    "Cannot instantiate GuildContext",
                    this);
            commands.put(instance.getName(), instance);
        }

        for (Class<? extends GuildListenerAdapter> klass: bot.getGuildListeners()) {
            final GuildListenerAdapter instance = Reflect.commonsNewInstance(klass,
                    "Cannot instantiate GuildContext",
                    this);
            getBot().getJda().addEventListener(instance);
            listeners.add(instance);
        }

        errorReporter = new ErrorReporter(this);
    }

    public String getGuildNameNormalized() {
        final Transliterator t = Transliterator.getInstance("Any-Latin; NFD");
        return t.transliterate(getGuildName());
    }

    public ErrorReporter getErrorReporter() {
        return errorReporter;
    }

    /**
     * Called after {@link net.dv8tion.jda.api.events.guild.GuildReadyEvent}
     * (instantly after constructor {@link GuildContext}, or on reconnect)
     */
    public void update(Guild guild, boolean created) {
        log.info("GuildContext [{}] {}", guild.getName(), created ? "created" : "updated");
    }

    public ZDSBot getBot() {
        return bot;
    }

    public String getGuildId() {
        return guildId;
    }

    public String getGuildName() {
        return guildName;
    }

    public final Guild getGuild() {
        return bot.getJda().getGuildById(getGuildId());
    }

    public final <T extends ZDSBBasicConfig> T getConfig() {
        return bot.getGuildConfig(getGuildId());
    }

    public final <T extends ZDSBBasicConfig> T getGlobalConfig() {
        return bot.getGlobalConfig();
    }

    public HashMap<String, CommandAdapter> getCommands() {
        return commands;
    }

    public LinkedList<GuildListenerAdapter> getListeners() {
        return listeners;
    }

    public String formatLog(String s, Object... args) {
        return "[%s] ".formatted(getGuildNameNormalized())
                + String.format(s, args);
    }

    public ResponseTarget getResponseTarget(MessageReceivedEvent event) {
        if (getConfig().doReplyToMessages()) {
            return new ResponseTarget(event.getMessage());
        } else {
            return new ResponseTarget(event.getChannel());
        }
    }

    public MessageChannel findLogChannel() {
        MessageChannel channel = null;

        try {
            String value = getConfig().logChannel.getValue();
            if (!value.isEmpty() && value.matches("\\d+") && Long.parseLong(value) > 0)
                channel = getGuild().getTextChannelById(value);

            if (channel == null && !getConfig().doSkipSearchingLogChannel()) {
                log.info(formatLog("Config channel not found, searching for any suitable..."));
                channel = getGuild().getSystemChannel();
                if (channel == null)
                    channel = getGuild().getDefaultChannel();
            }

            if (channel == null) {
                log.warn(formatLog("Failed to find log channel. Falling to first of op's PM"));
                channel = getBot().findLogChannel();
            }
        } catch (Throwable t) {
            log.error(formatLog("Failed to find log channel"), t);
        }

        return channel;
    }
}
