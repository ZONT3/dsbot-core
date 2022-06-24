package ru.zont.dsbot.core.commands;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.jetbrains.annotations.NotNull;
import ru.zont.dsbot.core.GuildContext;
import ru.zont.dsbot.core.listeners.GuildListenerAdapter;
import ru.zont.dsbot.core.util.DescribedException;
import ru.zont.dsbot.core.util.Strings;

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
            } catch (DescribedException e) {
                getContext().getErrorReporter().reportError(getContext().getResponseTarget(event),
                        e.getTitle(),
                        e.getDescription(),
                        e.getPicture(),
                        e.getColor(),
                        e.getCause() == null ? e : e.getCause(),
                        e.getCause() != null && e != e.getCause(), true);
            } catch (Throwable e) {
                getContext().getErrorReporter().reportError(getContext().getResponseTarget(event),
                        Strings.CORE.get("err.unexpected"),
                        Strings.CORE.get("err.unexpected.foot"),
                        null, 0, e, true, true);
            }
        }
    }

    private void handleEvent(MessageReceivedEvent event) {
        final Message message = event.getMessage();
        final String contentMsg = message.getContentDisplay();
        if (!contentMsg.startsWith(getConfig().getPrefix()))
            return;

        final Input input = new Input(stripPrefix(contentMsg));
        CommandAdapter adapter = input.findAndApplyAdapter(getContext());
        adapter.onCall(event, input);
    }

    private String stripPrefix(String inputString) {
        return inputString.replaceFirst(Pattern.quote(getPrefix()), "");
    }

}
