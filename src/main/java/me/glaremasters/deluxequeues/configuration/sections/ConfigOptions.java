package me.glaremasters.deluxequeues.configuration.sections;

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

    @Comment("How many players need to be online for the queues to start?")
    public static Property<Integer> PLAYERS_REQUIRED =
            newProperty("settings.players-required", 25);

    @Comment("How many seconds should be inbetween each queue movement?")
    public static Property<Integer> DELAY_LENGTH =
            newProperty("settings.delay-length", 2);

    @Comment("List all the servers here that you would like to have a queue for.")
    public static Property<List<String>> QUEUE_SERVERS =
            newListProperty("settings.servers", "");

    @Comment({"How would you like to inform the player that they are in the queue?",
    "Currently supports: ACTIONBAR, TEXT, TITLE"})
    public static Property<String> INFORM_METHOD =
            newProperty("notify.method", "ACTIONBAR");

    @Comment("How would you like the design for the ActionBar to look?")
    public static Property<String> ACTIONBAR_DESIGN =
            newProperty("notify.actionbar.design", "Current Position: {pos} / {total}");

    @Comment("How would you like the design for the text to look?")
    public static Property<String> TEXT_DESIGN =
            newProperty("notify.text.design", "You are currently in position {pos} out of {total}");

    @Comment("How would you like the design for the title to look?")
    public static Property<String> TITLE_HEADER =
            newProperty("notify.title.title", "Current in queue");

    public static Property<String> TITLE_FOOTER =
            newProperty("notify.title.subtitle", "Position: {pos} / {total}");

    @Override
    public void registerComments(CommentsConfiguration configuration) {
        String[] pluginHeader = {
                "DeluxeQueues",
                "Creator: Glare",
                "Contributors: https://github.com/darbyjack/DeluxeQueues/graphs/contributors",
                "Issues: https://github.com/darbyjack/DeluxeQueues/issues",
                "Spigot: TBD",
                "Discord: https://helpch.at/discord"
        };
        configuration.setComment("settings", pluginHeader);
    }

}
