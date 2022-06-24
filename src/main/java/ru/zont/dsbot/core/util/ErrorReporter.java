package ru.zont.dsbot.core.util;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import org.apache.commons.io.output.StringBuilderWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.zont.dsbot.core.GuildContext;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;

public class ErrorReporter {
    private static final Logger log = LoggerFactory.getLogger(ErrorReporter.class);

    private final GuildContext context;
    private final HashMap<String, Report> reports;

    public ErrorReporter(GuildContext context) {
        this.context = context;
        reports = new HashMap<>();
    }

    public synchronized long reportError(ResponseTarget reportTo, String title, String description, String picture, int color, Throwable cause) {
        String id = getId(title, description, cause);
        Report report = reports.getOrDefault(id, null);
        long period = context.getConfig().getErrorRepeatPeriod();
        long current = System.currentTimeMillis() / 1000;

        if (report == null || current - report.lastReport > period) {
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

    private void newReport(ResponseTarget reportTo, String title, String description,
                           String picture, int color, Throwable cause, String id) {
        if (!ResponseTarget.isValid(reportTo))
            reportTo = ResponseTarget.channel(context.findLogChannel());

        try {
            if (!ResponseTarget.isValid(reportTo))
                throw new IllegalStateException("Cannot find log channel");

            MessageBatch messages = MessageBatch.sendNow(reportTo.response(
                    errorMessage(title, description, picture, color, cause)));
            reports.put(id, new Report(messages, System.currentTimeMillis() / 1000));

            log.error("Reported error: {}\n{}", title, description);
            log.error("Exception:", cause);
        } catch (Throwable t) {
            log.error("Cannot report error: {}\n{}", title, description);
            log.error("Cause:", t);
            log.error("Error to report:", cause);
        }
    }

    private String getId(String title, String description, Throwable cause) {
        StringBuilder id = new StringBuilder(context.getGuildId()).append(":");
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

        StringBuilder content = new StringBuilder(description)
                .append("\n")
                .append("```\n");
        try (StringBuilderWriter writer = new StringBuilderWriter(content);
             PrintWriter pw = new PrintWriter(writer)) {
            cause.printStackTrace(pw);
        }

        MessageSplitter splitter = new MessageSplitter(content.append("\n```"));
        splitter.insert("```\n", "\n```");
        return splitter.splitEmbeds(builder);
    }

    private class Report {
        private MessageBatch messages;
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
