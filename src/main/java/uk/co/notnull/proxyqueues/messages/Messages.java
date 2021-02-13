package uk.co.notnull.proxyqueues.messages;

import co.aikar.locales.MessageKey;
import co.aikar.locales.MessageKeyProvider;

@SuppressWarnings("unused")
public enum Messages implements MessageKeyProvider {
    PREFIX__ERROR,
    PREFIX__INFO,
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
    COMMANDS__INFO_SERVER_RESPONSE,
    COMMANDS__INFO_PLAYER_RESPONSE,
    COMMANDS__INFO_STATUS_ONLINE,
    COMMANDS__INFO_STATUS_OFFLINE,
    RECONNECT__RESTORE_POSITION,
    RECONNECT__RESTORE_PRIORITY,
    ERRORS__SERVER_UNKNOWN,
    ERRORS__SERVER_NO_QUEUE,
    ERRORS__PLAYER_NO_QUEUE,
    ERRORS__PLAYER_SAME_SERVER,
    ERRORS__TARGET_UNKNOWN,
    ERRORS__TARGET_NO_QUEUE,
    ERRORS__QUEUE_CANNOT_JOIN,
    ERRORS__QUEUE_REMOVED,
    ERRORS__QUEUE_DESTROYED;


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
