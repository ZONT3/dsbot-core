package ru.zont.dsbot.core.commands;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import ru.zont.dsbot.core.GuildContext;
import ru.zont.dsbot.core.ZDSBot;
import ru.zont.dsbot.core.commands.exceptions.UnknownPermissionException;
import ru.zont.dsbot.core.config.ZDSBContextConfig;

import java.util.List;
import java.util.Objects;

public class PermissionsUtil {
    private final ZDSBot bot;
    private final GuildContext context;
    private Member member;
    private final User author;
    private final MessageChannel channel;

    public PermissionsUtil(ZDSBot bot, GuildContext context, MessageReceivedEvent event) {
        this.bot = bot;
        this.context = context;
        member = event.getMember();
        author = event.getAuthor();
        channel = event.getChannel();
    }

    public PermissionsUtil(ZDSBot bot, GuildContext context, SlashCommandInteractionEvent event) {
        this.bot = bot;
        this.context = context;
        member = event.getMember();
        author = event.getUser();
        channel = event.getChannel();
    }

    public boolean checkOperator() {
        return bot.getConfig().getOperators().contains(author.getId());
    }

    public boolean checkNotForeign() {
        return context != null && bot.getConfig().getApprovedGuilds().contains(context.getGuildId());
    }

    public boolean checkGuildAdmin() {
        checkMember();
        return member.isOwner() || member.hasPermission(Permission.ADMINISTRATOR);
    }

    public void checkMember() {
        if (member == null) member = getContextOrMain().getGuild().getMember(author);
        if (member == null) throw new UnknownPermissionException();
    }

    public boolean checkAnyRoleFrom(List<String> roles) {
        if (member == null) return false;
        return member.getRoles().stream().anyMatch(r -> roles.contains(r.getId()));
    }

    public boolean permSetAdminNotForeign() {
        return checkNotForeign() && checkGuildAdmin();
    }

    public boolean permSetAdmin() {
        return checkGuildAdmin();
    }

    public boolean permSetMessageManage() {
        if (channel.getType() == ChannelType.PRIVATE)
            return true;
        checkMember();
        if (channel instanceof TextChannel c)
            return member.hasAccess(c) && member.hasPermission(c, Permission.MESSAGE_MANAGE);
        return false;
    }

    protected ZDSBContextConfig getConfig() {
        return context != null ? context.getConfig() : bot.getGlobalConfig();
    }

    public ZDSBot getBot() {
        return bot;
    }

    public GuildContext getContext() {
        return context;
    }

    public GuildContext getContextOrMain() {
        return context != null ? context : bot.getMainGuildContext();
    }

    public Member getMember() {
        return member;
    }

    public User getAuthor() {
        return author;
    }

    public MessageChannel getChannel() {
        return channel;
    }

    public boolean permSetAdminFromMain() {
        GuildContext c = Objects.requireNonNull(getBot().getMainGuildContext());
        Member m = c.getGuild().getMember(author);
        return m != null && (m.isOwner() || m.hasPermission(Permission.ADMINISTRATOR));
    }
}
