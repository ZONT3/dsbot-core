package ru.zont.dsbot.core.commands.exceptions;

import ru.zont.dsbot.core.util.DescribedException;
import ru.zont.dsbot.core.util.Strings;

public class ForeignServerException extends InsufficientPermissionsException {
    public ForeignServerException() {
        super(Strings.CORE.get("err.foreign_guild"));
    }
}
