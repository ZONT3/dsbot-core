package ru.zont.dsbot.core.config;

import java.io.File;

public class ZDSBBasicConfig extends ZDSBConfig {

    public ZDSBBasicConfig(String configName, File dir, ZDSBConfig inherit) {
        super(configName, dir, inherit);
    }

    public Entry prefix = new Entry("zdsb.");
    public Entry logChannel = new Entry("0");
    public Entry skipSearchingGuildLogChannel = new Entry("false");
    public Entry replyToMessages = new Entry("true");
    public Entry errorRepeatPeriod = new Entry("4 * 60 * 60");

    public String getPrefix() {
        return prefix.getValue();
    }

    public long getErrorRepeatPeriod() {
        return (long) errorRepeatPeriod.eval();
    }

    public boolean doSkipSearchingLogChannel() {
        return skipSearchingGuildLogChannel.isTrue();
    }
}
