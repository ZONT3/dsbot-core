package ru.zont.dsbot.core.config;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

public class ZDSBBotConfig extends ZDSBConfig {
    public ZDSBBotConfig(String name, File dir, ZDSBConfig inherit) {
        super(name, dir, inherit);
    }

    public Entry botName = new Entry("Unnamed Bot");
    public Entry operators = new Entry("331524458806247426, 1337");
    public Entry approvedGuilds = new Entry("331526118635208716, 843501832126070792");
    public Entry cloneGlobalConfig = new Entry("false");
    public Entry pythonPath = new Entry("python");
    public Entry scriptsDir = new Entry("scripts");

    public boolean isOperator(String id) {
        return operators.getValue().contains(id);
    }

    public boolean shouldCloneGlobal() {
        return cloneGlobalConfig.isTrue();
    }

    public String getBotName() {
        return botName.getValue();
    }

    public Set<String> getOperators() {
        return new HashSet<>(operators.toList());
    }

    public Set<String> getApprovedGuilds() {
        return new HashSet<>(approvedGuilds.toList());
    }
}
