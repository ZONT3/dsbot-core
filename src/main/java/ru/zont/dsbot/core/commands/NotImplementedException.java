package ru.zont.dsbot.core.commands;

import ru.zont.dsbot.core.util.DescribedException;
import ru.zont.dsbot.core.util.Strings;

public class NotImplementedException extends DescribedException {

    public static final int COLOR = 0x5F1691;

    public NotImplementedException() {
        super(Strings.CORE.get("err.not_implemented"), null, COLOR);
    }
    public NotImplementedException(String exact) {
        super(Strings.CORE.get("err.not_implemented.exact", exact), null, COLOR);
    }
}
