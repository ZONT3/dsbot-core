package ru.zont.dsbot.core;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.zont.dsbot.core.commands.CommandAdapter;
import ru.zont.dsbot.core.commands.CommandListener;
import ru.zont.dsbot.core.commands.SlashCommandAdapter;
import ru.zont.dsbot.core.commands.impl.execution.ExecBase;
import ru.zont.dsbot.core.config.ZDSBContextConfig;
import ru.zont.dsbot.core.config.ZDSBBotConfig;
import ru.zont.dsbot.core.config.ZDSBConfigManager;
import ru.zont.dsbot.core.executil.ExecutionManager;
import ru.zont.dsbot.core.listeners.GuildListenerAdapter;
import ru.zont.dsbot.core.listeners.GuildReadyListener;
import ru.zont.dsbot.core.util.DBConnectionHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.security.auth.login.LoginException;
import java.sql.SQLException;
import java.util.*;

public class ZDSBot {
    private static final Logger log = LoggerFactory.getLogger(ZDSBot.class);

    private final String coreVersion;
    private final JDA jda;
    private final ZDSBConfigManager<? extends ZDSBContextConfig, ? extends ZDSBBotConfig> configManager;
    private final HashMap<String, GuildContext> contextStore;
    private final ArrayList<Class<? extends CommandAdapter>> commandAdapters;
    private final ArrayList<Class<? extends GuildListenerAdapter>> guildListeners;
    private final String botVersion;
    private final String botNameLong;
    private final ErrorReporter errorReporter;

    private final HashMap<String, CommandAdapter> commandsGlobal;
    private final ArrayList<GuildListenerAdapter> guildListenersGlobal;

    private final ExecutionManager executionManager;
    private final HashSet<String> globalBannedCommands;

    private DBConnectionHandler dbConnectionHandler = null;
    private final LinkedList<SlashCommandData> globalSlashCommands;

    public ZDSBot(JDABuilder jdaBuilder,
                  ZDSBConfigManager<? extends ZDSBContextConfig, ? extends ZDSBBotConfig> configManager,
                  ArrayList<Class<? extends CommandAdapter>> commandAdapters,
                  ArrayList<Class<? extends GuildListenerAdapter>> guildListeners)
            throws InterruptedException, LoginException {
        this(jdaBuilder, configManager, commandAdapters, guildListeners, null);
    }

    public ZDSBot(JDABuilder jdaBuilder,
                  ZDSBConfigManager<? extends ZDSBContextConfig, ? extends ZDSBBotConfig> configManager,
                  ArrayList<Class<? extends CommandAdapter>> commandAdapters,
                  ArrayList<Class<? extends GuildListenerAdapter>> guildListeners, String botVersion)
            throws InterruptedException, LoginException {
        this.configManager = configManager;
        this.commandAdapters = commandAdapters;
        this.guildListeners = guildListeners;
        this.botVersion = botVersion;
        coreVersion = loadCoreVersion();
        contextStore = new HashMap<>();
        globalSlashCommands = new LinkedList<>();

        botNameLong = "%s%s (ZDSBot v.%s)".formatted(
                configManager.botConfig().getBotName(),
                botVersion != null ? (" " + botVersion) : "",
                coreVersion);

        initConfigManager();

        jdaBuilder.addEventListeners(new GuildReadyListener(this));
        jda = jdaBuilder.build();

        globalBannedCommands = new HashSet<>();
        commandsGlobal = new HashMap<>();
        initGlobalCommandAdapters();

        guildListenersGlobal = new ArrayList<>(this.guildListeners.size());
        initGlobalGuildListeners(guildListeners);

        jda.awaitReady();

        errorReporter = new ErrorReporter(this);
        executionManager = new ExecutionManager(this);

        if (getMainGuildContext() == null)
            throw new IllegalStateException("Bot is not invited to the main guild or there are fatal error occurred on setup");

        log.info("Global slash commands: {}", globalSlashCommands.stream().map(CommandData::getName).toList());
        getJda().updateCommands().addCommands(globalSlashCommands).queue();
    }

    private void initConfigManager() {
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
    }

    private void initGlobalCommandAdapters() {
        ZDSBBotConfig cfg = getConfig();
        List<String> excludedByConfig = cfg.excludedCommands.toList();

        for (Class<? extends CommandAdapter> klass : this.commandAdapters) {
            final CommandAdapter instance;
            try {
                if (isCommandExcluded(cfg, excludedByConfig, klass)) continue;

                var constructor = klass.getDeclaredConstructor(ZDSBot.class, GuildContext.class);
                instance = constructor.newInstance(this, null);

                if (!instance.allowGlobal()) {
                    globalBannedCommands.add(instance.getName());
                    globalBannedCommands.addAll(instance.getAliases());
                    continue;
                }

                if (instance instanceof SlashCommandAdapter sca)
                    globalSlashCommands.add(sca.getSlashCommand());

                log.info(formatLog(null, "Command instantiated: %s", klass.getName()));
                commandsGlobal.put(instance.getName(), instance);
            } catch (Exception e) {
                throw new RuntimeException("Cannot instantiate Command " + klass.getName(), e);
            }
        }
    }

    private void initGlobalGuildListeners(ArrayList<Class<? extends GuildListenerAdapter>> guildListeners) {
        for (Class<? extends GuildListenerAdapter> klass : guildListeners) {
            final GuildListenerAdapter instance;
            try {
                var constructor = klass.getDeclaredConstructor(ZDSBot.class, GuildContext.class);
                instance = constructor.newInstance(this, null);

                if (!instance.allowGlobal())
                    continue;

                log.info(formatLog(null, "GuildListener instantiated: %s", klass.getName()));
                guildListenersGlobal.add(instance);
            } catch (Exception e) {
                throw new RuntimeException("Cannot instantiate GuildListener " + klass.getName(), e);
            }
        }
        GuildListenerAdapter.initAllListeners(guildListenersGlobal, getJda()::addEventListener);
    }

    public static boolean isCommandExcluded(ZDSBBotConfig cfg, List<String> excludedByConfig, Class<? extends CommandAdapter> klass) {
        if (ExecBase.class.isAssignableFrom(klass) && !cfg.allowExecution.isTrue()) {
            log.info("Execution command not allowed by config: {}", klass.getName());
            return true;
        }
        if (excludedByConfig.contains(klass.getSimpleName())) {
            log.info("Command not allowed by config: {}", klass.getName());
            return true;
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    @Nullable
    public <T extends CommandAdapter> T getGlobalCommandInstance(@Nonnull Class<T> clazz) {
        return (T) commandsGlobal.values().stream()
                .filter(l -> l.getClass().equals(clazz))
                .findAny().orElse(null);
    }

    @SuppressWarnings("unchecked")
    @Nullable
    public <T extends GuildListenerAdapter> T getGlobalListenerInstance(@Nonnull Class<T> clazz) {
        return (T) guildListenersGlobal.stream()
                .filter(l -> l.getClass().equals(clazz))
                .findAny().orElse(null);
    }

    public GuildContext getMainGuildContext() {
        return getGuildContext(getConfig().mainGuild.getValue());
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

    public static String formatLog(GuildContext context, String str, Object... args) {
        return context != null ? context.formatLog(str, args)
                : String.join(" ", "[GLOBAL]", str.formatted(args));
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
    public final <T extends ZDSBContextConfig> T getGlobalConfig() {
        return (T) configManager.globalConfig();
    }

    @SuppressWarnings("unchecked")
    public final <T extends ZDSBContextConfig> T getGuildConfig(String guildId) {
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

    public MessageChannel findChannelById(String id) {
        if (id == null) return null;
        id = id.trim();
        if (id.isEmpty() || !id.matches("\\d+") || Long.parseLong(id) <= 0)
            return null;
        MessageChannel channel = getJda().getTextChannelById(id);
        if (channel == null)
            channel = getJda().getNewsChannelById(id);
        if (channel == null)
            channel = getJda().getThreadChannelById(id);
        return channel;
    }

    public MessageChannel findLogChannel() {
        String channelId = getGlobalConfig().logChannel.getString();
        if (channelId != null) {
            TextChannel channel = getJda().getTextChannelById(channelId);
            if (channel != null)
                return channel;
        }

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

    public String getBotName() {
        return getConfig().getBotName();
    }

    public EmbedBuilder versionFooter(EmbedBuilder builder) {
        return builder.setFooter("%s v.%s ZDSB v.%s".formatted(
                getBotName(),
                getBotVersion(),
                getCoreVersion()));
    }

    public HashMap<String, CommandAdapter> getCommandsGlobal() {
        return commandsGlobal;
    }

    public ArrayList<GuildListenerAdapter> getGuildListenersGlobal() {
        return guildListenersGlobal;
    }

    public boolean isGlobalBannedCommand(String commandName) {
        return globalBannedCommands.contains(commandName);
    }

    public void setDbConnection(String conString) throws SQLException {
        dbConnectionHandler = new DBConnectionHandler(conString);
    }

    public DBConnectionHandler getDbConnectionHandler() {
        return dbConnectionHandler;
    }
}
