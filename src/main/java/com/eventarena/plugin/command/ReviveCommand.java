package com.eventarena.plugin.command;

import com.eventarena.plugin.EventArenaPlugin;
import com.eventarena.plugin.util.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class ReviveCommand implements CommandExecutor, TabCompleter {

    private final EventArenaPlugin plugin;

    public ReviveCommand(EventArenaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("event.revive")) {
            sender.sendMessage(plugin.configManager().msg("no-permission"));
            return true;
        }
        if (args.length != 1) {
            sender.sendMessage(ColorUtil.legacy("&cUsage: /revive <all|player>"));
            return true;
        }

        if (args[0].equalsIgnoreCase("all")) {
            int count = plugin.eventManager().reviveAll();
            sender.sendMessage(plugin.configManager().msg("revive-all-done"));
            sender.sendMessage(ColorUtil.legacy("&7(" + count + " player(s) revived)"));
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null || !plugin.eventManager().isParticipant(target.getUniqueId())) {
            sender.sendMessage(plugin.configManager().msg("revive-not-found"));
            return true;
        }
        if (!plugin.eventManager().reviveOne(target)) {
            sender.sendMessage(plugin.configManager().msg("revive-not-eliminated"));
            return true;
        }
        sender.sendMessage(ColorUtil.legacy("&aRevived &f" + target.getName() + "&a."));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> options = new ArrayList<>();
            options.add("all");
            for (var uuid : plugin.eventManager().participantsView().keySet()) {
                Player p = Bukkit.getPlayer(uuid);
                if (p != null) options.add(p.getName());
            }
            options.removeIf(s -> !s.toLowerCase().startsWith(args[0].toLowerCase()));
            return options;
        }
        return new ArrayList<>();
    }
}
