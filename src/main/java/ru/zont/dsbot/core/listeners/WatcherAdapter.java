package ru.zont.dsbot.core.listeners;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.GenericEvent;
import ru.zont.dsbot.core.GuildContext;
import ru.zont.dsbot.core.ZDSBot;
import ru.zont.dsbot.core.util.MessageBatch;
import ru.zont.dsbot.core.util.ResponseTarget;

import java.util.*;
import java.util.concurrent.ExecutionException;

public abstract class WatcherAdapter extends GuildListenerAdapter {
    public static final int MAX_MESSAGES = 50;
    private TextChannel channel = null;

    private MessageBatch messages;
    private Timer timer;

    public WatcherAdapter(ZDSBot bot, GuildContext context) {
        super(bot, context);
    }

    public abstract List<MessageEmbed> getMessages();

    public abstract String getChannelId();

    public abstract String getGuildId();

    private long getPeriod() {
        return 10_000;
    }

    public boolean doClearChannel() {
        return false;
    }

    public boolean doSearchExistingMessages() {
        return true;
    }

    @Override
    public final void init(Guild guild) {
        assert getContext() != null;
        final String channelId = getChannelId();
        channel = getContext().findChannel(channelId);

        if (channel == null)
            throw new RuntimeException("Cannot find corresponding channel: %s".formatted(channelId));

        initMessages();

        if (messages == null)
            throw new RuntimeException("Cannot init messages");

        final long period = getPeriod();
        timer = new Timer("%s watcher".formatted(getClass().getSimpleName()), true);
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                updateMessages();
            }
        }, period, period);
    }

    private void updateMessages() {
        messages.updateEmbeds(getMessages(), getChannel());
    }

    private void initMessages() {
        if (doClearChannel()) {
            try {
                final List<Message> recv = channel.getIterableHistory().takeAsync(MAX_MESSAGES + 1).get();
                if (recv.size() > MAX_MESSAGES)
                    throw new RuntimeException("Too many messages in specified channel. Cannot run watcher with doClearChannel == true in it.");
                recv.forEach(m -> m.delete().complete());
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        }

        final List<MessageEmbed> newMessages = getMessages();
        if (doSearchExistingMessages() && !doClearChannel()) {
            try {
                final List<Message> messageList = channel.getIterableHistory().takeAsync(MAX_MESSAGES + 1).get();
                final List<Message> sent = new ArrayList<>(newMessages.size());

                for (MessageEmbed newMessage: newMessages) {
                    Message curr = null;
                    for (Message message: messageList) {
                        final List<MessageEmbed> embeds = message.getEmbeds();
                        if (embeds.size() == 0) continue;
                        final MessageEmbed embed = embeds.get(0);
                        if (embed.getTitle() != null && embed.getTitle().equals(newMessage.getTitle())) {
                            curr = message;
                            break;
                        }
                    }

                    try {
                        if (curr != null)
                            curr = curr.editMessageEmbeds(newMessage).complete();
                    } catch (Exception ignored) { }

                    if (curr != null) {
                        sent.add(curr);
                    } else
                        sent.add(getChannel().sendMessageEmbeds(newMessage).complete());
                }

                messages = new MessageBatch(sent);
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        } else {
            messages = MessageBatch.sendNow(ResponseTarget.channel(getChannel()).responseEmbed(newMessages));
        }
    }

    @Override
    public void onEvent(Guild guild, GenericEvent event) { }

    @Override
    public final List<String> getAllowedGuilds() {
        return Collections.singletonList(getGuildId());
    }

    public final TextChannel getChannel() {
        return channel;
    }
}
