package ru.zont.dsbot.core;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import ru.zont.dsbot.core.config.ZDSBBasicConfig;
import ru.zont.dsbot.core.config.ZDSBBotConfig;
import ru.zont.dsbot.core.config.ZDSBConfig;
import ru.zont.dsbot.core.util.MessageBatch;
import ru.zont.dsbot.core.util.MessageSplitter;
import ru.zont.dsbot.core.util.ResponseTarget;
import ru.zont.dsbot.core.util.ZDSBotBuilder;

import javax.security.auth.login.LoginException;
import java.io.File;
import java.time.Instant;
import java.time.temporal.TemporalAccessor;

public class Exp {

    public static class Config extends ZDSBBasicConfig {
        public Config(String configName, File dir, ZDSBConfig inherit) {
            super(configName, dir, inherit);
            super.prefix = new Entry("zt.");
        }
    }

    public static class BotConfig extends ZDSBBotConfig {
        public BotConfig(String configName, File dir, ZDSBConfig inherit) {
            super(configName, dir, inherit);
            super.botName = new Entry("ZXC Bot");
        }
    }

    public static void main(String[] args) throws LoginException, InterruptedException {
        ZDSBot bot = mainBot(args[0]);
        bot.getJda().awaitReady();
        GuildContext context = bot.getGuildContext("331526118635208716");
        TextChannel channel = context.getGuild().getTextChannelById("450293189711101952");
        Message message = channel.retrieveMessageById("989619888496705556").complete();

        for (int i = 0; i < 100; i++) {
            context.getErrorReporter().reportError(
                    ResponseTarget.channel(channel), "лланвайрпллвурн", "гогергогогох.",
                    null, 0, new RuntimeException("Пиздец."));
            Thread.sleep(2500);
        }
    }

    private static ZDSBot mainBot(String token) throws LoginException, InterruptedException {
        return ZDSBotBuilder.createLight(token)
                .config(Config.class, BotConfig.class)
                .addDefaultIntents()
                .setCacheAll()
                .build();
    }
}
