package me.glaremasters.deluxequeues.messages;

import co.aikar.locales.MessageKey;
import co.aikar.locales.MessageKeyProvider;

public enum Messages implements MessageKeyProvider {
    COMMANDS__JOIN_DESCRIPTION,
    COMMANDS__LEAVE_DESCRIPTION,
    COMMANDS__INFO_DESCRIPTION,
    COMMANDS__KICK_DESCRIPTION,
    COMMANDS__RELOAD_DESCRIPTION,
    COMMANDS__HELP_DESCRIPTION,
    COMMANDS__KICK_SUCCESS,
    COMMANDS__JOIN_SUCCESS,
    COMMANDS__LEAVE_SUCCESS,
    COMMANDS__RELOAD_SUCCESS,
    COMMANDS__INFO_RESPONSE,
    ERRORS__SERVER_UNKNOWN,
    ERRORS__SERVER_NO_QUEUE,
    ERRORS__PLAYER_NO_QUEUE,
    ERRORS__PLAYER_SAME_SERVER,
    ERRORS__TARGET_NO_QUEUE,
    ERRORS__QUEUE_CANNOT_JOIN,
    ERRORS__QUEUE_REMOVED;


    /**
     * Message keys that grab from the config to send messages
     */
    private final MessageKey key = MessageKey.of(this.name().toLowerCase().replace("__", ".").replace("_", "-"));


    /**
     * Get the message get from the config
     * @return message key
     */
    public MessageKey getMessageKey() {
        return key;
    }
}
