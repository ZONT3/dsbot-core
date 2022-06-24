package ru.zont.dsbot.core.util;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.requests.restaction.MessageAction;

import java.util.*;
import java.util.function.BiConsumer;

public class ResponseTarget {

    public static boolean isValid(ResponseTarget tgt) {
        return tgt != null && tgt.isValid();
    }

    public static final int EMBEDS_PER_MESSAGE = 1;

    public static ResponseTarget channel(MessageChannel channel) {
        return new ResponseTarget(channel);
    }

    public static ResponseTarget message(Message msg) {
        return new ResponseTarget(msg);
    }

    public static void queueAll(Deque<MessageAction> actions) {
        for (MessageAction action : actions) action.queue();
    }

    private MessageChannel channel;
    private Message message;

    public ResponseTarget(MessageChannel channel) {
        this.channel = channel;
    }

    public ResponseTarget(Message message) {
        this.message = message;
    }

    public MessageAction responseEmbed(MessageEmbed embed) {
        if (channel != null)
            return channel.sendMessageEmbeds(embed);
        else if (message != null)
            return message.replyEmbeds(embed);
        else return null;
    }

    public Deque<MessageAction> responseEmbed(List<MessageEmbed> embeds) {
        final LinkedList<MessageAction> list = new LinkedList<>();
        final BiConsumer<List<MessageEmbed>, Integer> respFnc;

        if (channel != null)
            respFnc = (e, i) -> list.add(channel.sendMessageEmbeds(e));
        else if (message != null)
            respFnc = (e, i) -> list.add(i == 0 ? message.replyEmbeds(e) : message.getChannel().sendMessageEmbeds(e));
        else return null;

        if (embeds.size() <= EMBEDS_PER_MESSAGE) {
            respFnc.accept(embeds, 0);
        } else {
            for (int i = 0; i < embeds.size(); i += EMBEDS_PER_MESSAGE) {
                List<MessageEmbed> subList = embeds.subList(i, Math.min(embeds.size(), i + EMBEDS_PER_MESSAGE));
                respFnc.accept(subList, i / EMBEDS_PER_MESSAGE);
            }
        }
        return list;
    }

    public Deque<MessageAction> response(Message... messages) {
        LinkedList<MessageAction> list = new LinkedList<>();
        for (Message msg : messages) {
            if (channel != null) {
                list.add(channel.sendMessage(msg));
            } else if (message != null) {
                list.add(message.reply(msg));
            } else {
                return null;
            }
        }
        return list;
    }

    public MessageAction response(Message message) {
        if (channel != null) {
            return channel.sendMessage(message);
        } else if (message != null) {
            return message.reply(message);
        } else {
            return null;
        }
    }

    public MessageChannel getChannel() {
        if (!isValid()) return null;
        if (channel != null) return channel;
        return message.getChannel();
    }

    public boolean isValid() {
        return channel != null || message != null;
    }
}
