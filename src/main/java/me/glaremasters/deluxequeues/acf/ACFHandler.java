package me.glaremasters.deluxequeues.acf;

import co.aikar.commands.BungeeCommandManager;
import me.glaremasters.deluxequeues.DeluxeQueues;
import me.glaremasters.deluxequeues.commands.CommandHelp;
import me.glaremasters.deluxequeues.commands.CommandLeave;
import me.glaremasters.deluxequeues.queues.QueueHandler;

public class ACFHandler {

    private DeluxeQueues deluxeQueues;

    public ACFHandler(DeluxeQueues deluxeQueues, BungeeCommandManager commandManager) {
        this.deluxeQueues = deluxeQueues;
        commandManager.enableUnstableAPI("help");
        registerDependencyInjection(commandManager);
        registerCommandReplacements(commandManager);

        registerCommands(commandManager);
    }

    public void registerDependencyInjection(BungeeCommandManager commandManager) {
        commandManager.registerDependency(QueueHandler.class, deluxeQueues.getQueueHandler());
    }

    public void registerCommandReplacements(BungeeCommandManager commandManager) {
        commandManager.getCommandReplacements().addReplacement("dq", "queue|dq|queues");
    }

    public void registerCommands(BungeeCommandManager commandManager) {
        commandManager.registerCommand(new CommandHelp());
        commandManager.registerCommand(new CommandLeave());
    }


}
