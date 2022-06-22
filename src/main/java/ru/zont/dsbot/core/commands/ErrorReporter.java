package ru.zont.dsbot.core.commands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.zont.dsbot.core.GuildContext;
import ru.zont.dsbot.core.util.EmbedSplitter;
import ru.zont.dsbot.core.util.Strings;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;

public class ErrorReporter {
    public static Logger log = LoggerFactory.getLogger(ErrorReporter.class);

    public static final int ERROR_COLOR = 0xf00505;
    private final MessageChannel defaultChannel;

    private final HashMap<String, ReportData> map = new HashMap<>();

    private ReportData lastLogged;

    public ErrorReporter(GuildContext context) {
        final String channelId = context.getConfig().log_channel.getValue();
        MessageChannel channel = null;
        if (!"0".equals(channelId) && channelId.matches("\\d+")) {
            channel = context.getGuild().getTextChannelById(channelId);
            if (channel == null) {
                log.info(context.formatLog("Log channel not found in this guild, falling to global..."));
                channel = context.getBot().getJda().getTextChannelById(channelId);
            }
        }

        if (channel == null) {
            log.warn("Log channel not found, falling to first OP's private...");
            final String op = context.getBot().getConfig().getOperators().get(0);
            final User user = context.getBot().getJda().retrieveUserById(op).complete();
            channel = user.openPrivateChannel().complete();
        }

        if (channel != null) {
            log.info(context.formatLog("Log channel found."));
        }

        defaultChannel = channel;
    }

    private long reportError(DescribedException t) {
        return reportError(defaultChannel, null, t);
    }

    private long reportError(Throwable t, String title, String text) {
        return reportError(defaultChannel, null, t, title, text);
    }

    public long reportError(MessageChannel channel, Message replyTo, DescribedException t) {
        return reportError(channel, replyTo, t, t.getTitle(), t.getDescription());
    }

    public long reportError(MessageChannel channel, Message replyTo, Throwable t, String title, String text) {
        final String id = getId(t);
        final ReportData rpt = map.getOrDefault(id, null);

        if (rpt != null) {
            if ((System.currentTimeMillis() / 1000 - rpt.lastReport) < 60 * 60)
                return newReport(channel, replyTo, t, title, text, id);
            return addReport(rpt);
        } else {
            return newReport(channel, replyTo, t, title, text, id);
        }
    }

    private long addReport(ReportData rpt) {
        final long now = rpt.count + 1;

        if (rpt.reportMessage != null) {
            final List<MessageEmbed> embeds = rpt.reportMessage.getEmbeds();
            final EmbedBuilder builder = new EmbedBuilder(embeds.get(embeds.size() - 1));

            builder.setFooter(Strings.CORE.get("err.multiple", now))
                    .build();
            rpt.update(rpt.reportMessage.editMessageEmbeds(builder.build()).complete());
        }

        if (lastLogged != rpt) {
            log.error("Reported error", rpt.throwable);
            lastLogged = rpt;
        }

        return now;
    }

    private long newReport(MessageChannel channel, Message replyTo, Throwable t, String title, String text, String id) {
        Message last = null;
        if (channel != null || replyTo != null) {
            last = EmbedSplitter.create(
                    new EmbedBuilder()
                            .setColor(ERROR_COLOR)
                            .setTitle(title)
                    ).setDescription(text).sendAll(channel, replyTo).getLast();
        }
        final ReportData rpt = new ReportData(last, t);
        map.put(id, rpt);

        log.error("Reported error", t);
        lastLogged = rpt;

        return 1L;
    }

    private static String getId(Throwable t) {
        return "%s:%s".formatted(t.getClass().getName(), t.getMessage().hashCode());
    }

    private static class ReportData {
        private final Throwable throwable;
        private long lastReport;
        private long count;
        private Message reportMessage;

        private ReportData(Message reportMessage, Throwable t) {
            this.throwable = t;
            count = 0L;
            update(reportMessage);
        }

        private void update(Message reportMessage) {
            count++;

            final OffsetDateTime timeEdited = reportMessage.getTimeEdited();
            lastReport = timeEdited != null
                    ? timeEdited.toEpochSecond()
                    : reportMessage.getTimeCreated().toEpochSecond();
            this.reportMessage = reportMessage;
        }
    }
}
