package ru.zont.dsbot.core.commands.impl.execution;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import ru.zont.dsbot.core.GuildContext;
import ru.zont.dsbot.core.ZDSBot;
import ru.zont.dsbot.core.commands.Input;
import ru.zont.dsbot.core.commands.exceptions.InvalidSyntaxException;
import ru.zont.dsbot.core.executil.ExecutionManager;
import ru.zont.dsbot.core.util.ResponseTarget;

import java.io.PrintWriter;
import java.util.List;

public class Tell extends ExecBase {
    public Tell(ZDSBot bot, GuildContext context) {
        super(bot, context);
    }

    @Override
    public void onCall(ResponseTarget replyTo, Input input, MessageReceivedEvent event, Object... params) {
        final String[] args = input.getCommandLine().getArgs();
        if (args.length < 1)
            throw InvalidSyntaxException.argument(1, "PID must be provided", this);

        final String pidStr = args[0];
        final ExecutionManager manager = getBot().getExecutionManager();

        final PrintWriter stdin;
        final String content;
        final int pid;
        if (!pidStr.matches("\\d+")) {
            pid = manager.getLastOutputId(replyTo.getChannel().getId());
            if (pid > 0) stdin = manager.getStdin(pid);
            else stdin = null;

            if (stdin == null || stdin.checkError())
                throw InvalidSyntaxException.argument(1, "PID must be an integer, or any alive process must be in this channel", this);

            content = input.getContent();
        } else {
            content = input.getContent().replaceFirst("\\d+\\s", "");
            pid = Integer.parseInt(pidStr);
            stdin = manager.getStdin(pid);
        }

        if (stdin == null || stdin.checkError()) {
            throw InvalidSyntaxException.argument(1, "Process with such PID (%s) has not found. Probably already dead or you are using 'system' PID, but should use 'internal' instead".formatted(pid), this);
        }

        stdin.println(content);
    }

    @Override
    public String getName() {
        return "tell";
    }

    @Override
    public List<String> getAliases() {
        return List.of("w");
    }

    @Override
    public String getArgsSyntax() {
        return "<pid> <input...>";
    }

    @Override
    public boolean isWriteableChannelRequired() {
        return false;
    }

    @Override
    public String getShortDesc() {
        return "Write input to stdin of process";
    }
}
