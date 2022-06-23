package ru.zont.dsbot.core.commands;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import ru.zont.dsbot.core.GuildContext;

public class DescribedException extends RuntimeException {
    public static final int ERROR_COLOR = 0x910A0A;
    private final String description;
    private final String picture;
    private final int color;

    public DescribedException(String message) {
        this(message, null, null, ERROR_COLOR);
    }

    public DescribedException(String message, String description) {
        this(message, description, null, ERROR_COLOR);
    }

    public DescribedException(String message, String description, int color) {
        this(message, description, null, color);
    }

    public DescribedException(String message, String description, String picture, int color) {
        super(message);
        this.description = description;
        this.picture = picture;
        this.color = color;
    }

    public DescribedException(String message, Throwable cause) {
        this(message, null, null, ERROR_COLOR, cause);
    }

    public DescribedException(String message, String description, Throwable cause) {
        this(message, description, null, ERROR_COLOR, cause);
    }

    public DescribedException(String message, String description, int color, Throwable cause) {
        this(message, description, null, color, cause);
    }

    public DescribedException(String message, String description, String picture, int color, Throwable cause) {
        super(message, cause);
        this.description = description;
        this.picture = picture;
        this.color = color;
    }

    public String getTitle() {
        return getMessage();
    }

    public String getDescription() {
        return description;
    }

    public String getPicture() {
        return picture;
    }

    public int getColor() {
        return color;
    }
}
