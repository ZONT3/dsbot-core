package ru.zont.dsbot.core;

import com.ibm.icu.text.Transliterator;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.zont.dsbot.core.commands.SlashCommandAdapter;
import ru.zont.dsbot.core.config.ZDSBContextConfig;
import ru.zont.dsbot.core.commands.CommandAdapter;
import ru.zont.dsbot.core.config.ZDSBBotConfig;
import ru.zont.dsbot.core.listeners.GuildListenerAdapter;
import ru.zont.dsbot.core.util.LiteJSON;
import ru.zont.dsbot.core.util.Reflect;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Supplier;

import static ru.zont.dsbot.core.ZDSBot.isCommandExcluded;

public class GuildContext {
    private static final Logger log = LoggerFactory.getLogger(GuildContext.class);
    private static final HashMap<String, HashMap<String, Object>> storage = new HashMap<>();

    private final ZDSBot bot;
    private final String guildId;
    private final String guildName;

    private final HashMap<String, CommandAdapter> commands = new HashMap<>();
    private final LinkedList<GuildListenerAdapter> listeners = new LinkedList<>();
    private final ErrorReporter errorReporter;
    private final HashSet<String> foreignBannedCommands;
    private final HashSet<String> guildsBannedCommands;

    public GuildContext(ZDSBot bot, Guild guild) {
        this.bot = bot;
        guildId = guild.getId();
        guildName = guild.getName();

        guildsBannedCommands = new HashSet<>();
        foreignBannedCommands = new HashSet<>();

        initCommandAdapters();
        initGuildListeners(bot);

        errorReporter = new ErrorReporter(this);
    }

    public static <T> T getInstanceGlobal(Class<T> clazz, Supplier<T> newInstance) {
        return getInstance(clazz.getName(), newInstance, null);
    }

    public static <T> T getInstanceGlobal(String id, Supplier<T> newInstance) {
        return getInstance(id, newInstance, null);
    }

    @SuppressWarnings("unchecked")
    public static <T> T getInstance(String id, Supplier<T> newInstance, @Nullable GuildContext context) {
        HashMap<String, Object> map = storage.getOrDefault(id, null);
        if (map == null) {
            map = new HashMap<>();
            storage.put(id, map);
        }

        final String guildId = context != null ? context.getGuildId() : "GLOBAL";
        T instance = (T) map.getOrDefault(guildId, null);
        if (instance == null) {
            instance = newInstance.get();
            if (instance == null) return null;
            map.put(guildId, instance);
        }
        return instance;
    }

    private void initCommandAdapters() {
        final LinkedList<SlashCommandData> slashCommands = new LinkedList<>();

        ZDSBBotConfig cfg = getBot().getConfig();
        List<String> excludedByConfig = cfg.excludedCommands.toList();

        for (Class<? extends CommandAdapter> klass : bot.getCommandAdapters()) {
            if (isCommandExcluded(cfg, excludedByConfig, klass)) continue;

            final CommandAdapter instance = Reflect.commonsNewInstance(klass,
                    "Cannot instantiate Command " + klass.getName(),
                    bot, this);

            if (!instance.allowGuilds()) {
                guildsBannedCommands.add(instance.getName());
                guildsBannedCommands.addAll(instance.getAliases());
                if (!(instance instanceof SlashCommandAdapter))
                    continue;
            }

            if (!instance.allowForeignGuilds() && !bot.getConfig().getApprovedGuilds().contains(guildId)) {
                foreignBannedCommands.add(instance.getName());
                foreignBannedCommands.addAll(instance.getAliases());
                continue;
            }

            if (instance instanceof SlashCommandAdapter sca)
                if (!sca.isGlobal())
                    slashCommands.add(sca.getSlashCommand());

            log.info(formatLog( "Command instantiated: %s", instance.getClass().getName()));
            commands.put(instance.getName(), instance);
        }

        log.info(formatLog("Slash commands: {}"), slashCommands.stream().map(CommandData::getName).toList());
        getGuild().updateCommands().addCommands(slashCommands).queue();
    }

    private void initGuildListeners(ZDSBot bot) {
        for (Class<? extends GuildListenerAdapter> klass: bot.getGuildListeners()) {
            final GuildListenerAdapter instance = Reflect.commonsNewInstance(klass,
                    "Cannot instantiate GuildListener " + klass.getName(),
                    bot, this);

            final List<String> allowedGuilds = instance.getAllowedGuilds();
            if (allowedGuilds == null || allowedGuilds != GuildListenerAdapter.ALLOW_ALL_GUILDS && !allowedGuilds.contains(getGuildId()))
                continue;

            log.info(formatLog( "GuildListener instantiated: %s", instance.getClass().getName()));
            listeners.add(instance);
        }
        GuildListenerAdapter.initAllListeners(listeners, bot.getJda()::addEventListener);
    }

    @SuppressWarnings("unchecked")
    @Nullable
    public <T extends CommandAdapter> T getCommandInstance(@Nonnull Class<T> clazz) {
        return (T) getCommands().values().stream()
                .filter(l -> l.getClass().equals(clazz))
                .findAny().orElse(null);
    }

    @SuppressWarnings("unchecked")
    @Nullable
    public <T extends GuildListenerAdapter> T getListenerInstance(@Nonnull Class<T> clazz) {
        return (T) getListeners().stream()
                .filter(l -> l.getClass().equals(clazz))
                .findAny().orElse(null);
    }

    public String getGuildNameNormalized() {
        final Transliterator t = Transliterator.getInstance("Any-Latin; NFD; Latin-ASCII");
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
        log.info("GuildContext [{}] {}", getGuildNameNormalized(), created ? "created" : "updated");
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

    public final <T extends ZDSBContextConfig> T getConfig() {
        return bot.getGuildConfig(getGuildId());
    }

    public final <T extends ZDSBContextConfig> T getGlobalConfig() {
        return bot.getGlobalConfig();
    }

    public HashMap<String, CommandAdapter> getCommands() {
        return commands;
    }

    public LinkedList<GuildListenerAdapter> getListeners() {
        return listeners;
    }

    public <T> T getInstance(Class<T> clazz, Supplier<T> newInstance) {
        return getInstance(clazz.getName(), newInstance, this);
    }
    public <T> T getInstance(String id, Supplier<T> newInstance) {
        return getInstance(id, newInstance, this);
    }

    public LiteJSON getLJInstance(String name) {
        if (name == null || name.isBlank())
            throw new IllegalArgumentException("Name must be set");
        return getInstance("lj-%s".formatted(name), () -> new LiteJSON("%s-%s".formatted(name, getGuildId())));
    }

    public String formatLog(String s, Object... args) {
        return String.join(" ", "[%s]".formatted(getGuildNameNormalized()), String.format(s, args));
    }

    public MessageChannel findLogChannel() {
        String channelId = getConfig().logChannel.getString();
        MessageChannel channel = findChannel(channelId);
        if (channel == null && channelId != null)
            channel = getBot().getJda().getTextChannelById(channelId);

        try {
            if (channel == null && !getConfig().doSkipSearchingLogChannel()) {
                log.info(formatLog("Config channel not found, searching for any suitable..."));
                channel = getGuild().getSystemChannel();
                if (channel == null && getConfig().doTryDefaultChannelAsLog())
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

    public MessageChannel findChannel(String id) {
        if (id == null) return null;
        id = id.trim();
        if (id.isEmpty() || !id.matches("\\d+") || Long.parseLong(id) <= 0)
            return null;
        MessageChannel channel = getGuild().getTextChannelById(id);
        if (channel == null)
            channel = getGuild().getNewsChannelById(id);
        if (channel == null)
            channel = getGuild().getThreadChannelById(id);
        return channel;
    }

    public boolean isForeign() {
        return !getBot().getConfig().getApprovedGuilds().contains(getGuildId());
    }

    public boolean isGuildBannedCommand(String commandCall) {
        return guildsBannedCommands.contains(commandCall);
    }

    public boolean isForeignBannedCommand(String commandCall) {
        return foreignBannedCommands.contains(commandCall);
    }
}
