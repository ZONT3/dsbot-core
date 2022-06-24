package ru.zont.dsbot.core.commands;

import ru.zont.dsbot.core.util.DescribedException;
import ru.zont.dsbot.core.util.Strings;

public class InvalidSyntaxException extends DescribedException {
    public static InvalidSyntaxException argument(int i, String desc, CommandAdapter adapter) {
        String header = Strings.CORE.get("err.invalid_arg", i);
        return new InvalidSyntaxException(desc != null
                ? String.join("\n", header, desc) : header, adapter);
    }

    public static InvalidSyntaxException insufficientArgs(String desc, CommandAdapter adapter) {
        String header = Strings.CORE.get("err.insufficient_args");
        return new InvalidSyntaxException(desc != null
                ? String.join("\n", header, desc) : header, adapter);
    }

    public InvalidSyntaxException(String description, CommandAdapter adapter) {
        super(Strings.CORE.get("err.args.title"), description != null
                ? String.join("\n\n", description, adapter.getSyntax())
                : Strings.CORE.get("err.args.syntax", adapter.getSyntax(), adapter.getConfig().getPrefix(), adapter.getCallableName()));
    }
}
