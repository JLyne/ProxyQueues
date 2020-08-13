package uk.co.notnull.proxyqueues.configuration.sections;

import ch.jalu.configme.Comment;
import ch.jalu.configme.SettingsHolder;
import ch.jalu.configme.configurationdata.CommentsConfiguration;
import ch.jalu.configme.properties.Property;

import java.util.List;

import static ch.jalu.configme.properties.PropertyInitializer.newListProperty;
import static ch.jalu.configme.properties.PropertyInitializer.newProperty;

/**
 * Created by Glare
 * Date: 7/13/2019
 * Time: 10:37 PM
 */
public class ConfigOptions implements SettingsHolder {
    @Comment("How many seconds should be in-between each queue movement?")
    public static final Property<Integer> DELAY_LENGTH =
            newProperty("settings.delay-length", 2);

    @Comment("How many seconds after a player disconnects should they be removed from the queue?")
    public static final Property<Integer> DISCONNECT_TIMEOUT =
            newProperty("settings.disconnect-timeout", 180);

    @Comment({"List all the servers here that you would like to have a queue for.",
            "For each server, you need to specify:",
            "- The number of players to enabled queueing at",
            "- The maximum number of players to allow into the server from the normal queue",
            "- The maximum number of players to allow into the server from the normal and priority queues",
            "- The maximum number of players to allow into the server from all queues",
            "Syntax: server name; start players; max normal players ; max normal + priority players ; max all queues",
            "Example: hub;50;200;220;225"})
    public static final Property<List<String>> QUEUE_SERVERS =
            newListProperty("settings.servers", "");

    @Comment({"Players joining queue servers directly will be redirect to this server, or kicked if it isn't available"})
    public static final Property<String> WAITING_SERVER =
            newProperty("settings.waiting-server", "");

    @Comment({"What would you like the priority permission node to be?"})
    public static final Property<String> PRIORITY_PERMISSION =
            newProperty("settings.priority-permission", "proxyqueues.priority");

    @Comment({"What would you like the staff permission node to be?"})
    public static final Property<String> STAFF_PERMISSION =
            newProperty("settings.staff-permission", "proxyqueues.staff");

    @Comment({"List of kick reasons that should be considered fatal.",
            "If a player gets kicked from a queued server with one of these reasons, they will be removed from the queue without retrying.",
            "Accepts partial reasons"})
    public static final Property<List<String>> FATAL_ERRORS =
            newListProperty("settings.fatal-errors", "");

    @Comment({"How would you like to inform the player that they are in the queue?",
            "Currently supports: BOSSBAR, ACTIONBAR, TEXT, TITLE"})
    public static final Property<String> INFORM_METHOD =
            newProperty("notify.method", "ACTIONBAR");

    @Comment("How would you like the design for the ActionBar to look for the normal queue?")
    public static final Property<String> NORMAL_ACTIONBAR_DESIGN =
            newProperty("notify.normal.actionbar.design", "Queued for {server}: {pos}");

    @Comment("How would you like the design for the BossBar to look for the normal queue?")
    public static final Property<String> NORMAL_BOSSBAR_DESIGN =
            newProperty("notify.normal.bossbar.design", "Queued for {server}: {pos}");

    @Comment("How would you like the design for the text to look for the normal queue?")
    public static final Property<String> NORMAL_TEXT_DESIGN =
            newProperty("notify.normal.text.design", "You are currently in position {pos}");

    @Comment("How would you like the design for the title to look for the normal queue?")
    public static final Property<String> NORMAL_TITLE_HEADER =
            newProperty("notify.normal.title.title", "Current in queue");

    public static final Property<String> NORMAL_TITLE_FOOTER =
            newProperty("notify.normal.title.subtitle", "Position: {pos}");

    @Comment("How would you like the design for the ActionBar to look for the priority queue?")
    public static final Property<String> PRIORITY_ACTIONBAR_DESIGN =
            newProperty("notify.priority.actionbar.design", "Queued for {server}: {pos}");

    @Comment("How would you like the design for the BossBar to look for the priority queue?")
    public static final Property<String> PRIORITY_BOSSBAR_DESIGN =
            newProperty("notify.priority.bossbar.design", "Queued for {server}: {pos}");

    @Comment("How would you like the design for the text to look for the priority queue?")
    public static final Property<String> PRIORITY_TEXT_DESIGN =
            newProperty("notify.priority.text.design", "You are currently in position {pos}");

    @Comment("How would you like the design for the title to look for the priority queue?")
    public static final Property<String> PRIORITY_TITLE_HEADER =
            newProperty("notify.priority.title.title", "Current in queue");

    public static final Property<String> PRIORITY_TITLE_FOOTER =
            newProperty("notify.priority.title.subtitle", "Position: {pos}");

        @Comment("How would you like the design for the ActionBar to look for the staff queue?")
    public static final Property<String> STAFF_ACTIONBAR_DESIGN =
            newProperty("notify.staff.actionbar.design", "Queued for {server}: {pos}");

    @Comment("How would you like the design for the BossBar to look for the staff queue?")
    public static final Property<String> STAFF_BOSSBAR_DESIGN =
            newProperty("notify.staff.bossbar.design", "Queued for {server}: {pos}");

    @Comment("How would you like the design for the text to look for the staff queue?")
    public static final Property<String> STAFF_TEXT_DESIGN =
            newProperty("notify.staff.text.design", "You are currently in position {pos}");

    @Comment("How would you like the design for the title to look for the staff queue?")
    public static final Property<String> STAFF_TITLE_HEADER =
            newProperty("notify.staff.title.title", "Current in queue");

    public static final Property<String> STAFF_TITLE_FOOTER =
            newProperty("notify.staff.title.subtitle", "Position: {pos}");

    @Override
    public void registerComments(CommentsConfiguration configuration) {
        String[] pluginHeader = {
                "ProxyQueues"
        };
        configuration.setComment("settings", pluginHeader);
    }

}
