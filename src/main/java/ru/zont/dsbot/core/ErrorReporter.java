package ru.zont.dsbot.core;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.MessageEmbed;
import org.apache.commons.io.output.StringBuilderWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.zont.dsbot.core.util.*;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;

public class ErrorReporter {
    private static final Logger log = LoggerFactory.getLogger(ErrorReporter.class);

    private final ZDSBot bot;
    private final GuildContext context;
    private final HashMap<String, Report> reports;

    public ErrorReporter(GuildContext context) {
        this(context.getBot(), context);
    }

    public ErrorReporter(ZDSBot bot) {
        this(bot, null);
    }

    public ErrorReporter(ZDSBot bot, GuildContext context) {
        this.bot = bot;
        this.context = context;
        reports = new HashMap<>();
    }

    @SuppressWarnings("UnusedReturnValue")
    public long reportError(ResponseTarget reportTo, String title, String description, String picture, int color, Throwable cause) {
        try {
            return reportErrorWrapped(reportTo, title, description, picture, color, cause);
        } catch (Throwable t) {
            log.error(context.formatLog("Cannot report error: {}\n{}"), title, description);
            log.error("Cause:", t);
            log.error("Error to report:", cause);
        }
        return 0;
    }

    private synchronized long reportErrorWrapped(ResponseTarget reportTo, String title, String description, String picture, int color, Throwable cause) {
        String id = getId(title, description, cause);
        Report report = reports.getOrDefault(id, null);
        long period = getErrorRepeatPeriod();
        long current = System.currentTimeMillis() / 1000;

        if (!ResponseTarget.isValid(reportTo))
            reportTo = ResponseTarget.channel(getLogChannel());
        if (!ResponseTarget.isValid(reportTo))
            throw new IllegalStateException("Cannot find log channel");

        boolean chk = report == null || current - report.lastReport > period;
        if (!chk) {
            for (Message msg : report.messages) {
                String msgId = msg.getId();
                Message retMsg = reportTo.getChannel().retrieveMessageById(msgId).complete();
                if (retMsg == null) {
                    chk = true;
                    break;
                }
            }
        }
        if (chk) {
            newReport(reportTo, title, description, picture, color, cause, id);
            return 0;
        }

        report.count++;
        report.lastReport = current;
        final long minutes = (report.lastReport - report.firstReport) / 60;
        final long hours = minutes / 60;

        String footer;
        if (hours > 0) {
            footer = Strings.CORE.get("err.multiple.hour",
                    Strings.CORE.getPlural(report.count, "plurals.count"),
                    Strings.CORE.getPlural(hours, "plurals.last"),
                    Strings.CORE.getPlural(hours, "plurals.hour"),
                    Strings.CORE.getPlural(minutes % 60, "plurals.minute"));
        } else {
            footer = Strings.CORE.get("err.multiple",
                    Strings.CORE.getPlural(report.count, "plurals.count"),
                    Strings.CORE.getPlural(minutes, "plurals.last.r"),
                    Strings.CORE.getPlural(minutes, "plurals.minute"));
        }

        Message last = report.messages.getLast();
        EmbedBuilder builder = new EmbedBuilder(last.getEmbeds().get(0)).setFooter(footer);
        last.editMessageEmbeds(builder.build()).queue();

        return report.count;
    }

    private MessageChannel getLogChannel() {
        if (context != null) return context.findLogChannel();
        return bot.findLogChannel();
    }

    private long getErrorRepeatPeriod() {
        if (context != null)
            return context.getConfig().getErrorRepeatPeriod();
        return bot.getGlobalConfig().getErrorRepeatPeriod();
    }

    private void newReport(ResponseTarget reportTo, String title, String description,
                           String picture, int color, Throwable cause, String id) {
        MessageBatch messages = MessageBatch.sendNow(reportTo.responseEmbed(
                errorMessage(title, description, picture, color, cause)));
        reports.put(id, new Report(messages, System.currentTimeMillis() / 1000));

        log.error(context.formatLog("Reported error: {}\n{}"), title, description);
        log.error("Exception:", cause);
    }

    private String getId(String title, String description, Throwable cause) {
        StringBuilder id = new StringBuilder(context != null ? context.getGuildId() : "GLOBAL").append(":");
        if (cause != null) id.append(cause.getClass().getSimpleName());
        else id.append(title).append(":").append(description);
        return id.toString();
    }

    public List<MessageEmbed> errorMessage(String title, String description, String picture, int color, Throwable cause) {
        if (color <= 0)
            color = DescribedException.ERROR_COLOR;

        EmbedBuilder builder = new EmbedBuilder()
                .setColor(color)
                .setTitle(title)
                .setImage(picture);

        StringBuilder content = new StringBuilder(description);
        if (cause != null) {
            content.append("\n").append("```\n");
            try (StringBuilderWriter writer = new StringBuilderWriter(content);
                 PrintWriter pw = new PrintWriter(writer)) {
                cause.printStackTrace(pw);
            }
            content.append("\n```");
        }

        MessageSplitter splitter = new MessageSplitter(content);
        if (cause != null) splitter.insert("```\n", "\n```");
        return splitter.splitEmbeds(builder);
    }

    private static class Report {
        private final MessageBatch messages;
        private final long firstReport;
        private long lastReport;
        private long count;

        public Report(MessageBatch messages, long firstReport) {
            this.messages = messages;
            this.firstReport = firstReport;
            lastReport = firstReport;
            count = 1;
        }
    }
}
