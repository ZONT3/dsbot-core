package ru.zont.dsbot.core.commands.impl.basic;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.requests.restaction.MessageAction;
import ru.zont.dsbot.core.GuildContext;
import ru.zont.dsbot.core.ZDSBot;
import ru.zont.dsbot.core.commands.CommandAdapter;
import ru.zont.dsbot.core.commands.Input;
import ru.zont.dsbot.core.util.ResponseTarget;
import ru.zont.dsbot.core.util.Strings;

public class Ping extends CommandAdapter {
    public Ping(ZDSBot bot, GuildContext context) {
        super(bot, context);
    }

    @Override
    public void onCall(ResponseTarget replyTo, Input input, MessageReceivedEvent event, Object... params) {
        MessageEmbed embed = new EmbedBuilder()
                .setTitle("Pong!")
                .build();
        final MessageAction action = replyTo.respondEmbed(embed);

        long recv = System.currentTimeMillis();
        long created = event.getMessage().getTimeCreated().toInstant().toEpochMilli();
        Message msgSent = action.complete();
        long sent = System.currentTimeMillis();
        long sentCreated = msgSent.getTimeCreated().toInstant().toEpochMilli();

        long createdReceived = recv - created;
        long createdCreated = sentCreated - created;
        long recvSent = sent - recv;
        long createdSent = sent - created;

        msgSent.editMessageEmbeds(new EmbedBuilder(embed).setDescription("""
                Created - Received: `%04dms` *inaccurate*
                Messages diff: `%04dms`
                Sending took: `%04dms`
                Created - Sent: `%04dms` *inaccurate*""".formatted(createdReceived, createdCreated, recvSent, createdSent))
                .build()).queue();
    }

    @Override
    public String getName() {
        return "ping";
    }

    @Override
    public String getShortDesc() {
        return Strings.CORE.get("comms.ping.shortdesc");
    }
}
