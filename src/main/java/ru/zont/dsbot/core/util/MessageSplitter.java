package ru.zont.dsbot.core.util;


import net.dv8tion.jda.api.entities.Message;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Parts of code stolen from {@link net.dv8tion.jda.api.MessageBuilder}
 */
public class MessageSplitter {
    private final String content;

    public MessageSplitter(String content) {
        this.content = content;
    }

    public List<String> split(int maxLength, SplitPolicy... policy) {
        if (content.isEmpty())
            return Collections.singletonList("");

        LinkedList<String> messages = new LinkedList<>();

        if (content.length() <= maxLength) {
            messages.add(content);
            return messages;
        }

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
