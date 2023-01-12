package ru.zont.dsbot.core.commands.impl.execution;

import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;
import ru.zont.dsbot.core.GuildContext;
import ru.zont.dsbot.core.ZDSBot;
import ru.zont.dsbot.core.commands.*;
import ru.zont.dsbot.core.commands.exceptions.InvalidSyntaxException;
import ru.zont.dsbot.core.commands.exceptions.NotImplementedException;
import ru.zont.dsbot.core.executil.ExecutionManager;
import ru.zont.dsbot.core.util.ResponseTarget;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Exec extends ExecBase {
    public static final Pattern CODE_BLOCK_LANG_PATTERN = Pattern.compile("```([\\w-]+)\\n((.|\\n)+)```\\W*");
    public static final Pattern CODE_BLOCK_PATTERN = Pattern.compile("```((.|\\n)+)```\\W*");
    private final HashMap<String, Env> envMap = new HashMap<>(){{
        put("python", Exec.this::pythonEnv);
        put("py", Exec.this::pythonEnv);
        put("cmd", Exec.this::cmdEnv);
        put("bat", Exec.this::cmdEnv);
        put("sh", Exec.this::shEnv);
        put("ps1", Exec.this::powershellEnv);
        put("ps", Exec.this::powershellEnv);
        put("javascript", Exec.this::javascriptEnv);
        put("js", Exec.this::javascriptEnv);
        put("java", Exec.this::javaEnv);
    }};

    public Exec(ZDSBot bot, GuildContext context) {
        super(bot, context);
    }

    @Override
    public void onCall(ResponseTarget replyTo, Input input, MessageReceivedEvent event, Object... params) {
        final CommandLine cl = input.getCommandLine();
        final String content = input.getContentUnrecognized();

        final Matcher labeledMatcher = CODE_BLOCK_LANG_PATTERN.matcher(content);
        final Matcher blankMatcher = CODE_BLOCK_PATTERN.matcher(content);

        if (!cl.hasOption('i') && labeledMatcher.matches()) {
            callEnv(cl, labeledMatcher.group(1), labeledMatcher.group(2), replyTo.getChannel(), event);

        } else if (cl.hasOption('i') || blankMatcher.matches()) {
            final String code;
            if (!blankMatcher.matches()) {
                if (labeledMatcher.matches())
                    code = labeledMatcher.group(2);
                else code = content;
            } else {
                code = blankMatcher.group(1);
            }

            if (!cl.hasOption('i'))
                throw new InvalidSyntaxException("Environment / Language must be specified in start of code block, or by -i option");

            final String env = cl.getOptionValue('i');
            callEnv(cl, env, code, replyTo.getChannel(), event);
        } else {
            newProcess(getBot().getExecutionManager(), ArgumentTokenizer.tokenize(content).toArray(String[]::new), null, replyTo.getChannel(), event, cl);
        }

        if (cl.hasOption('s') && event != null)
            event.getMessage().delete().queue();
    }

    private void callEnv(CommandLine cl, String env, String code, MessageChannel channel, MessageReceivedEvent event) {
        if (!envMap.containsKey(env))
            throw InvalidSyntaxException.argument(1, "Unknown env: %s".formatted(env), null);
        final ExecutionManager manager = getBot().getExecutionManager();
        envMap.get(env).call(code, channel, event, cl, manager);
    }

    private void newProcess(ExecutionManager manager, String[] args, String env, MessageChannel channel, MessageReceivedEvent event, CommandLine cl) {
        final int pid = manager.newProcess(
                args, env, channel, (i) -> ResponseTarget.addResult(i == 0, event.getMessage()),
                !cl.hasOption('s'), !cl.hasOption('a'), cl.hasOption('w') || cl.hasOption('W'));
        if (cl.hasOption('W'))
            manager.getStdout(pid).setWindowSize(Integer.parseInt(cl.getOptionValue('W')));
    }

    @NotNull
    private Path toTempFile(String code, String extension, List<String> filePrefix) {
        final Path pythonCode;
        try {
            pythonCode = Files.createTempFile("code", extension);
            FileUtils.write(pythonCode.toFile(),
                    filePrefix != null ? String.join("\n\n", String.join("\n", filePrefix), code) : code,
                    StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return pythonCode;
    }

    public void pythonEnv(String code,
                              MessageChannel channel,
                              MessageReceivedEvent event,
                              CommandLine cl,
                              ExecutionManager manager) {
        final Path pythonCode = toTempFile(code, ".py", Collections.singletonList("# -*- coding: UTF-8 -*-"));

        final List<String> args = new ArrayList<>(List.of(getBotConfig().pythonPath.getValue(), "-X", "utf8"));
        if (!cl.hasOption('b'))
            args.add("-u");
        args.add(pythonCode.normalize().toString());

        newProcess(manager, args.toArray(String[]::new), "python code", channel, event, cl);
    }

    public void cmdEnv(String code,
                           MessageChannel channel,
                           MessageReceivedEvent event,
                           CommandLine cl,
                           ExecutionManager manager) {
        List<String> prefixes = new ArrayList<>();
        if (!cl.hasOption('e')) prefixes.add("@echo off");
        prefixes.add("chcp 65001");

        final Path path = toTempFile(code, ".bat", prefixes);
        final String[] args = List.of("cmd", "/c", path.normalize().toString()).toArray(String[]::new);

        newProcess(manager, args, "cmd code", channel, event, cl);
    }

    public void shEnv(String code,
                          MessageChannel channel,
                          MessageReceivedEvent event,
                          CommandLine cl,
                          ExecutionManager manager) {
        throw new NotImplementedException("Shell scripts execution");
    }

    public void powershellEnv(String code,
                                  MessageChannel channel,
                                  MessageReceivedEvent event,
                                  CommandLine cl,
                                  ExecutionManager manager) {
        throw new NotImplementedException("Windows PowerShell scripts execution");
    }

    public void javascriptEnv(String code,
                                  MessageChannel channel,
                                  MessageReceivedEvent event,
                                  CommandLine cl,
                                  ExecutionManager manager) {
        throw new NotImplementedException("javascript execution");
    }

    public void javaEnv(String code,
                            MessageChannel channel,
                            MessageReceivedEvent event,
                            CommandLine cl,
                            ExecutionManager manager) {
        throw new NotImplementedException("Java code execution");
    }

    @Override
    public Options getOptions() {
        return new Options()
                .addOption("i", "interpreter", true,
                        "Specify environment. Possible variants: " + String.join(", ", envMap.keySet()))
                .addOption("b", "buffered", false,
                        "Enable stdout buffering in python (remove -u option from interpreter call)")
                .addOption("s", "silent", false,
                        "Do not send information about process start/stop (except non-zero exit status) " +
                                "and delete caller's message (if possible)")
                .addOption("a", "auto-flush-off", false,
                        "Don't use auto-flush for `tell` command")
                .addOption("e", "echo", false,
                        "Do not put \"@echo off\" at start of bat (cmd) file")
                .addOption("w", "window", false,
                        "Trim output to fit one window")
                .addOption("W", "window-size", true,
                        "Trim output to fit one window, with specified number of lines");
    }

    @Override
    public String getName() {
        return "exec";
    }

    @Override
    public String getShortDesc() {
        return "Spawn subprocess and handle output";
    }

    @Override
    public String getArgsSyntax() {
        return "<commands or code...>";
    }

    private interface Env {
        void call(String code, MessageChannel channel, MessageReceivedEvent event, CommandLine cl, ExecutionManager manager);
    }
}
