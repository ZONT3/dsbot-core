package ru.zont.dsbot.core.commands.exceptions;

import ru.zont.dsbot.core.util.DescribedException;
import ru.zont.dsbot.core.util.Strings;

import static ru.zont.dsbot.core.util.Strings.CORE;

public class OnlySlashUsageException extends DescribedException {
    public OnlySlashUsageException() {
        super(CORE.get("err.only_slash.title"), CORE.get("err.only_slash"), 0x6D1DA5);
    }
}
