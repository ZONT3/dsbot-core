package ru.zont.dsbot.core.commands;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import ru.zont.dsbot.core.ErrorReporter;
import ru.zont.dsbot.core.GuildContext;
import ru.zont.dsbot.core.ZDSBot;
import ru.zont.dsbot.core.commands.exceptions.InsufficientPermissionsException;
import ru.zont.dsbot.core.listeners.GuildListenerAdapter;
import ru.zont.dsbot.core.util.DescribedException;
import ru.zont.dsbot.core.util.ResponseTarget;
import ru.zont.dsbot.core.util.Strings;

import java.util.regex.Pattern;

public class CommandListener extends GuildListenerAdapter {
    public CommandListener(GuildContext context) {
        super(context.getBot(), context);
    }

    public CommandListener(ZDSBot bot) {
        super(bot, null);
    }

    @Override
    public void onEvent(Guild guild, GenericEvent genericEvent) {
        if (genericEvent instanceof final MessageReceivedEvent event) {
            try {
                handleEvent(event);
            } catch (DescribedException e) {
                getErrorReporter().reportError(CommandAdapter.getResponseTarget(getConfig(), event),
                        e.getTitle(),
                        e.getDescription(),
                        e.getPicture(),
                        e.getColor(),
                        e.getCause() == null ? e : e.getCause(),
                        e.getCause() != null && e != e.getCause(), true);
            } catch (Throwable e) {
                getErrorReporter().reportError(CommandAdapter.getResponseTarget(getConfig(), event),
                        Strings.CORE.get("err.unexpected"),
                        Strings.CORE.get("err.unexpected.foot"),
                        null, 0, e, true, true);
            }
        }
    }

    private ErrorReporter getErrorReporter() {
        return getContext() != null ? getContext().getErrorReporter() : getBot().getErrorReporter();
    }

    private void handleEvent(MessageReceivedEvent event) {
        final Message message = event.getMessage();
        final String contentMsg = message.getContentDisplay();
        final boolean startsWith = contentMsg.startsWith(getConfig().getPrefix());
        if (!startsWith && getContext() != null)
            return;

        final ResponseTarget responseTarget = CommandAdapter.getResponseTarget(getConfig(), event);
        final Input input = new Input(startsWith ? stripPrefix(contentMsg) : contentMsg);
        final CommandAdapter adapter = input.findAndApplyAdapter(getBot(), getContext());

        if (adapter.isWriteableChannelRequired())
            CommandAdapter.requireWritableChannel(responseTarget);

        if (!adapter.getPermissionsUtil(event).checkOperator() && !adapter.checkPermission(event))
            throw new InsufficientPermissionsException();

        adapter.onCall(responseTarget, input, event);
    }

    private String stripPrefix(String inputString) {
        return inputString.replaceFirst(Pattern.quote(getPrefix()), "");
    }

}
