package me.glaremasters.deluxequeues.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.HelpCommand;
import me.glaremasters.deluxequeues.utils.Constants;

@CommandAlias("%dq")
public class CommandHelp extends BaseCommand {


    @HelpCommand
    @CommandPermission(Constants.BASE_PERM + "help")
    @Description("{@@descriptions.help}")
    public void execute(co.aikar.commands.CommandHelp help) {
        help.showHelp();
    }

}
