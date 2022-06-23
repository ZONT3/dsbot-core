package ru.zont.dsbot.core.commands;

import ru.zont.dsbot.core.util.Strings;

public class NotImplementedException extends DescribedException {
    public NotImplementedException() {
        super(Strings.CORE.get("err.not_implemented"));
    }
}