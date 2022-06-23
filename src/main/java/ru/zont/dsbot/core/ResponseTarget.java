package ru.zont.dsbot.core;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.requests.restaction.MessageAction;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ResponseTarget {
    private MessageChannel channel;
    private Message message;

    public ResponseTarget(MessageChannel channel) {
        this.channel = channel;
    }

    public ResponseTarget(Message message) {
        this.message = message;
    }

    public MessageAction response(MessageEmbed... embeds) {
        if (channel != null) {
            return channel.sendMessageEmbeds(Arrays.stream(embeds).toList());
        } else if (message != null) {
            return message.replyEmbeds(Arrays.stream(embeds).toList());
        }
        return null;
    }

    public List<MessageAction> response(Message... messages) {
        ArrayList<MessageAction> list = new ArrayList<>(messages.length);
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
}
