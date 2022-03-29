package ru.zont.dsbot.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ZDSBotTest extends StandardTest {
    @Test
    void getCoreVersion() {
        String ver = ZDSBot.loadCoreVersion();
        assertNotNull(ver, "Version retrieval error");
        assertNotEquals("", ver.trim());
        assertNotEquals("UNKNOWN", ver);
        assertTrue(ver.matches("\\d+\\.\\d+.*"), "Version format mismatch: %s".formatted(ver));
    }
}