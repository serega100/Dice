package me.serega100.dice;

import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public enum Message {
    ENEMY_IS_IGNORING,
    MAIN_HAND_SHOULD_BE_EMPTY,
    NO_REQUESTS,
    YOU_HAVE_NO_MONEY,
    LOCATION_IS_NOT_AVAILABLE,
    ENEMY_LOCATION_IS_NOT_AVAILABLE,
    YOU_ARE_ALREADY_PLAYING,
    ON_REQUEST,
    REQUEST_TO_SEND,
    MSG_TO_ACCEPT,
    MSG_TO_REFUSE,
    YOUR_REQUEST_ACCEPTED,
    YOU_ACCEPT_REQUEST,
    GAME_HAS_STARTED,
    YOU_DISALLOW_REQUEST,
    ON_REQUESTING_PLAYER_QUIT,
    YOUR_REQUEST_DISALLOWED,
    YOU_HAVE_NUMBER,
    DEAD_HEAD,
    YOU_WIN,
    YOU_LOSE,
    IGNORING_ENABLE,
    IGNORING_DISABLE,
    ON_SERVER_SHUTDOWN;

    static {
        FileConfiguration msgConfig = DicePlugin.getInstance().getMessageConfig();

        // Load vars from config and color words
        Map<String, String> vars = new HashMap<>();

        for (ChatColor color : ChatColor.values()) {
            vars.put(color.name().toLowerCase(), color.toString());
        }

        ConfigurationSection variables = msgConfig.getConfigurationSection("variables");
        for (String key : variables.getKeys(false)) {
            vars.put(key, handleMessage(vars, variables.getString(key)));
        }


        // Replace all vars and color codes
        ConfigurationSection msgSection = msgConfig.getConfigurationSection("messages");
        String prefixText = msgSection.getString("prefix");
        String prefix = handleMessage(vars, prefixText);
        for (Message msg : values()) {
            String msgName = msg.name().toLowerCase();
            String msgText = msgSection.getString(msgName);
            msg.value = prefix + handleMessage(vars, msgText);
        }
    }

    private String value;

    @Override
    public String toString() {
        return value;
    }

    private static String handleMessage(Map<String, String> vars, String string) {
        Pattern colorKeyPattern = Pattern.compile("&([0-9a-fk-or])");
        Pattern varPatter = Pattern.compile("%([0-9A-z_]+)%");

        string = colorKeyPattern.matcher(string).replaceAll("§$1");

        Matcher matcher = varPatter.matcher(string);
        StringBuilder builder = new StringBuilder();
        int lastStart = 0;
        while (matcher.find()) {
            builder.append(string, lastStart, matcher.start());
            String varName = matcher.group(1);
            String replacement = vars.get(varName);
            if (replacement == null) {
                builder.append(matcher.group(0));
            } else {
                builder.append(replacement);
            }
            lastStart = matcher.end();
        }
        builder.append(string, lastStart, string.length());
        return builder.toString();
    }
}
