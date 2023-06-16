package ru.zont.dsbot.core.config;

import net.dv8tion.jda.api.entities.MessageChannel;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public class ZDSBConfig {
    private static final Logger log = LoggerFactory.getLogger(ZDSBConfig.class);
    public static final List<String> TRUE_VALUES = List.of("true", "yes", "y", "on");
    public static final List<String> FALSE_VALUES = List.of("false", "no", "f", "n", "off");

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

    public static boolean checkConfigEntries(@Nullable Predicate<String> predicate, Entry... list) {
        Predicate<String> finalPredicate = predicate != null ? predicate : String::isBlank;

        return Arrays.stream(list).noneMatch(e -> finalPredicate.test(e.getValue()));
    }

    public boolean checkConfigEntriesStartsWith(String startsWith, @Nullable Predicate<String> predicate) {
        final Entry[] entries = Arrays.stream(this.getClass().getDeclaredFields())
                .filter(f ->
                        f.getType().equals(Entry.class) &&
                        f.canAccess(this) &&
                        f.getName().startsWith(startsWith))
                .map(f -> {
                    try {
                        return ((Entry) f.get(this));
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException(e);
                    }
                })
                .toArray(Entry[]::new);

        return checkConfigEntries(predicate, entries);
    }

    public class Entry {
        private final String defaultValue;
        private String value;
        private final boolean dontInherit;

        public Entry() {
            this("", false);
        }

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

        @Nonnull
        public String getValue() {
            updateIfNeeded();
            return value;
        }

        @Nullable
        public String getString() {
            String res = getValue();
            if (res.isBlank())
                return null;
            return res;
        }

        @Nonnull
        public String getString(String fallback) {
            String res = getValue();
            if (res.isBlank())
                return fallback;
            return res;
        }

        public String getDefaultValue() {
            return defaultValue;
        }

        public boolean isTrue() {
            final String string = getString();
            if (string == null) return false;
            final String value = string.strip();
            return TRUE_VALUES.contains(value);
        }

        public boolean isFalse() {
            final String string = getString();
            if (string == null) return false;
            final String value = string.strip();
            return FALSE_VALUES.contains(value);
        }

        public int getInt() {
            try {
                return Integer.parseInt(getValue());
            } catch (Exception e) {
                try {
                    return (int) eval();
                } catch (Exception e1) {
                    return 0;
                }
            }
        }

        public long getLong() {
            try {
                return Long.parseLong(getValue());
            } catch (Exception e) {
                try {
                    return (long) eval();
                } catch (Exception e1) {
                    return 0L;
                }
            }
        }

        public float geFloat() {
            try {
                return Float.parseFloat(getValue());
            } catch (Exception e) {
                try {
                    return (float) eval();
                } catch (Exception e1) {
                    return 0f;
                }
            }
        }

        public double getDouble() {
            try {
                return Double.parseDouble(getValue());
            } catch (Exception e) {
                try {
                    return eval();
                } catch (Exception e1) {
                    return 0.;
                }
            }
        }

        public double eval() {
            try {
                return eval(getValue());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        public List<String> toList() {
            return toList(" *, *");
        }

        public List<String> toList(String delim) {
            final String value = getString();
            if (value == null)
                return Collections.emptyList();
            return List.of(value.split(delim));
        }

        public void opList(Consumer<List<String>> consumer) {
            List<String> list = new ArrayList<>(toList());
            consumer.accept(list);
            setList(list);
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

        public void setList(List<? extends CharSequence> list) {
            setValue(String.join(", ", list));
        }

        public void clearValue() {
            setValue("");
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

        public static double eval(final String str) {
            return new Object() {
                int pos = -1, ch;

                void nextChar() {
                    ch = (++pos < str.length()) ? str.charAt(pos) : -1;
                }

                boolean eat(int charToEat) {
                    while (ch == ' ') nextChar();
                    if (ch == charToEat) {
                        nextChar();
                        return true;
                    }
                    return false;
                }

                double parse() {
                    nextChar();
                    double x = parseExpression();
                    if (pos < str.length()) throw new RuntimeException("Unexpected: " + (char)ch);
                    return x;
                }

                // Grammar:
                // expression = term | expression `+` term | expression `-` term
                // term = factor | term `*` factor | term `/` factor
                // factor = `+` factor | `-` factor | `(` expression `)` | number
                //        | functionName `(` expression `)` | functionName factor
                //        | factor `^` factor

                double parseExpression() {
                    double x = parseTerm();
                    for (;;) {
                        if      (eat('+')) x += parseTerm(); // addition
                        else if (eat('-')) x -= parseTerm(); // subtraction
                        else return x;
                    }
                }

                double parseTerm() {
                    double x = parseFactor();
                    for (;;) {
                        if      (eat('*')) x *= parseFactor(); // multiplication
                        else if (eat('/')) x /= parseFactor(); // division
                        else return x;
                    }
                }

                double parseFactor() {
                    if (eat('+')) return +parseFactor(); // unary plus
                    if (eat('-')) return -parseFactor(); // unary minus

                    double x;
                    int startPos = this.pos;
                    if (eat('(')) { // parentheses
                        x = parseExpression();
                        if (!eat(')')) throw new RuntimeException("Missing ')'");
                    } else if ((ch >= '0' && ch <= '9') || ch == '.') { // numbers
                        while ((ch >= '0' && ch <= '9') || ch == '.') nextChar();
                        x = Double.parseDouble(str.substring(startPos, this.pos));
                    } else if (ch >= 'a' && ch <= 'z') { // functions
                        while (ch >= 'a' && ch <= 'z') nextChar();
                        String func = str.substring(startPos, this.pos);
                        if (eat('(')) {
                            x = parseExpression();
                            if (!eat(')')) throw new RuntimeException("Missing ')' after argument to " + func);
                        } else {
                            x = parseFactor();
                        }
                        x = switch (func) {
                            case "sqrt" -> Math.sqrt(x);
                            case "sin" -> Math.sin(Math.toRadians(x));
                            case "cos" -> Math.cos(Math.toRadians(x));
                            case "tan" -> Math.tan(Math.toRadians(x));
                            default -> throw new RuntimeException("Unknown function: " + func);
                        };
                    } else {
                        throw new RuntimeException("Unexpected: " + (char)ch);
                    }

                    if (eat('^')) x = Math.pow(x, parseFactor()); // exponentiation

                    return x;
                }
            }.parse();
        }
    }
}
