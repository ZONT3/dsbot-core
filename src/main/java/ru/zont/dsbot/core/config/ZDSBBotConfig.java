package ru.zont.dsbot.core.config;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

public class ZDSBBotConfig extends ZDSBConfig {
    public ZDSBBotConfig(String name, File dir, ZDSBConfig inherit) {
        super(name, dir, inherit);
    }

    public Entry botName = new Entry("Unnamed Bot");
    public Entry operators = new Entry("331524458806247426");
    public Entry approvedGuilds = new Entry();
    public Entry cloneGlobalConfig = new Entry("false");
    public Entry pythonPath = new Entry("python");
    public Entry scriptsDir = new Entry("scripts");
    public Entry allowExecution = new Entry("false");
    public Entry mainGuild = new Entry("331526118635208716");
    public Entry excludedCommands = new Entry();

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
        HashSet<String> ids = new HashSet<>(approvedGuilds.toList());
        String mainId = mainGuild.getString();
        if (mainId != null) ids.add(mainId);
        return ids;
    }
}
