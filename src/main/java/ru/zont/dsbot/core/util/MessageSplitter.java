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
    private String lhsInsertion;
    private String rhsInsertion;
    private boolean keepTitle = false;
    private boolean numerateTitle = false;

    public static List<MessageEmbed> embeds(CharSequence content, MessageEmbed base) {
        return new MessageSplitter(content).splitEmbeds(base);
    }

    public static List<MessageEmbed> embeds(CharSequence content, EmbedBuilder base) {
        return new MessageSplitter(content).splitEmbeds(base);
    }

    public static List<String> strings(CharSequence content) {
        return strings(content, Message.MAX_CONTENT_LENGTH);
    }

    public static List<String> strings(CharSequence content, int maxLength, SplitPolicy... policy) {
        return new MessageSplitter(content).split(maxLength, policy);
    }

    private final String content;

    public MessageSplitter(CharSequence content) {
        this.content = content.toString();
    }

    public List<MessageEmbed> splitEmbeds(MessageEmbed embed) {
        return splitEmbeds(new EmbedBuilder(embed));
    }

    public List<MessageEmbed> splitEmbeds(MessageEmbed embed, SplitPolicy... policy) {
        return splitEmbeds(new EmbedBuilder(embed), policy);
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
                if (!keepTitle)
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

        MessageEmbed topEmbed = top.setDescription(split.get(0)).build();
        if (keepTitle && numerateTitle && topEmbed.getTitle() != null)
            topEmbed = new EmbedBuilder(topEmbed).setTitle(topEmbed.getTitle() + " #1").build();
        res.add(topEmbed);

        int i = 2;
        if (bot != null) {
            for (String s : split.subList(1, split.size() - 1)) {
                MessageEmbed embed = new EmbedBuilder(mid).setDescription(s).build();
                if (keepTitle && numerateTitle && embed.getTitle() != null)
                    embed = new EmbedBuilder(embed).setTitle(embed.getTitle() + " #" + i).build();
                res.add(embed);
                i++;
            }

            MessageEmbed botEmbed = bot.setDescription(split.get(split.size() - 1)).build();
            if (keepTitle && numerateTitle && botEmbed.getTitle() != null)
                botEmbed = new EmbedBuilder(botEmbed).setTitle(botEmbed.getTitle() + " #" + i).build();
            res.add(botEmbed);
        }

        return res;
    }

    public void insert(String lhs, String rhs) {
        this.lhsInsertion = lhs;
        this.rhsInsertion = rhs;
    }

    public List<String> split(int maxLength, SplitPolicy... policy) {
        if (content.isEmpty())
            return Collections.singletonList("");

        if (lhsInsertion != null && rhsInsertion != null)
            maxLength -= lhsInsertion.length() + rhsInsertion.length();
        else {
            lhsInsertion = "";
            rhsInsertion = "";
        }

        if (maxLength <= 10)
            throw new IllegalStateException("Max length is too small");

        if (content.length() <= maxLength)
            return Collections.singletonList(content);

        LinkedList<String> messages = new LinkedList<>();

        if (policy == null || policy.length == 0)
            policy = new SplitPolicy[]{SplitPolicy.NEWLINE, SplitPolicy.SPACE};

        int currentBeginIndex = 0;

        while (currentBeginIndex < content.length() - maxLength) {
            int maxEndIndex = -1;
            for (SplitPolicy splitPolicy : policy) {
                int currentEndIndex = splitPolicy.nextMessage(currentBeginIndex, content, maxLength);
                if (currentEndIndex > maxEndIndex)
                    maxEndIndex = currentEndIndex;
            }
            if (maxEndIndex != -1) {
                if (messages.size() > 0) {
                    int last = messages.size() - 1;
                    messages.set(last, messages.get(last) + rhsInsertion);
                }
                String substring = trimByPolicy(content.substring(currentBeginIndex, maxEndIndex), policy);
                messages.add(currentBeginIndex > 0 ? lhsInsertion + substring : substring);
                currentBeginIndex = maxEndIndex;
                continue;
            }
            throw new IllegalStateException("Failed to split the messages");
        }

        if (currentBeginIndex < content.length()) {
            if (messages.size() > 0) {
                int last = messages.size() - 1;
                messages.set(last, messages.get(last) + rhsInsertion);
            }
            String substring = trimByPolicy(content.substring(currentBeginIndex), policy);
            messages.add(lhsInsertion + substring);
        }

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

    public void setKeepTitle(boolean keep, boolean numerate) {
        keepTitle = keep;
        numerateTitle = numerate;
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
