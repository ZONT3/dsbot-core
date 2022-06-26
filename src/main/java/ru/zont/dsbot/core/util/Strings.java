package ru.zont.dsbot.core.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;

public class Strings {
    private static final Logger log = LoggerFactory.getLogger(Strings.class);

    private static final ResourceBundle STR_CORE = ResourceBundle.getBundle("strings_core", new UTF8Control());
    public static Strings CORE = new Strings("strings_core");

    private final ResourceBundle strLocal;

    public static final int DS_CODE_BLOCK_LINE_LENGTH = 60;

    public Strings() {
        this(null);
    }

    public Strings(String bundleName) {
        if (bundleName == null || STR_CORE.getBaseBundleName().equals(bundleName)) {
            strLocal = null;
            if (bundleName == null)
                log.warn("No local strings file defined, falling back to CORE's");
        } else this.strLocal = ResourceBundle.getBundle(bundleName, new UTF8Control());
    }

    public static String millisToDuration(long millis) {
        return CORE.millisToDuration(millis, 5, false);
    }

    public String millisToDuration(long millis, int maxFieldCount, boolean localise) {
        final Duration duration = Duration.ofMillis(millis);

        long sec = duration.toSecondsPart();
        float secFloat = sec + duration.toMillisPart() / 1000f;
        int min = duration.toMinutesPart();
        int hr = duration.toHoursPart();
        int day = duration.toHoursPart();
        int wk = (int) (duration.toDays() / 7);

        final List<String> strings;
        if (localise)
            strings = List.of(
                    "duration.sec", "duration.min", "duration.hr",
                    "duration.day", "duration.wk");
        else strings = List.of("sec", "min", "hr", "d", "w");

        final List<? extends Number> numbers = List.of(secFloat, min, hr, day, wk);

        final LinkedList<String> out = new LinkedList<>();
        for (int i = 0; i < strings.size(); i++) {
            String s = localise ? get(strings.get(i)) : strings.get(i);
            Number n = numbers.get(i);

            if (i >= maxFieldCount || n instanceof Integer && n.intValue() == 0)
                break;

            final String format;
            if (n instanceof Float) format = "%.03f %s";
            else format = "%d %s";
            out.add(String.format(Locale.ROOT, format, n, s));
        }
        final ArrayList<String> outReversed = new ArrayList<>();
        final Iterator<String> it = out.descendingIterator();
        while (it.hasNext()) outReversed.add(it.next());

        return String.join(" ", outReversed);
    }

    public String get(String id, Object... args) {
        final String str;
        if (strLocal != null && strLocal.containsKey(id))
            str = strLocal.getString(id);
        else if (STR_CORE.containsKey(id))
            str = STR_CORE.getString(id);
        else {
            log.warn("String not found: {}", id);
            return "#%s".formatted(id);
        }

        if (args.length > 0)
            return String.format(str, args);
        else return str;
    }

    public boolean has(String id) {
        return strLocal != null && strLocal.containsKey(id) || STR_CORE.containsKey(id);
    }

    public String getPlural(long count, String id) {
        String one = id + ".one";
        String few = id + ".few";
        String other = id + ".other";

        if (!has(id)) {
            if (has(one)) id = one;
            else if (has(other)) id = other;
            else if (has(few)) id = few;
            else throw new NullPointerException("Cannot find any definition of this ID");
        }
        if (!has(one)) one = id;
        if (!has(other)) other = id;
        if (!has(few)) few = other;

        return getPlural(count, get(one), get(few), get(other));
    }

    public static String getPlural(long count, String one, String few, String other) {
        long c = (count % 100);
        if (c == 1 || (c > 20 && c % 10 == 1))
            return String.format(one, count);
        if ((c < 10 || c > 20) && c % 10 >= 2 && c % 10 <= 4)
            return String.format(few, count);
        return String.format(other, count);
    }

    public static String trimSnippet(String original, int count) {
        int length = original.length();
        if (length <= count) return original;
        if (count < 3) {
            log.warn("String trim length too short! ({} < 3)", count);
            count = 3;
        }
        return original.substring(0, count - 3) + "...";
    }

    private static class UTF8Control extends ResourceBundle.Control {
        public ResourceBundle newBundle
                (String baseName, Locale locale, String format, ClassLoader loader, boolean reload)
                throws IllegalAccessException, InstantiationException, IOException
        {
            // The below is a copy of the default implementation.
            String bundleName = toBundleName(baseName, locale);
            String resourceName = toResourceName(bundleName, "properties");
            ResourceBundle bundle = null;
            InputStream stream = null;
            if (reload) {
                URL url = loader.getResource(resourceName);
                if (url != null) {
                    URLConnection connection = url.openConnection();
                    if (connection != null) {
                        connection.setUseCaches(false);
                        stream = connection.getInputStream();
                    }
                }
            } else {
                stream = loader.getResourceAsStream(resourceName);
            }
            if (stream != null) {
                try {
                    // Only this line is changed to make it to read properties files as UTF-8.
                    bundle = new PropertyResourceBundle(new InputStreamReader(stream, StandardCharsets.UTF_8));
                } finally {
                    stream.close();
                }
            }
            return bundle;
        }
    }
}
