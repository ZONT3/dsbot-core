package ru.zont.dsbot.core.commands;

import org.apache.commons.cli.Options;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class InputTest {

    private static CommandAdapter mock;

    @BeforeAll
    static void getCommandAdapter() {
        mock = mock(CommandAdapter.class);
        when(mock.getOptions()).thenReturn(new Options()
                .addOption("o", "")
                .addOption("p", "")
                .addOption("l", "longop", true, "")
                .addOption("v", "vle", true, "")
        );
        when(mock.doStopAtNonOption()).thenReturn(true);
    }

    @Test
    void getUnrecognizedBare() {
        String code = """
                ```py
                import os
                import time as t
                for i in range(5):
                    print(i)
                    t.sleep(2)
                ```""";
        Input input = new Input("command %s".formatted(code));
        input.applyAdapter(null, mock);
        assertEquals(code, input.getContentUnrecogrized());
    }

    @Test
    void getUnrecognizedAsIs() {
        Input input = new Input("command -op --longop \"val ue\" -v val right over");
        input.applyAdapter(null, mock);
        assertEquals("right over", input.getContentUnrecogrized());
    }

    @Test
    void getUnrecognizedAsIsBig() {
        String code = """
                ```py
                import os
                import time as t
                for i in range(5):
                    print(i)
                    t.sleep(2)
                ```""";
        Input input = new Input("command -op --longop \"val ue\" -v val %s".formatted(code));
        input.applyAdapter(null, mock);
        assertEquals(code, input.getContentUnrecogrized());
    }

    @Test
    void getUnrecognizedAsIsConfuse() {
        Input input = new Input("exec python -m pip install ipython");
        input.applyAdapter(null, mock);
        assertEquals("python -m pip install ipython", input.getContentUnrecogrized());
    }
}