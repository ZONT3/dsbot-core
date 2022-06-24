package ru.zont.dsbot.core.commands.basic;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.requests.restaction.MessageAction;
import org.apache.commons.cli.CommandLine;
import ru.zont.dsbot.core.GuildContext;
import ru.zont.dsbot.core.commands.CommandAdapter;
import ru.zont.dsbot.core.util.Strings;

import java.time.Instant;

public class Ping extends CommandAdapter {
    public Ping(GuildContext context) {
        super(context);
    }

    @Override
    public void onCall(MessageReceivedEvent event, String content, String[] args, CommandLine cl) {
        MessageEmbed embed = new EmbedBuilder()
                .setTitle("Pong!")
                .build();
        MessageAction action = getContext().getResponseTarget(event).responseEmbed(embed);

        long recv = System.currentTimeMillis();
        long created = event.getMessage().getTimeCreated().toInstant().toEpochMilli();
        Message msgSent = action.complete();
        long sent = System.currentTimeMillis();
        long sentCreated = msgSent.getTimeCreated().toInstant().toEpochMilli();

        long createdReceived = recv - created;
        long recvSent = sent - recv;
        long createdCreated = sentCreated - created;
        long sentSent = sentCreated - sent;

        msgSent.editMessageEmbeds(new EmbedBuilder(embed).setDescription("""
                Created - Received: `%04dms` *inaccurate*
                Messages diff: `%04dms`
                Sending took: `%04dms`
                Sent - Created: `%04dms` *inaccurate*""".formatted(createdReceived, createdCreated, recvSent, sentSent))
                .build()).queue();
    }

    @Override
    public String getName() {
        return "ping";
    }

    @Override
    public String getSyntax() {
        return getName();
    }

    @Override
    public String getShortDesc() {
        return Strings.CORE.get("comms.ping.shortdesc");
    }
}
