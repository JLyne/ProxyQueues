package uk.co.notnull.proxyqueues;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import ninja.leaping.configurate.ConfigurationNode;

import java.util.Collections;
import java.util.Map;

public class Messages {
    private static ConfigurationNode messages;
    private static final LegacyComponentSerializer legacyComponentSerializer = LegacyComponentSerializer.legacyAmpersand();

    public static void set(ConfigurationNode messages) {
        Messages.messages = messages;
    }

    public static String get(String id) {
        return get(id, Collections.emptyMap());
    }

    public static String get(String id, Map<String, String> replacements) {
        if(messages == null) {
            return "";
        }

        String message = messages.getNode((Object[]) id.split("\\."))
                .getString("Message " + id + " does not exist");

        for (Map.Entry<String, String> entry : replacements.entrySet()) {
            message = message.replace(entry.getKey(), entry.getValue());
        }

        return message;
    }

    public static Component getComponent(String id) {
        return getComponent(id, Collections.emptyMap());
    }

    public static Component getComponent(String id, Map<String, String> replacements) {
        if(messages == null) {
            return Component.empty();
        }

        String message = messages.getNode((Object[]) id.split("\\."))
                .getString("Message " + id + " does not exist");

        for (Map.Entry<String, String> entry : replacements.entrySet()) {
            message = message.replace(entry.getKey(), entry.getValue());
        }

        return legacyComponentSerializer.deserialize(message);
    }
}
