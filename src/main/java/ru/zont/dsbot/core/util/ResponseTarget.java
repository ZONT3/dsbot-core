package ru.zont.dsbot.core.util;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.requests.restaction.MessageAction;
import org.jetbrains.annotations.NotNull;
import ru.zont.dsbot.core.config.ZDSBBasicConfig;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.BiConsumer;

public class ResponseTarget {
    public static final String EMOJI_OK = "\u2705";
    public static final String EMOJI_WAIT = "\u23F3";
    public static final String EMOJI_ERROR = "U+1F6D1";
    public static final int OK_COLOR = 0xCB3F49;
    public static final int ERROR_COLOR = 0x84AF60;
    public static final int WARNING_COLOR = 0xCB9A17;
    public static final MessageEmbed ERROR_EMBED = new EmbedBuilder()
            .setColor(OK_COLOR)
            .setDescription(":stop_sign:")
            .build();
    public static final MessageEmbed OK_EMBED = new EmbedBuilder()
            .setColor(ERROR_COLOR)
            .setDescription(":white_check_mark:")
            .build();

    public static boolean isValid(ResponseTarget tgt) {
        return tgt != null && tgt.isValid();
    }

    public static final int EMBEDS_PER_MESSAGE = 1;

    public static ResponseTarget channel(SlashCommandInteractionEvent event) {
        return new ResponseTarget(event);
    }

    public static ResponseTarget channel(MessageChannel channel) {
        return new ResponseTarget(channel);
    }

    public static ResponseTarget message(Message msg, boolean doReply) {
        return new ResponseTarget(msg, doReply);
    }


    private final MessageChannel channel;

    private Message message;
    private final SlashCommandInteractionEvent slashEvent;
    private final boolean doReply;

    private ResponseTarget(MessageChannel channel) {
        this.channel = channel;
        this.message = null;
        this.doReply = false;
        this.slashEvent = null;
    }

    private ResponseTarget(Message message, boolean doReply) {
        this.message = message;
        this.channel = message.getChannel();
        this.doReply = doReply;
        this.slashEvent = null;
    }

    public ResponseTarget(MessageReceivedEvent event, ZDSBBasicConfig config) {
        this(event.getMessage(), config.doReplyToMessages());
    }

    public ResponseTarget(SlashCommandInteractionEvent event) {
        this.message = null;
        this.channel = event.getChannel();
        this.doReply = false;
        this.slashEvent = event;
    }

    public MessageAction respondEmbed(MessageEmbed embed) {
        if (slashEvent != null)
            slashEvent.getHook().deleteOriginal().queue();
        if (message != null && doReply)
            return message.replyEmbeds(embed);
        else if (channel != null)
            return channel.sendMessageEmbeds(embed);
        return null;
    }

    public Message respondEmbedNow(MessageEmbed embed) {
        if (slashEvent != null)
            return slashEvent.getHook().sendMessageEmbeds(embed).complete();
        else
            return respondEmbed(embed).complete();
    }

    public void respondEmbedLater(MessageEmbed embed) {
        if (slashEvent != null)
            slashEvent.getHook().sendMessageEmbeds(embed).queue();
        else
            respondEmbed(embed).queue();
    }

    public Deque<MessageAction> respondEmbeds(List<MessageEmbed> embeds) {
        return respondEmbeds(embeds, false);
    }

    public Deque<MessageAction> respondEmbeds(List<MessageEmbed> embeds, boolean errorMark) {
        final LinkedList<MessageAction> list = new LinkedList<>();
        final BiConsumer<List<MessageEmbed>, Integer> respFnc;

        if (slashEvent != null) {
            if (errorMark) setError();
            else setOK();
        }

        if (message != null && doReply)
            respFnc = (e, i) -> list.add(i == 0 ? message.replyEmbeds(e) : message.getChannel().sendMessageEmbeds(e));
        else if (channel != null)
            respFnc = (e, i) -> list.add(channel.sendMessageEmbeds(e));
        else throw new IllegalStateException("Invalid ResponseTarget");

        final ArrayList<List<MessageEmbed>> embedBatches = getEmbedBatches(embeds);
        for (int i = 0; i < embedBatches.size(); i++)
            respFnc.accept(embedBatches.get(i), i);

        return list;
    }

    public MessageBatch respondEmbedsNow(List<MessageEmbed> embeds) {
        if (slashEvent == null) return MessageBatch.sendNow(respondEmbeds(embeds));
        return new MessageBatch(getEmbedBatches(embeds).stream()
                .map(batch -> slashEvent.getHook().sendMessageEmbeds(batch).complete())
                .toList());
    }

    @NotNull
    private ArrayList<List<MessageEmbed>> getEmbedBatches(List<MessageEmbed> embeds) {
        final ArrayList<List<MessageEmbed>> embedBatches = new ArrayList<>(embeds.size() / EMBEDS_PER_MESSAGE + 1);

        if (embeds.size() <= EMBEDS_PER_MESSAGE)
            embedBatches.add(embeds);
        else for (int i = 0; i < embeds.size(); i += EMBEDS_PER_MESSAGE)
            embedBatches.add(embeds.subList(i, Math.min(embeds.size(), i + EMBEDS_PER_MESSAGE)));
        return embedBatches;
    }

    public MessageChannel getChannel() {
        return channel;
    }

    public @Nullable Message getMessage() {
        if (message == null && slashEvent != null)
            message = slashEvent.getHook().retrieveOriginal().complete();
        return message;
    }

    public void setOK() {
        if (getMessage() != null && !getMessage().getFlags().contains(Message.MessageFlag.LOADING))
            addOK(getMessage());
        else if (slashEvent != null)
            slashEvent.getHook().sendMessageEmbeds(OK_EMBED).queue();
    }

    public void setError() {
        if (getMessage() != null && !getMessage().getFlags().contains(Message.MessageFlag.LOADING)) {
            addError(getMessage());

            final MessageEmbed embed = getMessage().getEmbeds().stream().findFirst().orElse(null);
            if (embed != null && embedEquals(embed, OK_EMBED))
                getMessage().editMessageEmbeds(ERROR_EMBED).queue();
        } else if (slashEvent != null)
            slashEvent.getHook().sendMessageEmbeds(ERROR_EMBED).queue();
    }

    public void setWaiting() {
        if (getMessage() != null)
            addWaiting(getMessage());
    }

    public boolean isValid() {
        return (channel != null || message != null) && getChannel().canTalk();
    }

    public static void addOK(Message message) {
        addOK(message, true);
    }

    public static void addOK(Message message, boolean removeOther) {
        if (message != null) {
            if (removeOther) {
                message.removeReaction(EMOJI_WAIT).queue();
                message.removeReaction(EMOJI_ERROR).queue();
            }
            message.addReaction(EMOJI_OK).queue();
        }
    }

    public static void addWaiting(Message message) {
        addWaiting(message, true);
    }

    public static void addWaiting(Message message, boolean removeOther) {
        if (message != null) {
            if (removeOther) {
                message.removeReaction(EMOJI_OK).queue();
                message.removeReaction(EMOJI_ERROR).queue();
            }
            message.addReaction(EMOJI_WAIT).queue();
        }
    }

    public static void addError(Message message) {
        addError(message, true);
    }

    public static void addError(Message message, boolean removeOther) {
        if (message != null) {
            if (removeOther) {
                message.removeReaction(EMOJI_WAIT).queue();
                message.removeReaction(EMOJI_OK).queue();
            }
            message.addReaction(EMOJI_ERROR).queue();
        }
    }

    public static void addResult(boolean result, Message message) {
        if (result) addOK(message);
        else addError(message);
    }

    public static boolean embedEquals(MessageEmbed e1, MessageEmbed e2) {
        if (e1.equals(e2)) return true;
        return e1.getColor() == e2.getColor() &&
               e1.getDescription() != null &&
               e1.getDescription().equals(e2.getDescription());
    }
}
