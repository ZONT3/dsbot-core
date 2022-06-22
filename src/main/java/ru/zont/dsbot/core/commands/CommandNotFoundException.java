package ru.zont.dsbot.core.commands;

import ru.zont.dsbot.core.util.Strings;

public class CommandNotFoundException extends DescribedException {
    public CommandNotFoundException() {
        super(Strings.CORE.get("err.unknown_command"),
                Strings.CORE.get("err.unknown_command.desc", "ZONT_#9164, <@!331524458806247426>"));
    }
}
