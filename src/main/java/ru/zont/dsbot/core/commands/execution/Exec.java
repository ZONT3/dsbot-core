package ru.zont.dsbot.core.commands.execution;

import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.apache.commons.cli.Options;
import org.apache.commons.codec.Charsets;
import org.apache.commons.io.IOUtils;
import ru.zont.dsbot.core.GuildContext;
import ru.zont.dsbot.core.commands.ArgumentTokenizer;
import ru.zont.dsbot.core.commands.CommandAdapter;
import ru.zont.dsbot.core.commands.Input;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Scanner;

public class Exec extends CommandAdapter {
    public Exec(GuildContext context) {
        super(context);
    }

    @Override
    public void onCall(MessageReceivedEvent event, Input input, Object... params) {
        MessageChannel channel;
        if (event != null) channel = event.getChannel();
        else {
            if (params.length >= 1 && params[0] instanceof final MessageChannel ch)
                channel = ch;
            else throw new IllegalStateException("Channel not provided by external call");
        }

        String execStr = input.getUnrecognizedAsIs();
        try {
            final Process proc = Runtime.getRuntime().exec(ArgumentTokenizer.tokenize(execStr).toArray(String[]::new));
            Thread t = new Thread(() -> {
                Scanner s = new Scanner(proc.getInputStream());
                while (s.hasNext())
                    System.out.println(s.nextLine());
            });
            t.setPriority(10);
            t.start();
            proc.waitFor();
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Options getOptions() {
        return new Options()
                .addOption("i", "interpreter", true, "Specify environment");
    }

    @Override
    public String getName() {
        return "exec";
    }

    @Override
    public String getShortDesc() {
        return "Spawn subprocess and handle output";
    }
}
