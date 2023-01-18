package ru.zont.dsbot.core;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import ru.zont.dsbot.core.config.ZDSBContextConfig;
import ru.zont.dsbot.core.config.ZDSBBotConfig;
import ru.zont.dsbot.core.config.ZDSBConfig;
import ru.zont.dsbot.core.util.ZDSBotBuilder;

import javax.security.auth.login.LoginException;
import java.io.File;
import java.io.IOException;

public class Exp {

    public static class Config extends ZDSBContextConfig {
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

    public static void main(String[] args) throws LoginException, InterruptedException, IOException {
        ZDSBot bot = mainBot(args[0]);
        bot.getJda().awaitReady();
        GuildContext context = bot.getGuildContext("331526118635208716");
        TextChannel channel = context.getGuild().getTextChannelById("450293189711101952");
        Message message = channel.retrieveMessageById("989619888496705556").complete();
//        System.out.println(Charset.defaultCharset());
    }

    private static ZDSBot mainBot(String token) throws LoginException, InterruptedException {
        return ZDSBotBuilder.createLight(token)
                .config(Config.class, BotConfig.class)
                .allCoreCommands()
                .addDefaultIntents()
                .setCacheAll()
                .build();
    }
}
