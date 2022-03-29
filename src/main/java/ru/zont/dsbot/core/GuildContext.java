package ru.zont.dsbot.core;

import net.dv8tion.jda.api.entities.Guild;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.zont.dsbot.core.config.ZDSBBasicConfig;
import ru.zont.dsbot.core.listeners.CommandAdapter;
import ru.zont.dsbot.core.listeners.GuildListenerAdapter;
import ru.zont.dsbot.core.util.Reflect;

public class GuildContext {
    private static final Logger log = LoggerFactory.getLogger(GuildContext.class);

    private final ZDSBot bot;
    private final String guildId;
    private final String guildName;

    public GuildContext(ZDSBot bot, Guild guild) {
        this.bot = bot;
        guildId = guild.getId();
        guildName = guild.getName();

        for (Class<? extends CommandAdapter> klass : bot.getCommandAdapters()) {
            final CommandAdapter instance = Reflect.commonsNewInstance(klass,
                    "Cannot instantiate GuildContext",
                    this);
            getBot().getJda().addEventListener(instance);
        }

        for (Class<? extends GuildListenerAdapter> klass: bot.getGuildListeners()) {
            final GuildListenerAdapter instance = Reflect.commonsNewInstance(klass,
                    "Cannot instantiate GuildContext",
                    this);
            getBot().getJda().addEventListener(instance);
        }
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
}
