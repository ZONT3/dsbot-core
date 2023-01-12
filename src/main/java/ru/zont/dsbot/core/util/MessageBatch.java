package ru.zont.dsbot.core.util;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.requests.restaction.MessageAction;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class MessageBatch extends LinkedList<Message> {
    public MessageBatch(Collection<Message> messages) {
        super(messages);
    }

    public static MessageBatch sendNow(Deque<MessageAction> actions) {
        return new MessageBatch(actions.stream().map(RestAction::complete).collect(Collectors.toList()));
    }

    public static MessageBatch sendNowWithMessageSplitter(ResponseTarget replyTo,
                                                          CharSequence content,
                                                          EmbedBuilder base) {
        return sendNow(replyTo.respondEmbeds(MessageSplitter.embeds(content, base)));
    }

    public static MessageBatch sendNowWithMessageSplitter(MessageChannel channel,
                                                          CharSequence content,
                                                          EmbedBuilder base) {
        return sendNowWithMessageSplitter(ResponseTarget.channel(channel), content, base);
    }

    public void updatePresence(MessageChannel channel) {
        LinkedList<Message> toRemove = new LinkedList<>();
        for (Message message : this) {
            try {
                channel.retrieveMessageById(message.getId()).complete();
            } catch (ErrorResponseException ignored) {
                toRemove.add(message);
            }
        }
        for (Message message : toRemove) remove(message);
    }

    public void updateEmbeds(List<MessageEmbed> embeds, MessageChannel channel) {
        updatePresence(channel);
        int toEdit;
        if (embeds.size() < size()) {
            List<Message> messages = IntStream.range(0, size() - embeds.size())
                    .mapToObj(i -> removeLast())
                    .toList();
            channel.purgeMessages(messages);
            toEdit = embeds.size();
        } else if (embeds.size() > size()) {
            for (MessageEmbed embed : embeds.subList(size(), embeds.size()))
                add(channel.sendMessageEmbeds(embed).complete());
            toEdit = size();
        } else toEdit = size();

        Iterator<Message> it1 = iterator();
        Iterator<MessageEmbed> it2 = embeds.subList(0, toEdit).iterator();
        while (it1.hasNext() && it2.hasNext()) {
            it1.next().editMessageEmbeds(it2.next()).queue();
        }
    }
}
