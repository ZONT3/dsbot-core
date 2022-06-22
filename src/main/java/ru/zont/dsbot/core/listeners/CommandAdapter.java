package ru.zont.dsbot.core.listeners;

import ru.zont.dsbot.core.GuildContext;
import ru.zont.dsbot.core.ZDSBot;
import ru.zont.dsbot.core.commands.Input;
import ru.zont.dsbot.core.config.ZDSBBasicConfig;
import ru.zont.dsbot.core.config.ZDSBBotConfig;

import java.util.Collections;
import java.util.List;

public abstract class CommandAdapter {
    private final GuildContext context;

    public CommandAdapter(GuildContext context) {
        this.context = context;
    }

    public final GuildContext getContext() {
        return context;
    }

    public final ZDSBot getBot() {
        return context.getBot();
    }

    public final <T extends ZDSBBasicConfig> T getConfig() {
        return getContext().getConfig();
    }

    public final <T extends ZDSBBasicConfig> T getGlobalConfig() {
        return getContext().getGlobalConfig();
    }

    public final <T extends ZDSBBotConfig> T getBotConfig() {
        return getBot().getConfig();
    }

    public final String getPrefix() {
        return getConfig().getPrefix();
    }

    public abstract void onCall(Input input);

    public abstract String getName();

    public List<String> getAliases() {
        return Collections.emptyList();
    }

    public boolean isStringRepresentsThisCall(String inputString) {
        if (inputString.startsWith(getName())) return true;

        for (String alias: getAliases())
            if (inputString.startsWith(alias)) return true;

        return false;
    }

    public boolean dontCallByName() {
        return false;
    }

    public boolean shouldCheckByRawContent() {
        return false;
    }
}
