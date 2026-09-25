package com.eventarena.plugin.manager;

import com.eventarena.plugin.EventArenaPlugin;
import org.bukkit.World;

public class EventBorderManager {

    private final EventArenaPlugin plugin;

    public EventBorderManager(EventArenaPlugin plugin) {
        this.plugin = plugin;
    }

    public int minSize() {
        return plugin.configManager().config().getInt("border.min-size", 8);
    }

    public int maxSize() {
        return plugin.configManager().config().getInt("border.max-size", 400);
    }

    public boolean isValid(int size) {
        return size >= minSize() && size <= maxSize();
    }

    /** Applies + persists the border. Caller must validate with isValid() first. */
    public void setBorder(int size) {
        World world = plugin.worldManager().getOrCreateWorld();
        if (world == null) return;
        world.getWorldBorder().setCenter(0, 0);
        world.getWorldBorder().setSize(size);
        plugin.configManager().saveBorderSize(size);
    }

    public int currentSize() {
        World world = plugin.worldManager().getEventWorld();
        if (world == null) return plugin.configManager().getSavedBorderSize();
        return (int) world.getWorldBorder().getSize();
    }
}
