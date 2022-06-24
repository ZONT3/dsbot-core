package ru.zont.dsbot.core.util;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.requests.restaction.MessageAction;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

public class MessageBatch extends LinkedList<Message> {
    public MessageBatch(Collection<Message> messages) {
        super(messages);
    }

    public static MessageBatch sendNow(Deque<MessageAction> actions) {
        return new MessageBatch(actions.stream().map(RestAction::complete).collect(Collectors.toList()));
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
            for (int i = 0; i < size() - embeds.size(); i++) {
                Message message = removeLast();
                message.delete().queue();
            }
            toEdit = size();
        } else if (embeds.size() > size()) {
            toEdit = size();
            for (MessageEmbed embed : embeds.subList(size(), embeds.size())) {
                add(channel.sendMessageEmbeds(embed).complete());
            }
        } else toEdit = size();

        Iterator<Message> it1 = iterator();
        Iterator<MessageEmbed> it2 = embeds.subList(0, toEdit).iterator();
        while (it1.hasNext() && it2.hasNext()) {
            it1.next().editMessageEmbeds(it2.next()).queue();
        }
    }
}
