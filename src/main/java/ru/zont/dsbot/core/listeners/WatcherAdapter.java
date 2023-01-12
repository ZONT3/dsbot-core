package ru.zont.dsbot.core.listeners;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.GenericEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.zont.dsbot.core.GuildContext;
import ru.zont.dsbot.core.ZDSBot;

import javax.annotation.OverridingMethodsMustInvokeSuper;
import java.util.Timer;
import java.util.TimerTask;

public abstract class WatcherAdapter extends GuildListenerAdapter {
    private static final Logger log = LoggerFactory.getLogger(WatcherAdapter.class);
    public static final int DEFAULT_PERIOD = 25;

    private static int globalIdx = 0;

    @SuppressWarnings("FieldCanBeLocal")
    private Timer timer;
    public WatcherAdapter(ZDSBot bot, GuildContext context) {
        super(bot, context);
    }

    public abstract void update();

    public long getPeriod() {
        return DEFAULT_PERIOD;
    }

    @OverridingMethodsMustInvokeSuper
    @Override
    public boolean init(Guild guild) {
        globalIdx++;
        final long period = getPeriod();
        if (period < DEFAULT_PERIOD)
            log.warn("Too short period ({}s) for watcher {}. " +
                     "Should be not less than {}, rate limits will be occurred otherwise",
                    period, getClass().getName(), DEFAULT_PERIOD);

        timer = new Timer("%s watcher".formatted(getClass().getSimpleName()), true);
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    update();
                } catch (Exception e) {
                    getErrorReporter().reportError(null, e);
                }
            }
        }, 5000L + 500L * globalIdx, period * 1000L);

        return true;
    }

    @Override
    public void onEvent(Guild guild, GenericEvent event) { }
}
