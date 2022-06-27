package ru.zont.dsbot.core.commands.impl.execution;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import ru.zont.dsbot.core.GuildContext;
import ru.zont.dsbot.core.ZDSBot;
import ru.zont.dsbot.core.commands.CommandAdapter;

public abstract class ExecBase extends CommandAdapter {
    public ExecBase(ZDSBot bot, GuildContext context) {
        super(bot, context);
    }

    @Override
    public boolean allowForeignGuilds() {
        return false;
    }

    @Override
    public boolean checkPermission(MessageReceivedEvent event) {
        return getPermissionsUtil(event).permSetAdmin();
    }
}
