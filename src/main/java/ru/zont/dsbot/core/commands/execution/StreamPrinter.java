package ru.zont.dsbot.core.commands.execution;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.MessageEmbed;
import ru.zont.dsbot.core.util.MessageBatch;
import ru.zont.dsbot.core.util.MessageSplitter;
import ru.zont.dsbot.core.util.ResponseTarget;

import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;
import java.util.function.Supplier;

public class StreamPrinter {
    public static final int OUTPUT_UPDATE_PERIOD = 200;
    private final MessageChannel channel;
    private final InputStream stream;

    private final Thread updaterThread;
    private final Thread mainThread;

    private StringBuilder outSheet;
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
        for (Thread thread : List.of(mainThread, updaterThread)) {
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
        Scanner scanner = new Scanner(stream);
        updaterThread.start();

        while (scanner.hasNext() && updaterThread.isAlive()) {
            synchronized (this) {
                outSheet.append(scanner.nextLine()).append('\n');
                invalidate();
            }
            synchronized (updaterMonitor) {
                updaterMonitor.notifyAll();
            }
        }
        synchronized (updaterMonitor) {
            updaterMonitor.notifyAll();
        }
    }

    @SuppressWarnings("BusyWait")
    private void updaterThreadRun() {
        while (!Thread.interrupted() && mainThread.isAlive()) {
            try {
                synchronized (updaterMonitor) {
                    updaterMonitor.wait();
                }
                Thread.sleep(OUTPUT_UPDATE_PERIOD);
                if (!invalidated) continue;
            } catch (InterruptedException e) {
                break;
            }
            updateOutput();
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
            String content = "```\n%s\n```".formatted(outSheet.toString());

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

    public void invalidate() {
        invalidated = true;
    }

    public void setColor(int color) {
        this.color = color;
        updateOutput();
    }
}
