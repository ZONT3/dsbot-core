package ru.zont.dsbot.core.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.HashMap;
import java.util.function.Function;

public class ZDSBConfigManager<A extends ZDSBBasicConfig, B extends ZDSBBotConfig> {
    private final File dir;
    private final Class<A> instClass;
    private final Class<B> botConfigClass;
    private final HashMap<String, A> configInstanceStore = new HashMap<>();
    private B botConfig;
    private Function<String, String> commentsGetter;

    @SuppressWarnings("unchecked")
    public ZDSBConfigManager(String path) {
        this(path, (Class<A>) ZDSBBasicConfig.class);
    }

    @SuppressWarnings("unchecked")
    public ZDSBConfigManager(String path, Class<A> instClass) {
        this(path, instClass, (Class<B>) ZDSBBotConfig.class);
    }

    public ZDSBConfigManager(String path, Class<A> instClass, Class<B> botConfigClass) {
        dir = new File(path);
        this.instClass = instClass;
        this.botConfigClass = botConfigClass;
        if (dir.exists() && !dir.isDirectory())
            throw new IllegalStateException("Not a directory: " + dir);
        else if (!dir.exists())
            if (!dir.mkdirs())
                throw new RuntimeException("Cannot create config dir");
    }

    private A findConfigInst(String name, A inherit, boolean cloneInherited) {
        if (configInstanceStore.containsKey(name))
            return configInstanceStore.get(name);

        A instance = ZDSBConfig.newInstance(instClass, name, dir, inherit, commentsGetter, cloneInherited);
        configInstanceStore.put(name, instance);

        return instance;
    }

    public A guildConfig(String id) {
        return findConfigInst("guild-" + id, globalConfig(), botConfig().shouldCloneGlobal());
    }

    public A globalConfig() {
        return findConfigInst("global", null, false);
    }

    public B botConfig() {
        if (botConfig == null) {
            botConfig = ZDSBConfig.newInstance(botConfigClass, "config", dir, null, commentsGetter);
            botConfig.setCommentsGetter(commentsGetter);
        }
        return botConfig;
    }

    public void setCommentsGetter(Function<String, String> commentsGetter) {
        this.commentsGetter = commentsGetter;

        if (botConfig != null)
            botConfig.setCommentsGetter(commentsGetter);

        for (A inst: configInstanceStore.values())
            inst.setCommentsGetter(commentsGetter);
    }
}
