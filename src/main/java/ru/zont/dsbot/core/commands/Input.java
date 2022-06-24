package ru.zont.dsbot.core.commands;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.ParseException;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.zont.dsbot.core.GuildContext;
import ru.zont.dsbot.core.util.DescribedException;
import ru.zont.dsbot.core.util.Strings;

import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;

public class Input {
    private static final Logger log = LoggerFactory.getLogger(Input.class);

    private final String content;
    private final String comName;
    private final String rightover;

    private final DefaultParser defaultParser = new DefaultParser(false);

    private CommandLine commandLine = null;

    public Input(String content) {
        this.content = content;
        this.comName = content.split("\s", 2)[0];
        this.rightover = content.replaceFirst(Pattern.quote(comName) + "\s*", "");
    }

    public String getContent() {
        return content;
    }

    public String getComName() {
        return comName;
    }

    public String getRightover() {
        return rightover;
    }

    public CommandLine getCommandLine() {
        return commandLine;
    }

    private void applyAdapter(GuildContext context, CommandAdapter adapter) {
        String[] args = ArgumentTokenizer.tokenize(getRightover()).toArray(String[]::new);

        try {
            commandLine = defaultParser.parse(adapter.getOptions(), args, adapter.doStopAtNonOption());
        } catch (ParseException e) {
            if (adapter.doStopAtNonOption())
                log.error(context.formatLog("ParseException on parsing %s", adapter.getName()), e);
            else throw new InvalidSyntaxException(null, adapter);
        }
    }

    public CommandAdapter findAndApplyAdapter(GuildContext context) {
        CommandAdapter adapter = CommandAdapter.findAdapter(context, getComName(), getContent());
        if (adapter == null) throw new CommandNotFoundException();
        applyAdapter(context, adapter);
        return adapter;
    }
}
