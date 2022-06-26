package ru.zont.dsbot.core.commands.impl.basic;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import ru.zont.dsbot.core.GuildContext;
import ru.zont.dsbot.core.commands.CommandAdapter;
import ru.zont.dsbot.core.commands.Input;
import ru.zont.dsbot.core.util.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

public class Help extends CommandAdapter {

    public static final String LISTITEM_EXTENDED = "`%s` - %s\n`%s`";
    public static final String LISTITEM_LESS = " - `%s`: %s";
    public static final int HELP_COLOR = 0x1010A0;
    public static final Strings STR = Strings.CORE;

    public Help(GuildContext context) {
        super(context);
    }

    @Override
    public void onCall(ResponseTarget replyTo, Input input, MessageReceivedEvent event, Object... params) {
        CommandLine cl = input.getCommandLine();
        List<String> argList = cl.getArgList();

        if (argList.size() == 1) {
            CommandAdapter adapter = findAdapter(getContext(), argList.get(0), argList.get(0));
            if (adapter == null)
                throw new DescribedException(
                        STR.get("err.unknown_command"),
                        STR.get("comms.help.err.unknown_command"),
                        HELP_COLOR);
            help(adapter, event);

        } else if (argList.size() > 1) {
            LinkedList<String> badNames = new LinkedList<>();
            LinkedList<CommandAdapter> commands = new LinkedList<>();
            for (String s : argList) {
                CommandAdapter adapter = findAdapter(getContext(), s, s);
                if (adapter == null)
                    badNames.add(s);
                else commands.add(adapter);
            }

            if (commands.size() == 0)
                throw new DescribedException(
                        STR.get("comms.help.err.unknown_commands"),
                        STR.get("comms.help.err.unknown_commands.desc"),
                        HELP_COLOR);

            String prefix;
            if (badNames.size() > 0)
                prefix = STR.get("comms.help.list.not_found", String.join(", ", badNames));
            else prefix = null;

            helpAll(event, cl, commands, prefix);

        } else {
            helpAll(event, cl, getContext().getCommands().values(), null);
        }
    }

    private void help(CommandAdapter adapter, MessageReceivedEvent event) {
        EmbedBuilder builder = new EmbedBuilder()
                .setTitle(adapter.getName())
                .setColor(HELP_COLOR);
        MessageBatch.sendNow(getContext().getResponseTarget(event)
                .responseEmbed(MessageSplitter.embeds(adapter.getHelp(), builder)));
    }

    private void helpAll(MessageReceivedEvent event, CommandLine cl, Collection<CommandAdapter> commands, String prefix) {
        boolean less = cl.hasOption('l');
        boolean full = cl.hasOption('f');
        ArrayList<String> list = new ArrayList<>(commands.size());
        for (CommandAdapter cmd : commands) {
            list.add(full ? cmd.getHelp()
                    : (less ? LISTITEM_LESS : LISTITEM_EXTENDED)
                    .formatted(cmd.getName(), cmd.getShortDesc(), cmd.getSyntax()));
        }

        String helpList = String.join(full ? "\n\n" : (less ? "\n" : "\n\n"), list);
        String content = prefix != null ? String.join("\n\n", prefix, helpList) : helpList;
        EmbedBuilder builder = new EmbedBuilder()
                .setTitle(STR.get("comms.help.list.title"))
                .setColor(HELP_COLOR);
        MessageBatch.sendNow(getContext().getResponseTarget(event)
                .responseEmbed(MessageSplitter.embeds(content, builder)));
    }

    @Override
    public Options getOptions() {
        return new Options()
                .addOption("l", "less", false, "Compact list")
                .addOption("f", "full", false,
                        "Display full manual for each selected (or all available) commands");
    }

    @Override
    public String getArgsSyntax() {
        return "[command [command2 [command3 ... ]]]";
    }

    @Override
    public String getDescription() {
        return STR.get("comms.help.desc");
    }

    @Override
    public String getName() {
        return "help";
    }

    @Override
    public List<String> getAliases() {
        return List.of("man", "?");
    }

    @Override
    public String getShortDesc() {
        return STR.get("comms.help.shortdesc");
    }
}
