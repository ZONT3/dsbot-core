package ru.zont.dsbot.core.util;


import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Parts of code stolen from {@link net.dv8tion.jda.api.MessageBuilder}
 */
public class MessageSplitter {
    public static List<MessageEmbed> embeds(String content, MessageEmbed base) {
        return new MessageSplitter(content).splitEmbeds(base);
    }

    public static List<MessageEmbed> embeds(String content, EmbedBuilder base) {
        return new MessageSplitter(content).splitEmbeds(base);
    }

    public static List<String> strings(String content) {
        return strings(content, Message.MAX_CONTENT_LENGTH);
    }

    public static List<String> strings(String content, int maxLength, SplitPolicy... policy) {
        return new MessageSplitter(content).split(maxLength, policy);
    }

    private final String content;

    public MessageSplitter(String content) {
        this.content = content;
    }

    public List<MessageEmbed> splitEmbeds(MessageEmbed embed) {
        return splitEmbeds(new EmbedBuilder(embed));
    }

    public List<MessageEmbed> splitEmbeds(EmbedBuilder builder, SplitPolicy... policy) {
        builder.setDescription("");
        final int leftLen = Math.min(
                MessageEmbed.DESCRIPTION_MAX_LENGTH,
                MessageEmbed.EMBED_MAX_LENGTH_BOT - builder.length());

        List<String> split = split(leftLen, policy);

        EmbedBuilder top;
        EmbedBuilder mid;
        EmbedBuilder bot;
        if (split.size() > 1) {
            top = new EmbedBuilder(builder);
            bot = new EmbedBuilder(builder);
            mid = new EmbedBuilder(builder);
            for (EmbedBuilder bb : List.of(bot, mid)) {
                bb.setTitle(null, null);
                bb.setAuthor(null, null);
                bb.setImage(null);
                bb.setThumbnail(null);
            }
            for (EmbedBuilder bb : List.of(top, mid)) {
                bb.setTimestamp(null);
                bb.setFooter(null, null);
                bb.clearFields();
            }
        } else {
            top = new EmbedBuilder(builder);
            mid = null;
            bot = null;
        }

        LinkedList<MessageEmbed> res = new LinkedList<>();
        res.add(top.setDescription(split.get(0)).build());

        if (bot != null) {
            for (String s : split.subList(1, split.size() - 1))
                res.add(new EmbedBuilder(mid).setDescription(s).build());
            res.add(bot.setDescription(split.get(split.size() - 1)).build());
        }

        return res;
    }

    public List<String> split(int maxLength, SplitPolicy... policy) {
        if (content.isEmpty())
            return Collections.singletonList("");

        if (content.length() <= maxLength)
            return Collections.singletonList(content);

        LinkedList<String> messages = new LinkedList<>();

        if (policy == null || policy.length == 0)
            policy = new SplitPolicy[]{SplitPolicy.ANYWHERE};

        int currentBeginIndex = 0;

        while (currentBeginIndex < content.length() - maxLength) {
            int maxEndIndex = -1;
            for (SplitPolicy splitPolicy : policy) {
                int currentEndIndex = splitPolicy.nextMessage(currentBeginIndex, content, maxLength);
                if (currentEndIndex > maxEndIndex)
                    maxEndIndex = currentEndIndex;
            }
            if (maxEndIndex != -1) {
                String substring = trimByPolicy(content.substring(currentBeginIndex, maxEndIndex), policy);
                messages.add(substring);
                currentBeginIndex = maxEndIndex;
                continue;
            }
            throw new IllegalStateException("Failed to split the messages");
        }

        if (currentBeginIndex < content.length())
            messages.add(trimByPolicy(content.substring(currentBeginIndex), policy));

        return messages;
    }

    @NotNull
    private String trimByPolicy(String substring, SplitPolicy[] policy) {
        int lastLength;
        do {
            lastLength = substring.length();
            for (SplitPolicy p : policy)
                substring = p.trim(substring);
        } while (lastLength != substring.length());
        return substring;
    }

    public interface SplitPolicy {
        /**
         * Splits on newline chars {@code `\n`}.
         */
        SplitPolicy NEWLINE = new SplitPolicy.CharSequenceSplitPolicy("\n", true);

        /**
         * Splits on space chars {@code `\u0020`}.
         */
        SplitPolicy SPACE = new SplitPolicy.CharSequenceSplitPolicy(" ", true);

        /**
         * Splits exactly after 2000 chars.
         */
        SplitPolicy ANYWHERE = (i, c, m) -> Math.min(i + m, c.length());

        /**
         * Creates a new {@link SplitPolicy} splitting on the specified chars.
         *
         * @param chars  the chars to split on
         * @param remove whether to remove the chars when splitting on them
         * @return a new {@link SplitPolicy}
         */
        @Nonnull
        static SplitPolicy onChars(@Nonnull CharSequence chars, boolean remove) {
            return new SplitPolicy.CharSequenceSplitPolicy(chars, remove);
        }

        /**
         * Default {@link SplitPolicy} implementation. Splits on a specified {@link CharSequence}.
         */
        class CharSequenceSplitPolicy implements SplitPolicy {
            private final boolean remove;
            private final CharSequence chars;

            private CharSequenceSplitPolicy(@Nonnull final CharSequence chars, final boolean remove) {
                this.chars = chars;
                this.remove = remove;
            }

            @Override
            public int nextMessage(final int currentBeginIndex, final String content, int maxLength) {
                int searchEndIndex = currentBeginIndex + maxLength - (this.remove ? this.chars.length() : 0);
                int currentEndIndex = content
                        .substring(currentBeginIndex, Math.min(content.length(), searchEndIndex))
                        .lastIndexOf(this.chars.toString()) + currentBeginIndex;
                if (currentEndIndex < 0) {
                    return -1;
                } else {
                    return currentEndIndex + this.chars.length();
                }
            }

            @Override
            public String trim(String str) {
                String quote = Pattern.quote(chars.toString());
                return str.replaceFirst("^%s+".formatted(quote), "")
                        .replaceFirst("%s+$".formatted(quote), "");
            }
        }

        int nextMessage(int currentBeginIndex, String content, int maxLength);

        default String trim(String str) {
            return str;
        }
    }
}
