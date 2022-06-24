package ru.zont.dsbot.core.commands;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.ParseException;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.zont.dsbot.core.GuildContext;
import ru.zont.dsbot.core.listeners.GuildListenerAdapter;
import ru.zont.dsbot.core.util.DescribedException;
import ru.zont.dsbot.core.util.Strings;

import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;

public class CommandListener extends GuildListenerAdapter {
    private static final Logger log = LoggerFactory.getLogger(CommandListener.class);

    private final DefaultParser defaultParser = new DefaultParser(false);

    public CommandListener(GuildContext context) {
        super(context);
    }

    @Override
    public void onEvent(@NotNull Guild guild, @NotNull GenericEvent genericEvent) {
        if (genericEvent instanceof final MessageReceivedEvent event) {
            try {
                handleEvent(event);
            } catch (DescribedException e) {
                getContext().getErrorReporter().reportError(getContext().getResponseTarget(event),
                        e.getTitle(),
                        e.getDescription(),
                        e.getPicture(),
                        e.getColor(),
                        e.getCause() != e ? e.getCause() : null);
            } catch (Throwable e) {
                getContext().getErrorReporter().reportError(getContext().getResponseTarget(event),
                        Strings.CORE.get("err.unexpected"),
                        Strings.CORE.get("err.unexpected.foot"),
                        null, 0, e);
            }
        }
    }

    private void handleEvent(MessageReceivedEvent event) {
        final Message message = event.getMessage();
        final String contentMsg = message.getContentDisplay();
        if (!contentMsg.startsWith(getConfig().getPrefix()))
            return;
        final String content = stripPrefix(contentMsg);

        final HashMap<String, CommandAdapter> adapters = getContext().getCommands();
        CommandAdapter adapter = adapters.getOrDefault(content.split(" ", 2)[0], null);
        if (adapter == null || adapter.dontCallByName()) {
            final List<CommandAdapter> found = adapters.values()
                    .stream()
                    .filter(a -> a.isStringRepresentsThisCall(content))
                    .toList();

            if (found.size() > 1) {
                StringBuilder desc = new StringBuilder(Strings.CORE.get("err.ambiguous.desc")).append("\n```");
                for (CommandAdapter a : found) {
                    desc.append(" - ")
                            .append(a.getName())
                            .append(": ")
                            .append(a.getShortDesc())
                            .append("\n");
                }
                throw new DescribedException(Strings.CORE.get("err.ambiguous"), desc.append("```").toString());
            } else if (found.size() == 1) {
                adapter = found.get(0);
            }
        }

        if (adapter == null) throw new CommandNotFoundException();

        String[] args = ArgumentTokenizer.tokenize(content).toArray(String[]::new);
        CommandLine cl = null;

        try {
            cl = defaultParser.parse(adapter.getOptions(), args, adapter.doStopAtNonOption());
        } catch (ParseException e) {
            if (adapter.doStopAtNonOption())
                log.error(getContext().formatLog("ParseException on parsing %s", adapter.getName()), e);
            else throw new InvalidSyntaxException(null, adapter);
        }

        adapter.onCall(event, content, args, cl);
    }

    private String stripPrefix(String inputString) {
        return inputString.replaceFirst(Pattern.quote(getPrefix()), "");
    }
}
