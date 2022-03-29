package ru.zont.dsbot.core;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;
import org.mockito.MockitoSession;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class StandardTest {
    private MockitoSession session;

    protected static void copyResource(String name, File gCfg) throws IOException {
        try (InputStreamReader reader = new InputStreamReader(StandardTest.class.getResourceAsStream(name), StandardCharsets.UTF_8);
             OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(gCfg), StandardCharsets.UTF_8)) {
            IOUtils.copy(reader, writer);
        }
    }

    @BeforeEach
    public void mockSetup() {
        session = Mockito.mockitoSession()
                .initMocks(this)
                .startMocking();
    }

    @AfterEach
    public void mockFinish() {
        session.finishMocking();
    }
}
