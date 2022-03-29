package ru.zont.dsbot.core.config;

import java.io.File;

public class ZDSBBasicConfig extends ZDSBConfig {
    public ZDSBBasicConfig(String configName, File dir, ZDSBConfig inherit) {
        super(configName, dir, inherit);
    }

    public Entry prefix = new Entry("zdsb.");

    public String getPrefix() {
        return prefix.getValue();
    }
}
