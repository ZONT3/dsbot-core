package ru.zont.dsbot.core.commands;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.zont.dsbot.core.GuildContext;
import ru.zont.dsbot.core.ZDSBot;
import ru.zont.dsbot.core.commands.exceptions.InvalidSyntaxException;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Input {
    private static final Logger log = LoggerFactory.getLogger(Input.class);

    private final String content;
    private final String comName;
    private final String contentStripped;

    private final DefaultParser defaultParser = new DefaultParser(false);

    private CommandLine commandLine = null;

    public Input(String content) {
        this.content = content;
        this.comName = content.split("\s", 2)[0];
        this.contentStripped = content.replaceFirst(Pattern.quote(comName) + "\s*", "");
    }

    public String getContentFull() {
        return content;
    }

    public String getComName() {
        return comName;
    }

    public String getContent() {
        return contentStripped;
    }

    public CommandLine getCommandLine() {
        return commandLine;
    }

    public String getContentUnrecognized() {
        if (commandLine.getArgs().length > 0) {
            final String sep = "[\\s\"']+";
            final Pattern pattern = Pattern.compile(
                    commandLine.getArgList().stream().map(Pattern::quote).collect(Collectors.joining(sep)));
            final Matcher matcher = pattern.matcher(content);
            if (matcher.find()) return matcher.group();
            else return content.replaceFirst(".+(?=%s)".formatted(Pattern.quote(commandLine.getArgs()[0])), "");
        } else return "";
    }

    public void applyAdapter(GuildContext context, CommandAdapter adapter) {
        String[] args = ArgumentTokenizer.tokenize(getContent()).toArray(String[]::new);

        try {
            commandLine = defaultParser.parse(adapter.getOptions(), args, adapter.doStopAtNonOption());
        } catch (ParseException e) {
            if (adapter.doStopAtNonOption())
                log.error(ZDSBot.formatLog(context, "ParseException on parsing %s", adapter.getName()), e);
            else throw new InvalidSyntaxException(null, adapter);
        }
    }

    public CommandAdapter findAndApplyAdapter(ZDSBot bot, GuildContext context) {
        CommandAdapter adapter = CommandAdapter.findAndCheckAdapter(bot, context, getComName(), getContentFull(), false);
        applyAdapter(context, adapter);
        return adapter;
    }

}
