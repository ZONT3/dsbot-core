package ru.zont.dsbot.core.commands.exceptions;

import ru.zont.dsbot.core.util.DescribedException;
import ru.zont.dsbot.core.util.Strings;

public class InsufficientPermissionsException extends DescribedException {

    public InsufficientPermissionsException() {
        this(null);
    }

    public InsufficientPermissionsException(String description) {
        super(Strings.CORE.get("err.permission"), description);
        setColor(0xBC5013);
        setPicture("https://imgur.com/xqruckI.png");
    }
}
