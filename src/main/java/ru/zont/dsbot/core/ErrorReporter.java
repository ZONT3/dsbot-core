package ru.zont.dsbot.core;

public class ErrorReporter {
    private final GuildContext context;

    public ErrorReporter(GuildContext context) {
        this.context = context;
    }

    private void newReport(ResponseTarget reportTo, String title, String description, String picture, int color, Throwable cause) {

    }
}
