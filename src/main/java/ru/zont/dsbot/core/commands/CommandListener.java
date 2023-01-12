package ru.zont.dsbot.core.commands;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import ru.zont.dsbot.core.GuildContext;
import ru.zont.dsbot.core.ZDSBot;
import ru.zont.dsbot.core.commands.exceptions.InsufficientPermissionsException;
import ru.zont.dsbot.core.listeners.GuildListenerAdapter;
import ru.zont.dsbot.core.util.DescribedException;
import ru.zont.dsbot.core.util.ResponseTarget;
import ru.zont.dsbot.core.util.Strings;

import java.util.Set;
import java.util.regex.Pattern;

public class CommandListener extends GuildListenerAdapter {
    public CommandListener(GuildContext context) {
        super(context.getBot(), context);
    }

    public CommandListener(ZDSBot bot) {
        super(bot, null);
    }

    @Override
    public Set<Class<? extends GenericEvent>> getTypes() {
        return Set.of(MessageReceivedEvent.class, SlashCommandInteractionEvent.class,
                CommandAutoCompleteInteractionEvent.class);
    }

    @Override
    public void onEvent(Guild guild, GenericEvent genericEvent) {
        if (genericEvent instanceof final SlashCommandInteractionEvent event) {
            ResponseTarget responseTarget = new ResponseTarget(event);
            wrapCommand(responseTarget, () -> handleCommandEvent(event, responseTarget));
        } else if (genericEvent instanceof final MessageReceivedEvent event) {
            ResponseTarget responseTarget = new ResponseTarget(event, getConfig());
            wrapCommand(responseTarget, () -> handleMessageEvent(event, responseTarget));
        } else if (genericEvent instanceof CommandAutoCompleteInteractionEvent event) {
            try {
                final SlashCommandAdapter adapter = CommandAdapter.findAndCheckSlashAdapter(getBot(), getContext(),
                        event.getName(), event.getCommandString().substring(1));
                adapter.onSlashCommandAutoComplete(event);
            } catch (InsufficientPermissionsException ignored) {
            } catch (Throwable e) {
                getErrorReporter().reportError(null,
                        Strings.CORE.get("err.unexpected"),
                        Strings.CORE.get("err.unexpected.foot"),
                        null, 0, e, true, false);
            }
        }
    }

    private void wrapCommand(ResponseTarget reportTo, Runnable handler) {
        try {
            handler.run();
        } catch (DescribedException e) {
            getErrorReporter().reportError(reportTo,
                    e.getTitle(),
                    e.getDescription(),
                    e.getPicture(),
                    e.getColor(),
                    e.getCause() == null ? e : e.getCause(),
                    e.getCause() != null && e != e.getCause(), true);
        } catch (Throwable e) {
            getErrorReporter().reportError(reportTo,
                    Strings.CORE.get("err.unexpected"),
                    Strings.CORE.get("err.unexpected.foot"),
                    null, 0, e, true, true);
        }
    }

    private void handleCommandEvent(SlashCommandInteractionEvent event, ResponseTarget responseTarget) {
        if (event.getUser().isBot()) return;

        event.deferReply().complete();
        final SlashCommandAdapter adapter = CommandAdapter.findAndCheckSlashAdapter(getBot(), getContext(),
                event.getName(), event.getCommandString().substring(1));

        if (adapter.isWriteableChannelRequired())
            CommandAdapter.requireWritableChannel(responseTarget);

        if (!adapter.getPermissionsUtil(event).checkOperator() && !adapter.checkPermission(event))
            throw new InsufficientPermissionsException();

        adapter.onSlashCommand(event);
    }

    private void handleMessageEvent(MessageReceivedEvent event, ResponseTarget responseTarget) {
        if (event.isWebhookMessage()) return;
        if (event.getAuthor().isBot()) return;

        final Message message = event.getMessage();
        final String contentMsg = message.getContentRaw();
        final boolean startsWith = contentMsg.startsWith(getConfig().getPrefix());
        if (!startsWith && getContext() != null)
            return;

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
