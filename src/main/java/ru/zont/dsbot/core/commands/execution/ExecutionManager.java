package ru.zont.dsbot.core.commands.execution;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.MessageEmbed;
import ru.zont.dsbot.core.ZDSBot;
import ru.zont.dsbot.core.util.DescribedException;
import ru.zont.dsbot.core.util.ResponseTarget;
import ru.zont.dsbot.core.util.Strings;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.stream.Collectors;

public class ExecutionManager {
    public static final int COMPLETE_COLOR = 0x1CDC1C;
    public static final int COMPLETE_COLOR_STDERR = 0x8AB327;
    public static final int ERROR_COLOR = DescribedException.ERROR_COLOR;
    public static final int ERROR_COLOR_STDERR = 0xB38120;
    public static final int STDERR_COLOR = 0xB3A82B;

    private final ZDSBot bot;

    private final ArrayList<Process> processes = new ArrayList<>();
    private final ArrayList<StreamPrinter> stdoutList = new ArrayList<>();
    private final ArrayList<StreamPrinter> stderrList = new ArrayList<>();
    private final HashSet<Integer> terminated = new HashSet<>();

    public ExecutionManager(ZDSBot bot) {
        this.bot = bot;
    }

    public int newProcess(String[] args, String name, MessageChannel output, boolean verbose) {
        if (name == null) name = args[0];
        final String finalName = name;
        try {
            final Process process = Runtime.getRuntime().exec(args);
            processes.add(process);
            final int pid = processes.size();

            if (output != null) {
                final StreamPrinter stdout =
                        new StreamPrinter("[%d] %s stdout".formatted(pid, name), output, process.getInputStream());
                final StreamPrinter stderr =
                        new StreamPrinter("[%d] %s stderr".formatted(pid, name), output, process.getErrorStream());
                stdoutList.add(stdout);
                stderrList.add(stderr);
                stderr.setColor(STDERR_COLOR);
                stdout.startPrinter();
                stderr.startPrinter();

                if (verbose) verboseStart(output, pid, name, args, process);
                final long startTimestamp = System.currentTimeMillis();

                process.onExit().thenAccept(p -> {
                    final int exitCode = p.exitValue();
                    stdout.setColor(exitCode == 0 ? COMPLETE_COLOR : ERROR_COLOR);
                    stderr.setColor(exitCode == 0 ? COMPLETE_COLOR_STDERR : ERROR_COLOR_STDERR);

                    if (verbose)
                        verboseEnd(output, pid, finalName, exitCode, System.currentTimeMillis() - startTimestamp);

                    processes.remove(pid);
                });
            }

            return pid;
        } catch (IOException e) {
            bot.getErrorReporter().reportError(ResponseTarget
                    .channel(output != null ? output : bot.findLogChannel()), e);
        }
        return -1;
    }

    private void verboseStart(MessageChannel channel, int pid, String name, String[] args, Process process) {
        if (channel == null) {
            channel = bot.findLogChannel();
            if (channel == null) return;
        }

        final MessageEmbed embed = new EmbedBuilder()
                .setTitle("Process [%s] started".formatted(pid))
                .setDescription("""
                        Name: `%s`
                        Args: `%s`
                        System PID: `%d`
                        Internal PID: `%d`""".formatted(
                        name,
                        Strings.trimSnippet(
                                Arrays.stream(args).map("\"%s\""::formatted).collect(Collectors.joining(", ")),
                                Strings.DS_CODE_BLOCK_LINE_LENGTH - 10),
                        process.pid(), pid))
                .setColor(0xBCBCBC)
                .setTimestamp(Instant.now()).build();
        channel.sendMessageEmbeds(embed).queue();
    }

    private synchronized void verboseEnd(MessageChannel channel, int pid, String name, int exitCode, long execTime) {
        if (channel == null) {
            channel = bot.findLogChannel();
            if (channel == null) return;
        }

        boolean term = terminated.remove(pid);

        final MessageEmbed embed = new EmbedBuilder()
                .setTitle("Process [%s] %s".formatted(pid, term ? "terminated" : "finished"))
                .setDescription("""
                        Name: `%s`
                        Internal PID: `%d`
                        Duration: `%s`
                        Exit code: `%d`""".formatted(
                        name, pid, Strings.millisToDuration(execTime), exitCode))
                .setColor(exitCode == 0 ? COMPLETE_COLOR : ERROR_COLOR)
                .setTimestamp(Instant.now()).build();
        channel.sendMessageEmbeds(embed).queue();
    }

    public Process findProcess(int id) {
        if ((id - 1) < processes.size())
            return processes.get(id - 1);
        return null;
    }

    public synchronized void killProcess(int id, boolean force) {
        final Process process = findProcess(id);
        if (process != null && process.isAlive()) {
            if (force) process.destroyForcibly();
            else process.destroy();
            terminated.add(id);
        }
    }

    public StreamPrinter getStdout(int id) {
        return stdoutList.get(id);
    }

    public StreamPrinter getStderr(int id) {
        return stderrList.get(id);
    }
}
