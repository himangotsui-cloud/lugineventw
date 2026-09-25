package com.eventarena.plugin.command;

import com.eventarena.plugin.EventArenaPlugin;
import com.eventarena.plugin.util.ColorUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class BorderCommand implements CommandExecutor {

    private final EventArenaPlugin plugin;

    public BorderCommand(EventArenaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("event.border")) {
            sender.sendMessage(plugin.configManager().msg("no-permission"));
            return true;
        }
        if (args.length != 1) {
            sender.sendMessage(ColorUtil.legacy("&cUsage: /border <size>"));
            return true;
        }

        int size;
        try {
            size = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            sender.sendMessage(ColorUtil.legacy("&cSize must be a whole number."));
            return true;
        }

        if (!plugin.borderManager().isValid(size)) {
            sender.sendMessage(ColorUtil.placeholders(
                    plugin.configManager().msg("border-invalid"),
                    "%min%", String.valueOf(plugin.borderManager().minSize()),
                    "%max%", String.valueOf(plugin.borderManager().maxSize())));
            return true;
        }

        plugin.borderManager().setBorder(size);
        sender.sendMessage(ColorUtil.placeholders(
                plugin.configManager().msg("border-set"), "%size%", String.valueOf(size)));
        return true;
    }
}
