package ru.zont.dsbot.core;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.PrivateChannel;
import net.dv8tion.jda.api.entities.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.zont.dsbot.core.commands.CommandListener;
import ru.zont.dsbot.core.config.ZDSBBasicConfig;
import ru.zont.dsbot.core.config.ZDSBBotConfig;
import ru.zont.dsbot.core.config.ZDSBConfigManager;
import ru.zont.dsbot.core.commands.CommandAdapter;
import ru.zont.dsbot.core.executil.ExecutionManager;
import ru.zont.dsbot.core.listeners.GuildReadyListener;
import ru.zont.dsbot.core.listeners.GuildListenerAdapter;

import javax.security.auth.login.LoginException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Properties;

public class ZDSBot {
    private static final Logger log = LoggerFactory.getLogger(ZDSBot.class);

    private final String coreVersion;
    private final JDA jda;
    private final ZDSBConfigManager<? extends ZDSBBasicConfig, ? extends ZDSBBotConfig> configManager;
    private final HashMap<String, GuildContext> contextStore;
    private final ArrayList<Class<? extends CommandAdapter>> commandAdapters;
    private final ArrayList<Class<? extends GuildListenerAdapter>> guildListeners;
    private final String botVersion;
    private final String botNameLong;
    private final ErrorReporter errorReporter;
    private ExecutionManager executionManager;

    public ZDSBot(JDABuilder jdaBuilder,
                  ZDSBConfigManager<? extends ZDSBBasicConfig, ? extends ZDSBBotConfig> configManager,
                  ArrayList<Class<? extends CommandAdapter>> commandAdapters,
                  ArrayList<Class<? extends GuildListenerAdapter>> guildListeners)
            throws InterruptedException, LoginException {
        this(jdaBuilder, configManager, commandAdapters, guildListeners, null);
    }

    public ZDSBot(JDABuilder jdaBuilder,
                  ZDSBConfigManager<? extends ZDSBBasicConfig, ? extends ZDSBBotConfig> configManager,
                  ArrayList<Class<? extends CommandAdapter>> commandAdapters,
                  ArrayList<Class<? extends GuildListenerAdapter>> guildListeners, String botVersion)
            throws InterruptedException, LoginException {
        this.configManager = configManager;
        this.commandAdapters = commandAdapters;
        this.guildListeners = guildListeners;
        this.botVersion = botVersion;
        coreVersion = loadCoreVersion();
        contextStore = new HashMap<>();

        botNameLong = "%s%s (ZDSBot v.%s)".formatted(
                configManager.botConfig().getBotName(),
                botVersion != null ? (" " + botVersion) : "",
                coreVersion);

        configManager.setCommentsGetter(configName -> {
            if ("global".equals(configName))
                return botNameLong + " - Global config";
            else if ("config".equals(configName))
                return botNameLong + " - App config";
            else {
                final String id = configName.replaceFirst("guild-", "");
                final GuildContext context = contextStore.getOrDefault(id, null);
                if (context != null) {
                    return botNameLong + " - Guild '%s' config".formatted(context.getGuildNameNormalized());
                } else return botNameLong + " - %s config".formatted(configName);
            }
        });

        for (Class<? extends CommandAdapter> adapter : commandAdapters)
            log.info("CommandAdapter {} registered", adapter.getSimpleName());

        jdaBuilder.addEventListeners(new GuildReadyListener(this));
        jda = jdaBuilder.build();
        jda.awaitReady();

        errorReporter = new ErrorReporter(this);
        executionManager = new ExecutionManager(this);
    }

    public static String loadCoreVersion() {
        Properties properties = new Properties();
        try {
            properties.load(ZDSBot.class.getResourceAsStream("/version_core.properties"));
        } catch (Exception e) {
            log.error("Cannot load version config", e);
        }
        return properties.getProperty("version", "UNKNOWN");
    }

    public ArrayList<Class<? extends CommandAdapter>> getCommandAdapters() {
        return commandAdapters;
    }

    public ArrayList<Class<? extends GuildListenerAdapter>> getGuildListeners() {
        return guildListeners;
    }

    public String getCoreVersion() {
        return coreVersion;
    }

    public String getBotVersion() {
        return botVersion;
    }

    public String getBotNameLong() {
        return botNameLong;
    }

    public JDA getJda() {
        return jda;
    }

    @SuppressWarnings("unchecked")
    public final <T extends ZDSBBotConfig> T getConfig() {
        return (T) configManager.botConfig();
    }

    @SuppressWarnings("unchecked")
    public final <T extends ZDSBBasicConfig> T getGlobalConfig() {
        return (T) configManager.globalConfig();
    }

    @SuppressWarnings("unchecked")
    public final <T extends ZDSBBasicConfig> T getGuildConfig(String guildId) {
        return (T) configManager.guildConfig(guildId);
    }

    public final GuildContext getGuildContext(String guildId) {
        return contextStore.getOrDefault(guildId, null);
    }

    public final LinkedList<GuildContext> getGuildContexts() {
        return new LinkedList<>(contextStore.values());
    }

    public void updateGuildContext(Guild guild) {
        String id = guild.getId();
        GuildContext context;
        boolean notExist = !contextStore.containsKey(id);
        if (notExist) {
            context = new GuildContext(this, guild);
            getJda().addEventListener(new CommandListener(context));
            contextStore.put(id, context);
        } else context = contextStore.get(id);

        context.update(guild, notExist);
    }

    public MessageChannel findLogChannel() {
        for (String operator : getConfig().getOperators()) {
            try {
                User user = getJda().retrieveUserById(operator).complete();
                PrivateChannel privateChannel = user.openPrivateChannel().complete();
                if (privateChannel != null) {
                    return privateChannel;
                }
                log.warn("Cannot open PM with {}", user.getName());
            } catch (Throwable t) {
                log.warn("Cannot open PM with %s".formatted(operator), t);
            }
        }
        log.error("Cannot find log channel. OPs count: {}", getConfig().getOperators().size());
        return null;
    }

    public ErrorReporter getErrorReporter() {
        return errorReporter;
    }

    public ExecutionManager getExecutionManager() {
        return executionManager;
    }
}
