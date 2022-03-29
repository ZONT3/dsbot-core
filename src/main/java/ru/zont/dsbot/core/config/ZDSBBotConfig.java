package ru.zont.dsbot.core.config;

import java.io.File;

public class ZDSBBotConfig extends ZDSBConfig {
    public ZDSBBotConfig(String name, File dir, ZDSBConfig inherit) {
        super(name, dir, inherit);
    }

    public Entry botName = new Entry("Unnamed Bot");
    public Entry operators = new Entry("331524458806247426, 1337");
    public Entry cloneGlobalConfig = new Entry("false");

    public boolean isOperator(String id) {
        return operators.getValue().contains(id);
    }

    public boolean shouldCloneGlobal() {
        return cloneGlobalConfig.isTrue();
    }

    public String getBotName() {
        return botName.getValue();
    }
}
