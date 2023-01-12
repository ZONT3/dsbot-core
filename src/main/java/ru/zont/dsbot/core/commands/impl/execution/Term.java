package ru.zont.dsbot.core.commands.impl.execution;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.apache.commons.cli.Options;
import ru.zont.dsbot.core.GuildContext;
import ru.zont.dsbot.core.ZDSBot;
import ru.zont.dsbot.core.commands.Input;
import ru.zont.dsbot.core.commands.exceptions.InvalidSyntaxException;
import ru.zont.dsbot.core.util.ResponseTarget;

import java.util.List;

public class Term extends ExecBase {
    public Term(ZDSBot bot, GuildContext context) {
        super(bot, context);
    }

    @Override
    public void onCall(ResponseTarget replyTo, Input input, MessageReceivedEvent event, Object... params) {
        final String[] args = input.getCommandLine().getArgs();
        if (args.length < 1)
            throw InvalidSyntaxException.argument(1, "PID must be provided", this);

        String pidStr = args[0];
        if (!pidStr.matches("\\d+"))
            throw InvalidSyntaxException.argument(1, "PID must be an integer", this);

        final boolean success = getBot().getExecutionManager()
                .killProcess(Integer.parseInt(pidStr), input.getCommandLine().hasOption('f'));
        if (!success)
            throw InvalidSyntaxException.argument(1, "Process with such PID has not found. Probably already" +
                    " dead or you are using 'system' PID, but should use 'internal' instead", this);
    }

    @Override
    public Options getOptions() {
        return new Options()
                .addOption("f", "force", false, "Use 'force' termination. " +
                        "Probably wont change behaviour on Windows host");
    }

    @Override
    public String getName() {
        return "term";
    }

    @Override
    public List<String> getAliases() {
        return List.of("kill", "terminate");
    }

    @Override
    public String getShortDesc() {
        return "Terminate process created by `exec`";
    }

    @Override
    public String getArgsSyntax() {
        return "<pid>";
    }
}
