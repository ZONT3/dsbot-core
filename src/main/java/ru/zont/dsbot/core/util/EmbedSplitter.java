package ru.zont.dsbot.core.util;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.MessageEmbed;

import java.util.*;
import java.util.List;

public class EmbedSplitter {
    public static final String ELLIPSIS_SYMBOL = "…";

    private final EmbedBuilder original;
    private final StringBuilder desc;

    public EmbedSplitter(EmbedBuilder original) {
        this.original = original;
        desc = new StringBuilder(original.getDescriptionBuilder());
    }

    public static EmbedSplitter create(EmbedBuilder builder, CharSequence desc) {
        return new EmbedSplitter(builder).setDescription(desc);
    }

    public static EmbedSplitter create(EmbedBuilder builder) {
        return new EmbedSplitter(builder);
    }

    public EmbedSplitter setDescription(CharSequence text, Object... args) {
        desc.setLength(0);
        return appendDescription(text, args);
    }

    public EmbedSplitter appendDescription(CharSequence text, Object... args) {
        if (text != null && text.length() >= 1) {
            if (args.length > 0)
                desc.append(String.format(text.toString(), args));
            else
                desc.append(text);
        }
        return this;
    }

    public Queue<MessageEmbed> buildAll() {
        return buildAll(true);
    }

    public Queue<MessageEmbed> buildAll(boolean ellipsis) {
        return buildAll(ellipsis, SplitPolicy.NEWLINE, SplitPolicy.SPACE, SplitPolicy.ZWSP);
    }

    public Queue<MessageEmbed> buildAll(SplitPolicy... policy) {
        return buildAll(true, policy);
    }

    public Queue<MessageEmbed> buildAll(boolean ellipsis, SplitPolicy... policy) {
        original.setDescription(null);

        final int originalLength = original.length();
        final int descMaxLength = MessageEmbed.DESCRIPTION_MAX_LENGTH - (ellipsis ? ELLIPSIS_SYMBOL.length() : 0);
        final int firstSpaceLeft = Math.min(MessageEmbed.EMBED_MAX_LENGTH_BOT - originalLength, descMaxLength);

        if (desc.isEmpty())
            throw new UnsupportedOperationException("Cannot build a Message with no content. (You never added any content to the message)");

        LinkedList<MessageEmbed> messages = new LinkedList<>();

        if (desc.length() <= firstSpaceLeft) {
            messages.add(original.setDescription(desc.toString()).build());
            return messages;
        }

        final MessageEmbed originalBuilt = original.build();
        final EmbedBuilder example = new EmbedBuilder();
        example.setColor(originalBuilt.getColor());

        final int othersSpaceLeft = Math.min(MessageEmbed.EMBED_MAX_LENGTH_BOT - example.length(), descMaxLength);

        final EmbedBuilder exampleLast = new EmbedBuilder(example);
        final MessageEmbed.Footer footer = originalBuilt.getFooter();
        if (footer != null)
            exampleLast.setFooter(footer.getText(), footer.getIconUrl());

        final int lastSpaceLeft = Math.min(MessageEmbed.EMBED_MAX_LENGTH_BOT - exampleLast.length(), descMaxLength);
        final boolean shouldInsertFooter = othersSpaceLeft - lastSpaceLeft > 0;

        if (policy == null || policy.length == 0)
            policy = new SplitPolicy[]{ SplitPolicy.ANYWHERE };

        int currentBeginIndex = 0;

        messageLoop:
        while (currentBeginIndex < desc.length()) {
            final boolean first = currentBeginIndex == 0;
            final int length = first ? firstSpaceLeft : othersSpaceLeft;
            final boolean tryInsertFooter = shouldInsertFooter && desc.length() - currentBeginIndex <= length;

            for (SplitPolicy splitPolicy : policy) {
                int currentEndIndex = splitPolicy.nextMessage(desc, currentBeginIndex, tryInsertFooter ? lastSpaceLeft : length);
                if (currentEndIndex != -1) {
                    final boolean shouldActuallyInsertFooter = shouldInsertFooter && currentEndIndex >= desc.length();

                    final EmbedBuilder builder = new EmbedBuilder(
                                    first ? original :
                                    shouldActuallyInsertFooter ? exampleLast : example)
                            .setDescription(desc.subSequence(currentBeginIndex, currentEndIndex));

                    messages.add(builder.build());
                    currentBeginIndex = currentEndIndex;
                    continue messageLoop;
                }
            }
            throw new IllegalStateException("Failed to split the messages");
        }

        return messages;
    }

    public LinkedList<Message> sendAll(MessageChannel channel, Message replyTo) {
        return sendAll(channel, replyTo, null);
    }

    public LinkedList<Message> sendAll(MessageChannel channel, String content) {
        return sendAll(channel, null, content);
    }

    public LinkedList<Message> sendAll(MessageChannel channel) {
        return sendAll(channel, null, null);
    }

    public LinkedList<Message> sendAll(MessageChannel channel, Message replyTo, String content) {
        final ArrayList<MessageEmbed> embeds = new ArrayList<>(buildAll(true));
        final LinkedList<List<MessageEmbed>> chunks = new LinkedList<>();
        if (embeds.size() > 10) {
            for (int i = 0; i < embeds.size() / 10; i++)
                chunks.add(embeds.subList(i * 10, Math.min((i + 1) * 10, embeds.size())));
        } else chunks.add(embeds);

        LinkedList<Message> sent = new LinkedList<>();

        if (content != null) {
            final List<MessageEmbed> chunk = chunks.getFirst();
            final Message message = new MessageBuilder().setContent(content).setEmbeds(chunk).build();
            if (replyTo != null)
                sent.add(replyTo.reply(message).complete());
            else
                sent.add(channel.sendMessage(message).complete());
        } else if (replyTo != null) {
            sent.add(replyTo.reply(new MessageBuilder().setEmbeds(chunks.getFirst()).build()).complete());
        }

        for (List<MessageEmbed> chunk: content == null && replyTo == null ? chunks : chunks.subList(1, chunks.size()))
            sent.add(channel.sendMessageEmbeds(chunk).complete());

        return sent;
    }

    interface SplitPolicy {
        SplitPolicy NEWLINE = new SplitPolicy.CharSequenceSplitPolicy("\n", true);
        SplitPolicy SPACE = new SplitPolicy.CharSequenceSplitPolicy(" ", true);
        SplitPolicy ZWSP = new SplitPolicy.CharSequenceSplitPolicy("\u200b", true);
        SplitPolicy ANYWHERE = (t, b, m) -> Math.min(t.length(), b + m);

        int nextMessage(CharSequence targetString, int beginIndex, int maxLength);

        class CharSequenceSplitPolicy implements SplitPolicy {
            private final CharSequence chars;
            private final boolean remove;

            public CharSequenceSplitPolicy(CharSequence chars, boolean remove) {
                this.chars = chars;
                this.remove = remove;
            }

            @Override
            public int nextMessage(CharSequence targetString, int beginIndex, int maxLength) {
                if (targetString.length() <= maxLength + beginIndex)
                    return targetString.length();

                final int last = targetString.toString()
                        .substring(beginIndex, beginIndex + maxLength)
                        .lastIndexOf(chars.toString());

                if (last < 0) return ANYWHERE.nextMessage(targetString, beginIndex, maxLength);
                else return last + (remove ? 0 : chars.length());
            }
        }
    }
}
