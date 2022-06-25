package ru.zont.dsbot.core;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;
import ru.zont.dsbot.core.config.ZDSBBasicConfig;
import ru.zont.dsbot.core.config.ZDSBBotConfig;
import ru.zont.dsbot.core.config.ZDSBConfig;
import ru.zont.dsbot.core.util.DescribedException;
import ru.zont.dsbot.core.util.ZDSBotBuilder;

import javax.security.auth.login.LoginException;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import ru.zont.dsbot.core.commands.basic.*;
import ru.zont.dsbot.core.commands.execution.*;

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

    public static void main(String[] args) throws LoginException, InterruptedException, IOException {
        ZDSBot bot = mainBot(args[0]);
        bot.getJda().awaitReady();
        GuildContext context = bot.getGuildContext("331526118635208716");
        TextChannel channel = context.getGuild().getTextChannelById("450293189711101952");
        Message message = channel.retrieveMessageById("989619888496705556").complete();

        final ScheduledExecutorService ex = Executors.newScheduledThreadPool(3);
        final ExecutionManager manager = new ExecutionManager(bot);

        for (String s: List.of("scripts/test.py", "scripts/test2.py", "scripts/test3.py")) {
            final int id = manager.newProcess(new String[]{"python", "-u", s}, null, channel, true);
            ex.schedule(() -> manager.killProcess(id, false), 10, TimeUnit.SECONDS);
        }
    }

    private static ZDSBot mainBot(String token) throws LoginException, InterruptedException {
        return ZDSBotBuilder.createLight(token)
                .config(Config.class, BotConfig.class)
                .addCommandAdapters(Ping.class, Help.class, Exec.class)
                .addDefaultIntents()
                .setCacheAll()
                .build();
    }
}
