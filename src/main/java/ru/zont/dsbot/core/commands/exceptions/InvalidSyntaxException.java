package ru.zont.dsbot.core.commands.exceptions;

import ru.zont.dsbot.core.commands.CommandAdapter;
import ru.zont.dsbot.core.util.DescribedException;
import ru.zont.dsbot.core.util.Strings;

import javax.annotation.Nullable;

public class InvalidSyntaxException extends DescribedException {
    public static InvalidSyntaxException argument(int i, @Nullable String desc, CommandAdapter adapter) {
        String header = Strings.CORE.get("err.invalid_arg", i);
        return new InvalidSyntaxException(desc != null
                ? String.join("\n", header, desc) : header, adapter);
    }

    public static InvalidSyntaxException insufficientArgs(@Nullable String desc, CommandAdapter adapter) {
        String header = Strings.CORE.get("err.insufficient_args");
        return new InvalidSyntaxException(desc != null
                ? String.join("\n", header, desc) : header, adapter);
    }

    public InvalidSyntaxException(String description, CommandAdapter adapter) {
        super(Strings.CORE.get("err.args.title"), getDescription(description, adapter));
    }

    public InvalidSyntaxException(String message) {
        this(message, null);
    }

    private static String getDescription(String description, CommandAdapter adapter) {
        if (adapter != null) {
            final String syntax = Strings.CORE.get("err.args.syntax",
                    adapter.getSyntax(),
                    adapter.getConfig().getPrefix(),
                    adapter.getCallableName());
            if (description != null)
                return String.join("\n\n", description, syntax);
            else return syntax;
        }
        return description != null ? description : "*No additional information*";
    }
}
