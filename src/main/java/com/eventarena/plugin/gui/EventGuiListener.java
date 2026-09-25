package com.eventarena.plugin.gui;

import com.eventarena.plugin.EventArenaPlugin;
import com.eventarena.plugin.state.EventMode;
import com.eventarena.plugin.util.ColorUtil;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

public class EventGuiListener implements Listener {

    private final EventArenaPlugin plugin;

    public EventGuiListener(EventArenaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!EventGui.TITLE.equals(event.getView().getTitle())) {
            return;
        }

        // Block every form of item movement: normal pickup/place, shift-click,
        // double-click collect, hotbar swap, and clicks in the player's own
        // inventory while this GUI is open.
        event.setCancelled(true);

        if (event.getClickedInventory() == null) return;
        if (!event.getClickedInventory().equals(event.getView().getTopInventory())) return;

        int slot = event.getRawSlot();
        EventMode mode = EventGui.modeForSlot(slot);
        if (mode == null) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;

        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
        player.closeInventory();

        if (plugin.eventManager().hasEvent()) {
            player.sendMessage(plugin.configManager().msg("event-already-exists"));
            return;
        }

        plugin.eventManager().createEvent(mode);
        player.sendMessage(ColorUtil.placeholders(
                plugin.configManager().msg("event-created"), "%mode%", ColorUtil.legacy(mode.displayName())));
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getView().getTitle().equals(EventGui.TITLE)) {
            event.setCancelled(true);
        }
    }
}
