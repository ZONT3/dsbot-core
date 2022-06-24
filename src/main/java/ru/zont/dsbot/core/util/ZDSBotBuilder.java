package ru.zont.dsbot.core.util;

import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import ru.zont.dsbot.core.ZDSBot;
import ru.zont.dsbot.core.config.ZDSBBasicConfig;
import ru.zont.dsbot.core.config.ZDSBBotConfig;
import ru.zont.dsbot.core.config.ZDSBConfigManager;
import ru.zont.dsbot.core.commands.CommandAdapter;
import ru.zont.dsbot.core.listeners.GuildListenerAdapter;

import javax.security.auth.login.LoginException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.Consumer;

public class ZDSBotBuilder {
    private final JDABuilder jdaBuilder;
    private ZDSBConfigManager<? extends ZDSBBasicConfig, ? extends ZDSBBotConfig> config;
    private final ArrayList<Class<? extends CommandAdapter>> commandAdapters = new ArrayList<>();
    private final ArrayList<Class<? extends GuildListenerAdapter>> guildListeners = new ArrayList<>();

    public static ZDSBotBuilder createLight(String key) {
        return new ZDSBotBuilder(JDABuilder.createLight(key));
    }

    public ZDSBotBuilder(JDABuilder jdaBuilder) {
        this.jdaBuilder = jdaBuilder;
    }

    public ZDSBotBuilder onJdaBuilder(Consumer<JDABuilder> action) {
        action.accept(jdaBuilder);
        return this;
    }

    /**
     * Sets following intents for JDA:
     * <li>{@link GatewayIntent#GUILD_MESSAGES}</li>
     * <li>{@link GatewayIntent#DIRECT_MESSAGES}</li>
     * <li>{@link GatewayIntent#GUILD_MEMBERS}</li>
     */
    public ZDSBotBuilder addDefaultIntents() {
        jdaBuilder.enableIntents(
                GatewayIntent.GUILD_MESSAGES,
                GatewayIntent.DIRECT_MESSAGES,
                GatewayIntent.GUILD_MEMBERS
        );
        return this;
    }

    public ZDSBotBuilder setCacheAll() {
        jdaBuilder
                .setMemberCachePolicy(MemberCachePolicy.ALL)
                .setChunkingFilter(ChunkingFilter.ALL);
        return this;
    }

    /**
     * Alias for {@link JDABuilder#addEventListeners(Object...)}
     */
    public ZDSBotBuilder addListeners(Object... adapters) {
        jdaBuilder.addEventListeners(adapters);
        return this;
    }

    public final ZDSBotBuilder addCommandAdapters(Class<? extends CommandAdapter> adapter) {
        commandAdapters.add(adapter);
        return this;
    }

    public final ZDSBotBuilder addGuildListeners(Class<? extends GuildListenerAdapter> listener) {
        guildListeners.add(listener);
        return this;
    }

    @SafeVarargs
    public final ZDSBotBuilder addCommandAdapters(Class<? extends CommandAdapter>... adapters) {
        commandAdapters.addAll(Arrays.asList(adapters));
        return this;
    }

    @SafeVarargs
    public final ZDSBotBuilder addGuildListeners(Class<? extends GuildListenerAdapter>... listeners) {
        guildListeners.addAll(Arrays.asList(listeners));
        return this;
    }

    public <T extends ZDSBBasicConfig> ZDSBotBuilder defaultConfig(Class<T> configClass) {
        config = new ZDSBConfigManager<>("cfg", configClass);
        return this;
    }

    public ZDSBotBuilder defaultConfig() {
        config = new ZDSBConfigManager<>("cfg");
        return this;
    }

    public <A extends ZDSBBasicConfig, B extends ZDSBBotConfig> ZDSBotBuilder config(Class<A> configClass,
                                                                                     Class<B> botConfigClass) {
        config = new ZDSBConfigManager<>("cfg", configClass, botConfigClass);
        return this;
    }

    public ZDSBot build() throws LoginException, InterruptedException {
        if (config == null) defaultConfig();
        return new ZDSBot(jdaBuilder, config, commandAdapters, guildListeners);
    }
}
