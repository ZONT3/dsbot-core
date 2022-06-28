package ru.zont.dsbot.core.commands.impl.execution;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.apache.commons.cli.Options;
import ru.zont.dsbot.core.GuildContext;
import ru.zont.dsbot.core.ZDSBot;
import ru.zont.dsbot.core.commands.Input;
import ru.zont.dsbot.core.commands.exceptions.InvalidSyntaxException;
import ru.zont.dsbot.core.commands.exceptions.NotImplementedException;
import ru.zont.dsbot.core.executil.ExecutionManager;
import ru.zont.dsbot.core.util.ResponseTarget;

import java.io.File;
import java.util.List;

public class Do extends ExecBase {
    private static final List<String> supportedFormats = List.of("py", "bat", "cmd");

    public Do(ZDSBot bot, GuildContext context) {
        super(bot, context);
    }

    @Override
    public void onCall(ResponseTarget replyTo, Input input, MessageReceivedEvent event, Object... params) {
        final String[] args = input.getCommandLine().getArgs();
        if (args.length < 1)
            throw InvalidSyntaxException.argument(1, "PID must be provided", this);

        String script = args[0];
        final String scriptsDir = getBotConfig().scriptsDir.getValue();
        File scriptFile = new File(scriptsDir, script);
        if (!scriptFile.isFile()) {
            for (String format: supportedFormats) {
                scriptFile = new File(scriptsDir, String.join("", script, ".", format));
                if (scriptFile.isFile())
                    break;
            }
        }

        if (!scriptFile.isFile()) throw InvalidSyntaxException.argument(1, "Unknown script name", this);

        final String[] split = scriptFile.getName().split("\\.");
        final String ext = split[split.length - 1];

        addWaiting(event);
        final ExecutionManager manager = getBot().getExecutionManager();
        try {
            switch (ext) {
                case "py" -> {
                    final List<String> pArgs = List.of(getBotConfig().pythonPath.getValue(),
                            "-X", "utf8", "-u", scriptFile.getPath());
                    final boolean verbose = input.getCommandLine().hasOption('v');
                    manager.newProcess(pArgs.toArray(String[]::new),
                            script,
                            replyTo.getChannel(),
                            (i) -> addResult(i == 0, event),
                            verbose, true, !verbose);
                }
                case "cmd", "bat" -> throw new NotImplementedException("Windows CMD execution");
                default -> throw new InvalidSyntaxException("Unknown script format", this);
            }
        } catch (Throwable t) {
            addError(event);
            throw t;
        }
    }

    @Override
    public String getName() {
        return "do";
    }

    @Override
    public Options getOptions() {
        return new Options()
                .addOption("v", "verbose", false, "Provide verbose information");
    }

    @Override
    public String getArgsSyntax() {
        return "<script>";
    }

    @Override
    public String getShortDesc() {
        return "Run a script on sever";
    }
}
