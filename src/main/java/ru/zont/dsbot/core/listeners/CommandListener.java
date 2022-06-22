package ru.zont.dsbot.core.listeners;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.jetbrains.annotations.NotNull;
import ru.zont.dsbot.core.GuildContext;
import ru.zont.dsbot.core.commands.CommandNotFoundException;
import ru.zont.dsbot.core.commands.DescribedException;
import ru.zont.dsbot.core.commands.NotImplementedException;
import ru.zont.dsbot.core.util.Strings;

import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;

public class CommandListener extends GuildListenerAdapter {
    public CommandListener(GuildContext context) {
        super(context);
    }

    @Override
    public void onEvent(@NotNull Guild guild, @NotNull GenericEvent genericEvent) {
        if (genericEvent instanceof final MessageReceivedEvent event) {
            try {
                handleEvent(event);
            } catch (CommandNotFoundException | NotImplementedException ignored) {
            } catch (DescribedException e) {
                log.info("DescribedException from user %s, command string: %s".formatted(
                        event.getAuthor().getAsTag(),
                        event.getMessage().getContentRaw()
                ));
                log.info("%s: %s".formatted(e.getTitle(), e.getDescription()), e);
                getContext().getErrorReporter().reportError(event.getChannel(), event.getMessage(), e)
            } catch (Throwable e) {
                getContext().getErrorReporter().reportError(
                        event.getChannel(),
                        event.getMessage(),
                        e,
                        Strings.CORE.get("err.unexpected"),
                        e.getMessage());
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

            } else if (found.size() == 1) {
                adapter = found.get(0);
            } else {
                throw
            }
        }
    }

    private String stripPrefix(String inputString) {
        return inputString.replaceFirst(Pattern.quote(getPrefix()), "");
    }
}
