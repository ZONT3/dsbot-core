package ru.zont.dsbot.core.commands;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import ru.zont.dsbot.core.GuildContext;
import ru.zont.dsbot.core.ZDSBot;
import ru.zont.dsbot.core.commands.exceptions.UnknownPermissionException;
import ru.zont.dsbot.core.config.ZDSBBasicConfig;

public class PermissionsUtil {
    private final ZDSBot bot;
    private final GuildContext context;
    private final MessageReceivedEvent event;

    public PermissionsUtil(ZDSBot bot, GuildContext context, MessageReceivedEvent event) {
        this.bot = bot;
        this.context = context;
        this.event = event;
    }

    public boolean checkOperator() {
        return bot.getConfig().getOperators().contains(event.getAuthor().getId());
    }

    public boolean checkNotForeign() {
        return context != null && bot.getConfig().getApprovedGuilds().contains(context.getGuildId());
    }

    public boolean checkGuildAdmin() {
        final Member member = event.getMember();
        if (member == null) throw new UnknownPermissionException();
        return member.isOwner() || member.hasPermission(Permission.ADMINISTRATOR);
    }

    public boolean permSetAdminNotForeign() {
        return checkNotForeign() && checkGuildAdmin();
    }

    public boolean permSetAdmin() {
        return checkGuildAdmin();
    }

    private ZDSBBasicConfig getConfig() {
        return context != null ? context.getConfig() : bot.getGlobalConfig();
    }
}
