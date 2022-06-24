package ru.zont.dsbot.core.commands;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.io.output.StringBuilderWriter;
import org.jetbrains.annotations.Nullable;
import ru.zont.dsbot.core.GuildContext;
import ru.zont.dsbot.core.ZDSBot;
import ru.zont.dsbot.core.config.ZDSBBasicConfig;
import ru.zont.dsbot.core.config.ZDSBBotConfig;
import ru.zont.dsbot.core.util.DescribedException;
import ru.zont.dsbot.core.util.Strings;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collections;
import java.util.HashMap;
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

    @Nullable
    public static CommandAdapter findAdapter(GuildContext context, String comName, String content) {
        HashMap<String, CommandAdapter> adapters = context.getCommands();
        CommandAdapter adapter = adapters.getOrDefault(comName, null);
        if (adapter == null || adapter.dontCallByName()) {
            final List<CommandAdapter> found = adapters.values()
                    .stream()
                    .filter(a -> a.isStringRepresentsThisCall(content))
                    .toList();

            if (found.size() > 1) {
                throw new AmbiguousCallException(found);
            } else if (found.size() == 1) {
                adapter = found.get(0);
            }
        }
        return adapter;
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

    public abstract void onCall(MessageReceivedEvent event, Input input, Object... params);

    public abstract String getName();

    public abstract String getShortDesc();

    public String getDescription() {
        return "";
    }

    public String getHelp() {
        String help = "```%s```\n%s%s".formatted(getSyntax(), getShortDesc(), getDescription().length() > 0 ? ("\n\n" + getDescription()) : "");
        try (StringBuilderWriter sbw = new StringBuilderWriter();
             PrintWriter pw = new PrintWriter(sbw)) {
            sbw.append(help);
            Options options = getOptions();
            if (options.getOptions().size() != 0) {
                sbw.append("\n```\n");
                helpFormatter.printOptions(pw, 45, options, 1, 2);
                sbw.append("\n```");
            }
            return sbw.toString();
        }
    }

    public String getCallableName() {
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
        helpFormatter.printUsage(pw, 45, getCallableName(), getOptions());

        StringBuilder sb = new StringBuilder(w.toString().replaceAll("\\s*\\n\\s*", ""));

        Routing routing = getRouting();

        if (routing != null) {
            sb.append(" ");
            sb.append(routing.routeVector());
        }

        if (getArgsSyntax() != null && (routing == null || routing.getDepth() == 1)) {
            sb.append(" ");
            sb.append(getArgsSyntax());
        }

        return sb.toString();
    }

    public Routing getRouting() {
        return null;
    }

    public String getArgsSyntax() {
        return null;
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

    private static class AmbiguousCallException extends DescribedException {
        public AmbiguousCallException(List<CommandAdapter> found) {
            super(Strings.CORE.get("err.ambiguous"), generateDescription(found));
        }

        private static String generateDescription(List<CommandAdapter> found) {
            StringBuilder desc = new StringBuilder(Strings.CORE.get("err.ambiguous.desc")).append("\n```");
            for (CommandAdapter a : found) {
                desc.append(" - ")
                        .append(a.getName())
                        .append(": ")
                        .append(a.getShortDesc())
                        .append("\n");
            }
            return desc.append("```").toString();
        }
    }

    protected final void call(String content, Object... params) {
        final Input input = new Input(content);
        CommandAdapter adapter = input.findAndApplyAdapter(getContext());
        adapter.onCall(null, input, params);
    }
}
