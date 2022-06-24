package ru.zont.dsbot.core.commands;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.io.output.StringBuilderWriter;
import ru.zont.dsbot.core.GuildContext;
import ru.zont.dsbot.core.ZDSBot;
import ru.zont.dsbot.core.config.ZDSBBasicConfig;
import ru.zont.dsbot.core.config.ZDSBBotConfig;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collections;
import java.util.List;

public abstract class CommandAdapter {
    private final GuildContext context;
    private final HelpFormatter helpFormatter = new HelpFormatter() {{
        setSyntaxPrefix("");
        setWidth(45);
    }};

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

    public abstract void onCall(MessageReceivedEvent event, String content, String[] args, CommandLine cl);

    public abstract String getName();

    public abstract String getShortDesc();

    public String getDescription() {
        return "";
    }

    public String getHelp() {
        String help = "`%s`\n\n%s%s".formatted(getSyntax(), getShortDesc(), getDescription().length() > 0 ? ("\n\n" + getDescription()) : "");
        String name = getCanonicalName();

        try (StringBuilderWriter sbw = new StringBuilderWriter();
             PrintWriter pw = new PrintWriter(sbw)) {
            sbw.append(help).append("```\n");
            helpFormatter.printHelp(pw, 45, name, help, getOptions(), 2, 0, "", false);
            sbw.append("\n```");
            return sbw.toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String getCanonicalName() {
        String name;
        if (dontCallByName() && getAliases().size() > 0)
            name = getAliases().get(0);
        else name = getName();
        return name;
    }

    public boolean doStopAtNonOption() {
        return true;
    }

    public Options getOptions() {
        return new Options();
    }

    public String getSyntax() {
        StringWriter w = new StringWriter();
        PrintWriter pw = new PrintWriter(w);
        helpFormatter.printUsage(pw, 45, getCanonicalName(), getOptions());
        return w.toString();
    }

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
}
