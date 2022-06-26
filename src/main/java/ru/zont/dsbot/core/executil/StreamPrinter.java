package ru.zont.dsbot.core.executil;

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
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;
import java.util.function.Supplier;
import java.util.regex.Pattern;

public class StreamPrinter {
    public static final int OUTPUT_UPDATE_PERIOD = 2000;
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
    private int currIndex = 0;

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
        try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            updaterThread.start();

            int next;
            while ((next = reader.read()) >= 0 && updaterThread.isAlive()) {
                append((char) next);
                synchronized (updaterMonitor) {
                    updaterMonitor.notifyAll();
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        synchronized (updaterMonitor) {
            updaterMonitor.notifyAll();
        }
    }

    private synchronized void append(char next) {
        final String before = outSheet.toString();
        final int length = before.length();

        if (next == '\r') {
            currIndex = outSheet.lastIndexOf("\n") + 1;
            return;
        }
        if (next == '\n')
            currIndex = length;
        if (currIndex >= length)
            outSheet.append(next);
        else outSheet.setCharAt(currIndex, next);
        currIndex++;

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
