package com.eventarena.plugin.command;

import com.eventarena.plugin.EventArenaPlugin;
import com.eventarena.plugin.gui.EventGui;
import com.eventarena.plugin.manager.EventManager;
import com.eventarena.plugin.util.ColorUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class EventCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBCOMMANDS = List.of(
            "make", "announce", "join", "leave", "start", "stop", "status"
    );

    private final EventArenaPlugin plugin;

    public EventCommand(EventArenaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            showStatus(sender);
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "make" -> handleMake(sender);
            case "announce" -> handleAnnounce(sender, args);
            case "join" -> handleJoin(sender);
            case "leave" -> handleLeave(sender);
            case "start" -> handleStart(sender);
            case "stop" -> handleStop(sender);
            case "status" -> showStatus(sender);
            default -> sender.sendMessage(ColorUtil.legacy("&cUnknown subcommand. /event [make|announce|join|leave|start|stop|status]"));
        }
        return true;
    }

    private void handleMake(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.configManager().msg("player-only"));
            return;
        }
        if (!player.hasPermission("event.make")) {
            player.sendMessage(plugin.configManager().msg("no-permission"));
            return;
        }
        if (plugin.eventManager().hasEvent()) {
            player.sendMessage(plugin.configManager().msg("event-already-exists"));
            return;
        }
        player.openInventory(EventGui.build());
    }

    private void handleAnnounce(CommandSender sender, String[] args) {
        if (!sender.hasPermission("event.announce")) {
            sender.sendMessage(plugin.configManager().msg("no-permission"));
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(ColorUtil.legacy("&cUsage: /event announce <message>"));
            return;
        }
        String message = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));
        String format = plugin.configManager().config().getString("announce.format", "&c[EVENT] %message%");
        plugin.getServer().broadcastMessage(ColorUtil.legacy(
                ColorUtil.placeholders(format, "%message%", message)));
    }

    private void handleJoin(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.configManager().msg("player-only"));
            return;
        }
        EventManager em = plugin.eventManager();
        EventManager.JoinResult result = em.join(player);
        switch (result) {
            case SUCCESS -> player.sendMessage(ColorUtil.placeholders(
                    plugin.configManager().msg("joined"),
                    "%current%", String.valueOf(em.totalCount()),
                    "%max%", String.valueOf(em.maxPlayers())));
            case NO_EVENT -> player.sendMessage(plugin.configManager().msg("event-not-created"));
            case ALREADY_JOINED -> player.sendMessage(plugin.configManager().msg("already-joined"));
            case FULL -> player.sendMessage(plugin.configManager().msg("event-full"));
            case ALREADY_STARTED -> player.sendMessage(plugin.configManager().msg("cannot-join-active"));
        }
    }

    private void handleLeave(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.configManager().msg("player-only"));
            return;
        }
        if (plugin.eventManager().leave(player)) {
            player.sendMessage(plugin.configManager().msg("left"));
        } else {
            player.sendMessage(plugin.configManager().msg("not-in-event"));
        }
    }

    private void handleStart(CommandSender sender) {
        if (!sender.hasPermission("event.make")) {
            sender.sendMessage(plugin.configManager().msg("no-permission"));
            return;
        }
        EventManager.StartResult result = plugin.eventManager().startEvent();
        switch (result) {
            case STARTING -> sender.sendMessage(ColorUtil.legacy("&aEvent starting..."));
            case NOT_ENOUGH_PLAYERS -> sender.sendMessage(ColorUtil.placeholders(
                    plugin.configManager().msg("not-enough-players"),
                    "%min%", String.valueOf(plugin.eventManager().minPlayers())));
            case WRONG_STATE -> sender.sendMessage(plugin.configManager().msg("event-not-created"));
        }
    }

    private void handleStop(CommandSender sender) {
        if (!sender.hasPermission("event.make")) {
            sender.sendMessage(plugin.configManager().msg("no-permission"));
            return;
        }
        if (!plugin.eventManager().hasEvent()) {
            sender.sendMessage(plugin.configManager().msg("event-not-created"));
            return;
        }
        plugin.eventManager().stopEvent("event-stopped");
        sender.sendMessage(ColorUtil.legacy("&cEvent stopped."));
    }

    private void showStatus(CommandSender sender) {
        EventManager em = plugin.eventManager();
        if (!em.hasEvent()) {
            sender.sendMessage(plugin.configManager().msg("event-not-created"));
            return;
        }
        sender.sendMessage(plugin.configManager().rawMsg("status-header"));
        sender.sendMessage(ColorUtil.placeholders(plugin.configManager().rawMsg("status-mode"),
                "%mode%", ColorUtil.legacy(em.mode().displayName())));
        sender.sendMessage(ColorUtil.placeholders(plugin.configManager().rawMsg("status-players"),
                "%current%", String.valueOf(em.activeCount()), "%max%", String.valueOf(em.maxPlayers())));
        sender.sendMessage(ColorUtil.placeholders(plugin.configManager().rawMsg("status-border"),
                "%size%", String.valueOf(plugin.borderManager().currentSize())));
        sender.sendMessage(ColorUtil.placeholders(plugin.configManager().rawMsg("status-state"),
                "%state%", em.state().name()));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return SUBCOMMANDS.stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
}
