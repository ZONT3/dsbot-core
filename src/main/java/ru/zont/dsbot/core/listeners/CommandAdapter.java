package ru.zont.dsbot.core.listeners;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.jetbrains.annotations.NotNull;
import ru.zont.dsbot.core.GuildContext;
import ru.zont.dsbot.core.commands.Input;

public abstract class CommandAdapter extends GuildListenerAdapter {
    public CommandAdapter(GuildContext context) {
        super(context);
    }

    @Override
    public final void onEvent(@NotNull Guild guild, @NotNull GenericEvent genericEvent) {
        if (genericEvent instanceof final MessageReceivedEvent event) {
            final Message message = event.getMessage();
            final String content = shouldCheckByRawContent() ? message.getContentDisplay() : message.getContentRaw();


        }
    }

    public abstract void onCall(Input input);

    public boolean shouldCheckByRawContent() {
        return false;
    }
}
