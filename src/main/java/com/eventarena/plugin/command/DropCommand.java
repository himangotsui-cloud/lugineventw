package com.eventarena.plugin.command;

import com.eventarena.plugin.EventArenaPlugin;
import com.eventarena.plugin.util.ColorUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class DropCommand implements CommandExecutor {

    private final EventArenaPlugin plugin;

    public DropCommand(EventArenaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("event.drop")) {
            sender.sendMessage(plugin.configManager().msg("no-permission"));
            return true;
        }
        if (plugin.worldManager().isDropInProgress()) {
            sender.sendMessage(ColorUtil.legacy("&eA drop is already in progress."));
            return true;
        }

        sender.sendMessage(plugin.configManager().msg("drop-started"));
        plugin.worldManager().dropArena(() ->
                plugin.getServer().broadcastMessage(plugin.configManager().msg("drop-done")));
        return true;
    }
}
