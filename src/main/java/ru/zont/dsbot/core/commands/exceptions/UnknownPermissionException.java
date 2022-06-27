package ru.zont.dsbot.core.commands.exceptions;

import ru.zont.dsbot.core.util.DescribedException;
import ru.zont.dsbot.core.util.Strings;

public class UnknownPermissionException extends InsufficientPermissionsException {
    public UnknownPermissionException() {
        super(Strings.CORE.get("err.unknown_perm"));
    }
}
