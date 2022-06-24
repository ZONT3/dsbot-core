package ru.zont.dsbot.core.commands;

import java.util.LinkedList;
import java.util.List;

public class ArgumentTokenizer {
    private static final int STATE_NO_TOKEN = 0;
    private static final int STATE_NORMAL_TOKEN = 1;
    private static final int STATE_SINGLE_QUOTE = 2;
    private static final int STATE_DOUBLE_QUOTE = 3;

    public static List<String> tokenize(String arguments) {
        return tokenize(arguments, false);
    }

    public static List<String> tokenize(String arguments, boolean stringify) {
        LinkedList<String> argList = new LinkedList<>();
        StringBuilder currArg = new StringBuilder();
        boolean escaped = false;
        int state = STATE_NO_TOKEN;
        int len = arguments.length();
        for (int i = 0; i < len; i++) {
            char c = arguments.charAt(i);
            if (escaped) {
                escaped = false;
                currArg.append(c);
            } else {
                switch (state) {
                    case STATE_SINGLE_QUOTE:
                        if (c == '\'') {
                            state = STATE_NORMAL_TOKEN;
                        } else {
                            currArg.append(c);
                        }
                        break;
                    case STATE_DOUBLE_QUOTE:
                        if (c == '"') {
                            state = STATE_NORMAL_TOKEN;
                        } else if (c == '\\') {
                            i++;
                            char next = arguments.charAt(i);
                            if (next == '"' || next == '\\') {
                                currArg.append(next);
                            } else {
                                currArg.append(c);
                                currArg.append(next);
                            }
                        } else {
                            currArg.append(c);
                        }
                        break;
                    case STATE_NO_TOKEN:
                    case STATE_NORMAL_TOKEN:
                        switch (c) {
                            case '\\':
                                escaped = true;
                                state = STATE_NORMAL_TOKEN;
                                break;
                            case '\'':
                                state = STATE_SINGLE_QUOTE;
                                break;
                            case '"':
                                state = STATE_DOUBLE_QUOTE;
                                break;
                            default:
                                if (!Character.isWhitespace(c)) {
                                    currArg.append(c);
                                    state = STATE_NORMAL_TOKEN;
                                } else if (state == STATE_NORMAL_TOKEN) {
                                    // Whitespace ends the token; start a new one
                                    argList.add(currArg.toString());
                                    currArg = new StringBuilder();
                                    state = STATE_NO_TOKEN;
                                }
                        }
                        break;
                    default:
                        throw new IllegalStateException("ArgumentTokenizer state " + state + " is invalid!");
                }
            }
        }
        if (escaped) {
            currArg.append('\\');
            argList.add(currArg.toString());
        } else if (state != STATE_NO_TOKEN) {
            argList.add(currArg.toString());
        }
        if (stringify) {
            argList.replaceAll(s -> "\"" + escapeQuotesAndBackslashes(s) + "\"");
        }
        return argList;
    }

    private static String escapeQuotesAndBackslashes(String s) {
        final StringBuilder buf = new StringBuilder(s);

        for (int i = s.length() - 1; i >= 0; i--) {
            char c = s.charAt(i);
            if ((c == '\\') || (c == '"')) {
                buf.insert(i, '\\');
            } else if (c == '\n') {
                buf.deleteCharAt(i);
                buf.insert(i, "\\n");
            } else if (c == '\t') {
                buf.deleteCharAt(i);
                buf.insert(i, "\\t");
            } else if (c == '\r') {
                buf.deleteCharAt(i);
                buf.insert(i, "\\r");
            } else if (c == '\b') {
                buf.deleteCharAt(i);
                buf.insert(i, "\\b");
            } else if (c == '\f') {
                buf.deleteCharAt(i);
                buf.insert(i, "\\f");
            }
        }
        return buf.toString();
    }
}