package ru.zont.dsbot.core.util;

import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import ru.zont.dsbot.core.StandardTest;

import static org.junit.jupiter.api.Assertions.*;

class StringsTest extends StandardTest {
    @InjectMocks
    private final Strings instance = new Strings("strings_test");

    @Test
    void get() {
        assertEquals("Невозможно выполнить команду", instance.get("err.cannot_complete"));
    }

    @Test
    void getFallback() {
        assertEquals("#id.not.exists", instance.get("id.not.exists"));
    }

    @Test
    void has() {
        assertTrue(instance.has("err"));
        assertFalse(instance.has("err.kek.lol"));
    }

    @Test
    void extendedGet() {
        assertEquals("Хуевая команда", instance.get("err.command_name"));
    }

    @Test
    void extendedHas() {
        assertTrue(instance.has("err.command_name"));
        assertTrue(instance.has("command.test"));
        assertFalse(instance.has("command.kek"));
    }

    @Test
    void getPlural() {
        String one = "\\d+ раз";
        String few = "\\d+ раза";
        String id = "plurals.count";
        
        assertTrue(instance.getPlural(0, id).matches(one));
        assertTrue(instance.getPlural(1, id).matches(one));
        assertTrue(instance.getPlural(2, id).matches(few));
        assertTrue(instance.getPlural(3, id).matches(few));
        assertTrue(instance.getPlural(4, id).matches(few));
        assertTrue(instance.getPlural(5, id).matches(one));
        assertTrue(instance.getPlural(10, id).matches(one));
        assertTrue(instance.getPlural(11, id).matches(one));
        assertTrue(instance.getPlural(12, id).matches(one));
        assertTrue(instance.getPlural(20, id).matches(one));
        assertTrue(instance.getPlural(21, id).matches(one));
        assertTrue(instance.getPlural(22, id).matches(few));
        assertTrue(instance.getPlural(25, id).matches(one));
        assertTrue(instance.getPlural(100, id).matches(one));
        assertTrue(instance.getPlural(101, id).matches(one));
        assertTrue(instance.getPlural(102, id).matches(few));
        assertTrue(instance.getPlural(201, id).matches(one));
        assertTrue(instance.getPlural(202, id).matches(few));
    }

    @Test
    void trimSnippet() {
        String str = "Watch online";
        assertEquals("W...", Strings.trimSnippet(str, 4));
        assertEquals(str, Strings.trimSnippet(str, str.length()));
        assertEquals("Watch on...", Strings.trimSnippet(str, str.length() - 1));
        assertEquals("...", Strings.trimSnippet(str, 3));
        assertEquals("Wa", Strings.trimSnippet("Wa", 2));
        assertEquals("...", Strings.trimSnippet("Wat", 2));
        
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            sb.append("ax");
            
            int len = sb.length() - 1;
            int actual = Strings.trimSnippet(sb.toString(), len).length();
            assertTrue(len >= actual || len < 3, "len: %d; actual: %d".formatted(len, actual));
        }
    }

    @Test
    void getPluralStatic() {
        String one = "Слон";
        String few = "Слона";
        String other = "Слонов";

        assertEquals(one, Strings.getPlural(1, one, few, other));
        assertEquals(few, Strings.getPlural(2, one, few, other));
        assertEquals(few, Strings.getPlural(3, one, few, other));
        assertEquals(few, Strings.getPlural(4, one, few, other));
        assertEquals(other, Strings.getPlural(5, one, few, other));
        assertEquals(other, Strings.getPlural(10, one, few, other));
        assertEquals(other, Strings.getPlural(11, one, few, other));
        assertEquals(other, Strings.getPlural(12, one, few, other));
        assertEquals(other, Strings.getPlural(20, one, few, other));
        assertEquals(one, Strings.getPlural(21, one, few, other));
        assertEquals(few, Strings.getPlural(22, one, few, other));
        assertEquals(other, Strings.getPlural(25, one, few, other));
        assertEquals(other, Strings.getPlural(100, one, few, other));
        assertEquals(one, Strings.getPlural(101, one, few, other));
        assertEquals(few, Strings.getPlural(102, one, few, other));
        assertEquals(one, Strings.getPlural(201, one, few, other));
        assertEquals(few, Strings.getPlural(202, one, few, other));
    }
}