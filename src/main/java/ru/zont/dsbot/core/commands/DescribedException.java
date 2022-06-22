package ru.zont.dsbot.core.commands;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import ru.zont.dsbot.core.GuildContext;

public class DescribedException extends RuntimeException {
    private final String description;
    private final String picture;

    public DescribedException(String message, String description) {
        this(message, description, (String) null);
    }

    public DescribedException(String message, String description, String picture) {
        super(message);
        this.description = description;
        this.picture = picture;
    }

    public DescribedException(String message, String description, Throwable cause) {
        this(message, description, null, cause);
    }

    public DescribedException(String message, String description, String picture, Throwable cause) {
        super(message, cause);
        this.description = description;
        this.picture = picture;
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
}
