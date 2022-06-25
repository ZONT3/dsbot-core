package ru.zont.dsbot.core.commands.execution;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.MessageEmbed;
import org.apache.commons.codec.Charsets;
import ru.zont.dsbot.core.util.MessageBatch;
import ru.zont.dsbot.core.util.MessageSplitter;
import ru.zont.dsbot.core.util.ResponseTarget;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;
import java.util.function.Supplier;
import java.util.regex.Pattern;

public class StreamPrinter {
    public static final int OUTPUT_UPDATE_PERIOD = 2000;
    public static final int BUFFER_UPDATE_PERIOD = 600;
    public static final Pattern PATTERN_PRE_APPEND = Pattern.compile("\r{2,}");
    private final MessageChannel channel;
    private final InputStream stream;

    private final Thread updaterThread;
    private final Thread mainThread;

    private final StringBuilder outSheet;
    private MessageBatch messages;
    private boolean invalidated = false;

    private final Object updaterMonitor = new Object();
    private MessageEmbed embedTemplate;
    private Supplier<MessageEmbed> templateGetter;
    private int color = -1;

    public StreamPrinter(String name, MessageChannel channel, InputStream stream) {
        this.channel = channel;
        this.stream = stream;
        outSheet = new StringBuilder();

        mainThread = new Thread(this::mainThreadRun, "StramPrinter:main(%s)".formatted(name));
        updaterThread = new Thread(this::updaterThreadRun, "StreamPrinter:updater(%s)".formatted(name));
        for (Thread thread: List.of(mainThread, updaterThread)) {
            thread.setDaemon(true);
            thread.setPriority(Thread.MAX_PRIORITY);
        }

        embedTemplate = new EmbedBuilder()
                .setTitle(name)
                .setColor(0x1747A5)
                .build();
        templateGetter = () -> embedTemplate;
    }

    public void startPrinter() {
        mainThread.start();
    }

    private void mainThreadRun() {
        try (InputStreamReader reader = new InputStreamReader(stream)) {
            updaterThread.start();

            StringBuilder buff = new StringBuilder();
            int next;
            boolean fill = false;
            long nextFill = 0;
            while ((next = reader.read()) >= 0 && updaterThread.isAlive()) {
                boolean fillNow = false;
                if (next == '\n' || next == '\r') {
                    fill = true;
                } else if (fill) {
                    fill = false;
                    fillNow = true;
                }
                fillNow = fillNow || !buff.isEmpty() && buff.charAt(buff.length() - 1) == '\n';

                if (nextFill <= 0 && !buff.isEmpty())
                    nextFill = System.currentTimeMillis() + BUFFER_UPDATE_PERIOD;

                if (fillNow || !buff.isEmpty() && nextFill < System.currentTimeMillis()) {
                    nextFill = 0;
                    append(buff);
                    synchronized (updaterMonitor) {
                        updaterMonitor.notifyAll();
                    }
                }
                buff.append((char) next);
            }
            if (!buff.isEmpty()) append(buff);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        synchronized (updaterMonitor) {
            updaterMonitor.notifyAll();
        }
    }

    private synchronized void append(StringBuilder buff) {
        String buffStr = PATTERN_PRE_APPEND.matcher(buff.toString()).replaceAll("\r");
        String before = outSheet.toString();

        if (!outSheet.isEmpty() && outSheet.charAt(outSheet.length() - 1) == '\r') {
            final int i = outSheet.lastIndexOf("\n") + 1;
            if (i < outSheet.length())
                outSheet.replace(i, buffStr.length() + i, buffStr);
            else outSheet.append(buffStr);
        } else {
            outSheet.append(buffStr);
        }
        buff.delete(0, buff.length());

        if (!before.equals(outSheet.toString())) {
            invalidate();
        }
    }

    private void updaterThreadRun() {
        while (!Thread.interrupted() && mainThread.isAlive()) {
            try {
                synchronized (updaterMonitor) {
                    updaterMonitor.wait();
                }
                synchronized (updaterMonitor) {
                    final long until = System.currentTimeMillis() + OUTPUT_UPDATE_PERIOD;
                    while (System.currentTimeMillis() < until) {
                        updaterMonitor.wait(Math.max(10, until - System.currentTimeMillis()));
                    }
                }
                synchronized (this) {
                    if (invalidated) updateOutput();
                }
            } catch (InterruptedException e) {
                break;
            }
        }
        updateOutput();
    }

    private synchronized void updateOutput() {
        if (channel == null)
            throw new NullPointerException("Channel is null");
        if (!channel.canTalk())
            throw new IllegalStateException("Cannot write to specified channel!");

        List<MessageEmbed> embeds;
        if (!outSheet.isEmpty()) {
            final String content = String.join("\n", "```", outSheet.toString(), "```");

            EmbedBuilder template = new EmbedBuilder(templateGetter.get());
            if (color >= 0) template.setColor(color);

            MessageSplitter splitter = new MessageSplitter(content);
            splitter.setKeepTitle(true, true);
            splitter.insert("```\n", "\n```");
            embeds = splitter.splitEmbeds(template, MessageSplitter.SplitPolicy.NEWLINE);
        } else embeds = Collections.emptyList();

        if (messages == null && embeds.size() > 0)
            messages = MessageBatch.sendNow(ResponseTarget.channel(channel).responseEmbed(embeds));
        else if (messages != null) messages.updateEmbeds(embeds, channel);
        invalidated = false;
    }

    public void setEmbedTemplate(MessageEmbed embedTemplate) {
        this.embedTemplate = embedTemplate;
    }

    public void setTemplateGetter(Supplier<MessageEmbed> templateGetter) {
        this.templateGetter = templateGetter;
    }

    public synchronized void invalidate() {
        invalidated = true;
    }

    public void setColor(int color) {
        this.color = color;
        updateOutput();
    }
}
