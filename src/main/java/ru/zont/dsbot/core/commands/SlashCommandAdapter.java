package ru.zont.dsbot.core.commands;

import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import ru.zont.dsbot.core.GuildContext;
import ru.zont.dsbot.core.ZDSBot;
import ru.zont.dsbot.core.commands.exceptions.OnlySlashUsageException;
import ru.zont.dsbot.core.util.ResponseTarget;

public abstract class SlashCommandAdapter extends CommandAdapter {
    public SlashCommandAdapter(ZDSBot bot, GuildContext context) {
        super(bot, context);
    }

    @Override
    public void onCall(ResponseTarget replyTo, Input input, MessageReceivedEvent event, Object... params) {
        throw new OnlySlashUsageException();
    }

    public abstract void onSlashCommand(SlashCommandInteractionEvent event);

    public void onSlashCommandAutoComplete(CommandAutoCompleteInteractionEvent event) { }

    public abstract SlashCommandData getSlashCommand();

    public abstract boolean isGlobal();

    public boolean checkPermission(SlashCommandInteractionEvent event) {
        return checkPermission(getPermissionsUtil(event));
    }

    public final PermissionsUtil getPermissionsUtil(SlashCommandInteractionEvent event) {
        return new PermissionsUtil(getBot(), getContext(), event);
    }

    @Override
    public final boolean allowGlobal() {
        return isGlobal();
    }

    @Override
    public boolean allowGuilds() {
        return !isGlobal();
    }
}
