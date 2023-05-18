package ru.zont.dsbot.core.listeners;

import net.dv8tion.jda.api.entities.*;
import ru.zont.dsbot.core.GuildContext;
import ru.zont.dsbot.core.ZDSBot;
import ru.zont.dsbot.core.util.MessageBatch;
import ru.zont.dsbot.core.util.ResponseTarget;

import javax.annotation.Nonnull;
import javax.annotation.OverridingMethodsMustInvokeSuper;
import java.util.*;
import java.util.concurrent.ExecutionException;

public abstract class DisplayedWatcherAdapter extends WatcherAdapter {
    public static final int MAX_MESSAGES = 50;

    private MessageChannel channel = null;
    private MessageBatch messages;

    public DisplayedWatcherAdapter(ZDSBot bot, GuildContext context) {
        super(bot, context);
    }

    @Nonnull
    public abstract List<MessageEmbed> getMessages();

    public abstract String getChannelId();

    public boolean doClearChannel() {
        return false;
    }

    public boolean doSearchExistingMessages() {
        return true;
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public boolean init(Guild guild) {
        if (!super.init(guild))
            return false;

        updateChannel();

        if (channel != null)
            initMessages();

        return true;
    }

    @OverridingMethodsMustInvokeSuper
    @Override
    public void update() {
        boolean needInit = channel == null;
        updateChannel();

        if (channel != null) {
            if (needInit || messages == null)
                initMessages();
            else
                messages.updateEmbeds(getMessages(), getChannel());
        }
    }

    private void updateChannel() {
        final String channelId = getChannelId();
        channel = Objects.requireNonNull(getContext()).findChannel(channelId);
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
            messages = MessageBatch.sendNow(ResponseTarget.channel(getChannel()).respondEmbeds(newMessages));
        }
    }

    protected final boolean isChannelValid() {
        updateChannel();
        return getChannel() != null;
    }

    protected final MessageChannel getChannel() {
        return channel;
    }

    @Override
    public final boolean allowGlobal() {
        return false;
    }
}
