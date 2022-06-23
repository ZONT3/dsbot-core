package ru.zont.dsbot.core.util;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MessageSplitterTest {

    @Test
    void split1() {
        String content = """
                Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et
                dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip
                ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore
                eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia
                deserunt mollit anim id est laborum.""";
        MessageSplitter spl = new MessageSplitter(content);
        List<String> split = spl.split(200, MessageSplitter.SplitPolicy.NEWLINE, MessageSplitter.SplitPolicy.SPACE);

        assertTrue(content.length() / 200 <= split.size(), "Chunks count lower than expected");
        assertFalse(split.get(0).endsWith("al"), split.get(0));
        assertTrue(split.get(0).endsWith("ut"), split.get(0));
        assertTrue(split.get(1).startsWith("aliquip"), split.get(1));
    }

    @Test
    void split2() {
        String content = """
                Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et
                dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip
                ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore
                eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia
                deserunt mollit anim id est laborum.
                Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et
                dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip
                ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore
                eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia
                deserunt mollit anim id est laborum.
                Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et
                dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip
                ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore
                eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia
                deserunt mollit anim id est laborum.
                Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et
                dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip
                ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore
                eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia
                deserunt mollit anim id est laborum.
                """;
        MessageSplitter spl = new MessageSplitter(content);
        List<String> split = spl.split(200, MessageSplitter.SplitPolicy.NEWLINE, MessageSplitter.SplitPolicy.SPACE);

        assertTrue(content.length() / 200 <= split.size(), "Chunks count lower than expected");
        assertFalse(split.get(0).endsWith("al"), split.get(0));
        assertTrue(split.get(0).endsWith("ut"), split.get(0));
        assertTrue(split.get(1).startsWith("aliquip"), split.get(1));
        assertFalse(split.get(split.size() - 1).endsWith("\n"), "Last chunk is ending with newline");
    }

    @Test
    void split3() {
        String content = """
                Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et
                dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip
                ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore
                eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia
                deserunt mollit anim id est laborum.""";
        MessageSplitter spl = new MessageSplitter(content);
        List<String> split = spl.split(2000, MessageSplitter.SplitPolicy.NEWLINE, MessageSplitter.SplitPolicy.SPACE);

        assertEquals(1, split.size(), "Chunks count not 1");
    }
}