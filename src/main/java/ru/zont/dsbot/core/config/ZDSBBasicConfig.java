package ru.zont.dsbot.core.config;

import java.io.File;

public class ZDSBBasicConfig extends ZDSBConfig {

    public ZDSBBasicConfig(String configName, File dir, ZDSBConfig inherit) {
        super(configName, dir, inherit);
    }

    public Entry prefix = new Entry("zdsb.");
    public Entry logChannel = new Entry("0");
    public Entry replyToMessages = new Entry("true");

    public String getPrefix() {
        return prefix.getValue();
    }
}
