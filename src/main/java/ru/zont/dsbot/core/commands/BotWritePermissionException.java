package ru.zont.dsbot.core.commands;

import ru.zont.dsbot.core.util.DescribedException;
import ru.zont.dsbot.core.util.Strings;

public class BotWritePermissionException extends DescribedException {
    public BotWritePermissionException() {
        super(Strings.CORE.get("err.permission.title"), Strings.CORE.get("err.permission.bot"));
    }
}
