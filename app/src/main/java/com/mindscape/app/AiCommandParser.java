package com.mindscape.app;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class AiCommandParser {
    private static final Pattern COMMAND_PATTERN = Pattern.compile(
            "<\\s*(CREATE_CENTER|MOVE_CENTER|CREATE_CATEGORY|CREATE_NOTE|CREATE_CONNECTION|DELETE_NODE|HIDE_NODE|SHOW_NODE|DELETE_FILE_LINK|MOVE_FILE_LINK|COPY_FILE_LINK)[\\s:]*(.*?)\\s*(?:>|$)",
            Pattern.DOTALL
    );

    public static final class Command {
        public final String type;
        public final String[] args;

        public Command(String type, String[] args) {
            this.type = type;
            this.args = args;
        }

        public String firstArg() {
            return args.length > 0 ? args[0] : "";
        }
    }

    private AiCommandParser() {
    }

    public static String stripCommands(String text) {
        if (text == null) return "";
        return COMMAND_PATTERN.matcher(text).replaceAll("").trim();
    }

    public static List<Command> parse(String text) {
        List<Command> commands = new ArrayList<>();
        if (text == null) return commands;

        Matcher matcher = COMMAND_PATTERN.matcher(text);
        while (matcher.find()) {
            String[] args = matcher.group(2).split("\\|");
            for (int i = 0; i < args.length; i++) {
                args[i] = args[i].trim();
            }
            commands.add(new Command(matcher.group(1), args));
        }
        return commands;
    }
}
