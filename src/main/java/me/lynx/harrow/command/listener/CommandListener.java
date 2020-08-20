package me.lynx.harrow.command.listener;

import me.lynx.harrow.HarrowLogger;
import me.lynx.harrow.command.AbstractCommandService;
import me.lynx.harrow.command.ParentCommand;
import me.lynx.harrow.command.template.IChildCommand;
import me.lynx.harrow.util.Utils;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.server.ServerCommandEvent;

import java.util.function.Supplier;
import java.util.stream.Stream;

public class CommandListener implements Listener {

    private final AbstractCommandService commandService;
    private String PREFIX;

    public CommandListener(AbstractCommandService commandService) {
        this.commandService = commandService;
        PREFIX = commandService.getPlugin().getName().toLowerCase() + ":";
        commandService.getPlugin()
            .getServer()
            .getPluginManager()
            .registerEvents(this, commandService.getPlugin());
    }
    
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
    private void aliasForCommandUsed(PlayerCommandPreprocessEvent e) {
        String noSlash = e.getMessage().substring(1);
        String[] split = noSlash.split(" ");
        String command = split[0];

        Supplier<Stream<ParentCommand>> stream = () -> commandService.getCommands().stream()
            .filter(ParentCommand::isRegistered)
            .filter(cmd -> Utils.containsIgnoreCase(command, cmd.getAliases()));
        if (stream.get().count() < 1) return;

        ParentCommand parentCommand = stream.get().findFirst().orElseGet(null);
        if (parentCommand == null) return;
        split[0] = parentCommand.getName();

        if (split.length > 1 && !split[1].equalsIgnoreCase("")) {
            String child = split[1];

            IChildCommand childCommand = parentCommand.getChildCommands().stream()
                    .filter(cmd -> Utils.containsIgnoreCase(child, cmd.getAliases()))
                    .findFirst().orElseGet(null);
            if (childCommand == null) return;
            split[1] = childCommand.getName();
        }

        e.setMessage(Utils.processCommand(split, true));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
    private void aliasForCommandUsed(ServerCommandEvent e) {
        String noSlash = e.getCommand();
        String[] split = noSlash.split(" ");
        String command = split[0];
        boolean foundPrefix = command.startsWith(PREFIX);

        Supplier<Stream<ParentCommand>> stream = () -> commandService.getCommands().stream()
                .filter(ParentCommand::isRegistered)
                .filter(cmd -> Utils.containsIgnoreCase(foundPrefix ? command.substring(PREFIX.length()) : command
                        , cmd.getAliases()));
        if (stream.get().count() < 1) return;

        ParentCommand parentCommand = stream.get().findFirst().orElseGet(null);
        if (parentCommand == null) return;

        split[0] = foundPrefix ? PREFIX + parentCommand.getName() : parentCommand.getName();

        if (split.length > 1 && !split[1].equalsIgnoreCase("")) {
            String child = split[1];

            IChildCommand childCommand = parentCommand.getChildCommands().stream()
                    .filter(cmd -> Utils.containsIgnoreCase(child, cmd.getAliases()))
                    .findFirst().orElseGet(null);
            if (childCommand == null) return;

            split[1] = childCommand.getName();
        }

        e.setCommand(Utils.processCommand(split, false));
        HarrowLogger.warn(Utils.processCommand(split, false), commandService.getPlugin().getName());
    }

}