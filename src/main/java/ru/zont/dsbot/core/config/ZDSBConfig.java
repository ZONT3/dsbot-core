package ru.zont.dsbot.core.config;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;

public class ZDSBConfig {
    private static final Logger log = LoggerFactory.getLogger(ZDSBConfig.class);

    private final File configFile;
    private final String configName;
    private long loadedTimestampInherited;
    private long loadedTimestamp;
    private Properties loadedProperties;
    private final ZDSBConfig inherit;
    private HashMap<String, Entry> fields;
    
    private final HashMap<String, Long> observedTimestamps = new HashMap<>();

    private boolean shouldCloneInherited = false;
    private Function<String, String> commentsGetter;

    public ZDSBConfig(String configName, File dir, ZDSBConfig inherit) {
        this.inherit = inherit;
        this.configName = configName;
        configFile = new File(dir, configName + ".properties");

        if (configFile.exists() && !configFile.isFile()) {
            log.warn("Found not-a-file with required config filename, removing it...");
            try {
                FileUtils.forceDelete(configFile);
                log.info("Successfully deleted bad file/dir");
            } catch (IOException e) {
                throw new IllegalStateException("Cannot delete bad file/dir", e);
            }
        } else if (!configFile.exists()) {
            try {
                FileUtils.createParentDirectories(configFile);
            } catch (IOException e) {
                throw new RuntimeException("Cannot create config parent directories", e);
            }
        }
    }

    public static <T extends ZDSBConfig> T newInstance(Class<T> klass, String name, File dir, T inherit, Function<String, String> commentsGetter) {
        return newInstance(klass, name, dir, inherit, commentsGetter, false);
    }

    public static <T extends ZDSBConfig> T newInstance(Class<T> klass, String name, File dir, T inherit, Function<String, String> commentsGetter, boolean shouldCloneInherited) {
        T instance;
        try {
            Constructor<T> constructor = klass.getDeclaredConstructor(String.class, File.class, ZDSBConfig.class);
            instance = constructor.newInstance(name, dir, inherit);
            instance.setCommentsGetter(commentsGetter);
            if (shouldCloneInherited)
                instance.setShouldCloneInherited(true);
            else instance.processConfigFields();
        } catch (Exception e) {
            throw new RuntimeException("Cannot instantiate config", e);
        }
        return instance;
    }

    private HashMap<String, Entry> retrieveEntriesFields() {
        HashMap<String, Entry> res = new HashMap<>();
        for (Field field : getClass().getFields()) {
            if (Modifier.isPublic(field.getModifiers()) && field.getType().equals(Entry.class)) {
                String name = field.getName();
                try {
                    Entry value = (Entry) field.get(this);
                    res.put(name, value);
                } catch (Exception e) {
                    log.error("Cannot retrieve Entry field " + name, e);
                }
            }
        }
        return res;
    }

    protected synchronized void processConfigFields() {
        loadedProperties = new Properties();
        if (configFile.isFile()) {
            try (InputStreamReader reader = new InputStreamReader(new FileInputStream(configFile), StandardCharsets.UTF_8)) {
                loadedProperties.load(reader);
                loadedTimestamp = configFile.lastModified();
            } catch (IOException e) {
                throw new RuntimeException("Cannot load config: " + configFile, e);
            }
        }

        HashSet<String> removeEntries = new HashSet<>();
        HashMap<String, Entry> fields = retrieveEntriesFields();

        // inherit -> this
        if (inherit != null) {
            for (var e : inherit.getEntries().entrySet()) {
                final String key = e.getKey();
                Entry entry = e.getValue();
                // FILE not contains entry
                if (!loadedProperties.containsKey(key)) {
                    // Should inherit entry?
                    if (!entry.dontInherit()) {
                        if (shouldCloneInherited) {
                            // inherit -> FILE
                            loadedProperties.setProperty(key, entry.value);
                        } else {
                            // inherit -> this
                            if (fields.containsKey(key))
                                fields.get(key).value = entry.value;
                            else
                                fields.put(key, new Entry(entry.value));
                        }
                    } else {
                        removeEntries.add(key);
                    }
                }
            }
            loadedTimestampInherited = inherit.loadedTimestamp;
        }

        // defaults -> FILE
        for (var e : fields.entrySet()) {
            String key = e.getKey();
            Entry entry = e.getValue();
            String fieldValue = entry.value;

            if (removeEntries.contains(key)) {
                entry.value = null;
                continue;
            }

            if (loadedProperties.containsKey(key))
                entry.value = loadedProperties.getProperty(key);
            else if (inherit == null || shouldCloneInherited)
                loadedProperties.setProperty(key, fieldValue);
        }
        loadedProperties.forEach((k,v) -> {
            if (!fields.containsKey(k.toString())) {
                fields.put(k.toString(), new Entry(v.toString()));
            }
        });

        storeLoadedProperties();
        this.fields = fields;
    }
    
    public boolean wasUpdated(String observerTag) {
        final long timestamp = observedTimestamps.getOrDefault(observerTag, 0L);
        final long lastModified = configFile.lastModified();
        observedTimestamps.put(observerTag, lastModified);
        return lastModified > timestamp;
    }

    private void storeLoadedProperties() {
        try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(configFile), StandardCharsets.UTF_8)) {
            loadedProperties.store(writer, getComments());
        } catch (IOException e) {
            throw new RuntimeException("Cannot store config: " + configFile, e);
        }
        loadedTimestamp = configFile.lastModified();
    }

    private String getComments() {
        if (commentsGetter != null)
            return commentsGetter.apply(configName);
        return "ZDSBot Config\n" +
                "Cannot retrieve exact info, must be an error in ZDSBot behaviour, " +
                "or just raw usage of ZDSBConfig or ZDSBConfigManager classes";
    }

    public String getValue(String key) {
        return getValue(key, null);
    }

    public String getValue(String key, String defaultValue) {
        updateIfNeeded();
        return loadedProperties.getProperty(key, defaultValue);
    }

    public HashMap<String, Entry> getEntries() {
        updateIfNeeded();
        return fields;
    }

    private boolean isUpdateNeeded() {
        return fields == null
                || configFile.lastModified() > loadedTimestamp
                || inherit != null && (inherit.isUpdateNeeded() || inherit.loadedTimestamp > loadedTimestampInherited);
    }

    private void updateIfNeeded() {
        if (isUpdateNeeded()) {
            processConfigFields();
            log.info("Config %s reloaded from disk".formatted(configName));
        }
    }

    public File getConfigFile() {
        return configFile;
    }

    public void setShouldCloneInherited(boolean shouldCloneGlobal) {
        this.shouldCloneInherited = shouldCloneGlobal;
        processConfigFields();
    }

    public void setCommentsGetter(Function<String, String> commentsGetter) {
        this.commentsGetter = commentsGetter;
        processConfigFields();
    }

    public class Entry {
        private final String defaultValue;
        private String value;
        private final boolean dontInherit;

        public Entry(String defaultValue) {
            this(defaultValue, false);
        }

        public Entry(String defaultValue, boolean dontInherit) {
            value = defaultValue;
            this.defaultValue = defaultValue;
            this.dontInherit = dontInherit;
        }

        public boolean dontInherit() {
            return dontInherit;
        }

        public String getValue() {
            updateIfNeeded();
            return value;
        }

        public String getDefaultValue() {
            return defaultValue;
        }

        public boolean isTrue() {
            if (getValue() == null) return false;
            final String value = getValue().strip();
            return "true".equals(value) ||
                    "yes".equals(value) ||
                    "t".equals(value) ||
                    "y".equals(value) ||
                    "on".equals(value);
        }

        public boolean isFalse() {
            if (getValue() == null) return false;
            final String value = getValue().strip();
            return "false".equals(value) ||
                    "no".equals(value) ||
                    "f".equals(value) ||
                    "n".equals(value) ||
                    "off".equals(value);
        }

        public int getInt() {
            try {
                return Integer.parseInt(getValue());
            } catch (Exception e) {
                return 0;
            }
        }

        public long getLong() {
            try {
                return Long.parseLong(getValue());
            } catch (Exception e) {
                return 0L;
            }
        }

        public float geFloat() {
            try {
                return Float.parseFloat(getValue());
            } catch (Exception e) {
                return 0f;
            }
        }

        public double getDouble() {
            try {
                return Double.parseDouble(getValue());
            } catch (Exception e) {
                return 0.;
            }
        }

        public List<String> toList() {
            return toList(" *, *");
        }

        public List<String> toList(String delim) {
            final String value = getValue();
            if (value == null)
                return new LinkedList<>();
            return List.of(value.split(delim));
        }

        public void setValue(String value) {
            for (Map.Entry<String, Entry> e : getEntries().entrySet()) {
                if (e.getValue() == Entry.this) {
                    synchronized (ZDSBConfig.this) {
                        loadedProperties.setProperty(e.getKey(), value);
                        this.value = value;
                        storeLoadedProperties();
                    }
                    return;
                }
            }
            throw new RuntimeException("Cannot find this field in ZDSBConfig " + configName);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null) return false;

            if (o instanceof CharSequence)
                return o.equals(value);

            if (getClass() != o.getClass()) return false;
            Entry entry = (Entry) o;
            return Objects.equals(value, entry.value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(value);
        }
    }
}
