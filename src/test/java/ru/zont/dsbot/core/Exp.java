package ru.zont.dsbot.core;

import ru.zont.dsbot.core.config.ZDSBBasicConfig;
import ru.zont.dsbot.core.config.ZDSBBotConfig;
import ru.zont.dsbot.core.config.ZDSBConfig;
import ru.zont.dsbot.core.util.ZDSBotBuilder;

import javax.security.auth.login.LoginException;
import java.io.File;
import java.util.LinkedList;

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
        ZDSBot bot = ZDSBotBuilder.createLight(args[0])
                .config(Config.class, BotConfig.class)
                .addDefaultIntents()
                .setCacheAll()
                .build();

        while (true) {
            final LinkedList<GuildContext> guildContexts = bot.getGuildContexts();
            if (!bot.getGlobalConfig().wasUpdated("zxc"))
                if (guildContexts.stream().noneMatch(c -> c.getConfig().wasUpdated("zxc")))
                    continue;

            System.out.println("");
            System.out.printf("Global prefix: %s\n", bot.getGlobalConfig().getPrefix());
            for (GuildContext context: guildContexts) {
                System.out.printf("Guild '%s' prefix: %s\n", context.getGuildName(), context.getConfig().getPrefix());
            }

            guildContexts.forEach(c -> c.getConfig().wasUpdated("zxc"));
        }
    }
}
