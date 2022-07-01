package ru.zont.dsbot.core.commands;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.io.output.StringBuilderWriter;
import org.jetbrains.annotations.Nullable;
import ru.zont.dsbot.core.GuildContext;
import ru.zont.dsbot.core.ZDSBot;
import ru.zont.dsbot.core.commands.exceptions.BotWritePermissionException;
import ru.zont.dsbot.core.config.ZDSBBasicConfig;
import ru.zont.dsbot.core.config.ZDSBBotConfig;
import ru.zont.dsbot.core.util.DescribedException;
import ru.zont.dsbot.core.util.ResponseTarget;
import ru.zont.dsbot.core.util.Strings;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public abstract class CommandAdapter {
    public static final String EMOJI_OK = "\u2705";
    public static final String EMOJI_WAIT = "\u23F3";
    public static final String EMOJI_ERROR = "U+1F6D1";

    private final ZDSBot bot;
    private final GuildContext context;

    private final HelpFormatter helpFormatter = new HelpFormatter() {{
        setSyntaxPrefix("");
        setWidth(Strings.DS_CODE_BLOCK_LINE_LENGTH);
    }};

    public CommandAdapter(ZDSBot bot, GuildContext context) {
        this.bot = bot;
        this.context = context;
    }

    public abstract void onCall(ResponseTarget replyTo, Input input, MessageReceivedEvent event, Object... params);

    public abstract String getName();

    public abstract String getShortDesc();

    public String getDescription() {
        return "";
    }

    public boolean checkPermission(MessageReceivedEvent event) {
        return true;
    }

    public boolean isWriteableChannelRequired() {
        return true;
    }

    public String getHelp() {
        String help = "```%s```\n%s%s".formatted(getSyntax(), getShortDesc(), getDescription().length() > 0 ? ("\n\n" + getDescription()) : "");
        try (StringBuilderWriter sbw = new StringBuilderWriter();
             PrintWriter pw = new PrintWriter(sbw)) {
            sbw.append(help);
            Options options = getOptions();
            if (options.getOptions().size() != 0) {
                sbw.append("\n```\n");
                helpFormatter.printOptions(pw, Strings.DS_CODE_BLOCK_LINE_LENGTH, options, 1, 2);
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

    public boolean allowForeignGuilds() {
        return true;
    }

    public boolean allowGlobal() {
        return true;
    }

    public boolean allowGuilds() {
        return true;
    }

    public boolean dontCallByName() {
        return false;
    }

    @Nullable
    public static CommandAdapter findAdapter(ZDSBot bot, GuildContext context, String comName, String content) {
        HashMap<String, CommandAdapter> adapters = context != null ? context.getCommands() : bot.getCommandsGlobal();
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

    @Nullable
    public final GuildContext getContext() {
        return context;
    }

    public final ZDSBot getBot() {
        return bot;
    }

    public final <T extends ZDSBBasicConfig> T getConfig() {
        return getContext() != null ? getContext().getConfig() : getGlobalConfig();
    }

    public final <T extends ZDSBBasicConfig> T getGlobalConfig() {
        return getBot().getGlobalConfig();
    }

    public final <T extends ZDSBBotConfig> T getBotConfig() {
        return getBot().getConfig();
    }

    public final PermissionsUtil getPermissionsUtil(MessageReceivedEvent event) {
        return new PermissionsUtil(getBot(), getContext(), event);
    }

    public final String getPrefix() {
        return getConfig().getPrefix();
    }

    protected final void addOK(MessageReceivedEvent event) {
        addOK(event, true);
    }

    protected final void addOK(MessageReceivedEvent event, boolean removeOther) {
        if (event != null) {
            final Message message = event.getMessage();
            if (removeOther) {
                message.removeReaction(EMOJI_WAIT).queue();
                message.removeReaction(EMOJI_ERROR).queue();
            }
            message.addReaction(EMOJI_OK).queue();
        }
    }

    protected final void addWaiting(MessageReceivedEvent event) {
        addWaiting(event, true);
    }

    protected final void addWaiting(MessageReceivedEvent event, boolean removeOther) {
        if (event != null) {
            final Message message = event.getMessage();
            if (removeOther) {
                message.removeReaction(EMOJI_OK).queue();
                message.removeReaction(EMOJI_ERROR).queue();
            }
            message.addReaction(EMOJI_WAIT).queue();
        }
    }

    protected final void addError(MessageReceivedEvent event) {
        addError(event, true);
    }

    protected final void addError(MessageReceivedEvent event, boolean removeOther) {
        if (event != null) {
            final Message message = event.getMessage();
            if (removeOther) {
                message.removeReaction(EMOJI_WAIT).queue();
                message.removeReaction(EMOJI_OK).queue();
            }
            message.addReaction(EMOJI_ERROR).queue();
        }
    }

    protected final void addResult(boolean result, MessageReceivedEvent event) {
        if (result) addOK(event);
        else addError(event);
    }

    /**
     * Call another command, like it was called from discord chat
     * @param content command string (<b>without</b> any command prefix like {@link ZDSBBasicConfig#getPrefix()})
     * @param params any params that will be passed in {@link CommandAdapter#onCall(ResponseTarget, Input, MessageReceivedEvent, Object...)} vararg.
     *               First vararg preferred to be an object that represents {@link ResponseTarget}. If so, first argument
     *               in {@code onCall} method will not be {@code null}. Valid objects are:
     *               <ul>
     *               <li>{@link ResponseTarget} itself</li>
     *               <li>{@link MessageChannel} or successors</li>
     *               <li>{@link Message} or successors</li>
     *               <li>{@link MessageReceivedEvent}</li>
     *               <li>{@link GenericEvent} implementation that contains {@code getMessage()} or {@code getChannel()} methods</li>
     *               </ul>
     */
    protected final void call(String content, Object... params) {
        final Input input = new Input(content);
        CommandAdapter adapter = input.findAndApplyAdapter(getBot(), getContext());
        ResponseTarget responseTarget;
        try {
            responseTarget = getResponseTarget(null, params);
        } catch (Exception ignored) {
            responseTarget = null;
        }
        adapter.onCall(responseTarget, input, null, params);
    }

    protected final ResponseTarget getResponseTarget(MessageReceivedEvent event, Object[] params) {
        if (event != null) return getResponseTarget(event);
        if (params.length >= 1) {
            if (params[0] instanceof final ResponseTarget target)
                return target;
            if (params[0] instanceof final MessageChannel channel)
                return ResponseTarget.channel(channel);
            if (params[0] instanceof final Message message)
                return ResponseTarget.message(message);
            if (params[0] instanceof final MessageReceivedEvent e)
                return getResponseTarget(e);
            if (params[0] instanceof final GenericEvent e) {
                Message message = null;
                try {
                    final Object msgObj = e.getClass().getMethod("getMessage").invoke(e);
                    if (msgObj instanceof final Message msg)
                        message = msg;
                } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException ignored) { }

                if (message != null && getConfig().doReplyToMessages())
                    return ResponseTarget.message(message);

                try {
                    final Object channelObj = e.getClass().getMethod("getChannel").invoke(e);
                    if (channelObj instanceof final MessageChannel channel)
                        return ResponseTarget.channel(channel);
                } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException ignored) { }

                if (message != null) return ResponseTarget.message(message);
            }
        }
        throw new IllegalStateException("Channel not provided by external call");
    }

    public final ResponseTarget getResponseTarget(MessageReceivedEvent event) {
        return getResponseTarget(getConfig(), event);
    }

    public static ResponseTarget getResponseTarget(ZDSBBasicConfig config, MessageReceivedEvent event) {
        if (config.doReplyToMessages()) {
            return new ResponseTarget(event.getMessage());
        } else {
            return new ResponseTarget(event.getChannel());
        }
    }

    public static void requireWritableChannel(ResponseTarget toCheck) {
        final boolean valid = ResponseTarget.isValid(toCheck);
        if (valid && !toCheck.getChannel().canTalk())
            throw new BotWritePermissionException();
        else if (!valid)
            throw new IllegalStateException("ResponseTarget must be valid for this command.");
    }
    public static class AmbiguousCallException extends DescribedException {

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
}
