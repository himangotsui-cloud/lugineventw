package com.eventarena.plugin.manager;

import com.eventarena.plugin.EventArenaPlugin;
import com.eventarena.plugin.util.ColorUtil;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class ConfigManager {

    private final EventArenaPlugin plugin;
    private FileConfiguration config;
    private FileConfiguration messages;
    private File messagesFile;

    // persisted runtime state (survives restarts): border size, world name in use
    private File stateFile;
    private FileConfiguration state;

    public ConfigManager(EventArenaPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        config = plugin.getConfig();

        messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        messages = YamlConfiguration.loadConfiguration(messagesFile);

        stateFile = new File(plugin.getDataFolder(), "state.yml");
        if (!stateFile.exists()) {
            try {
                stateFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create state.yml: " + e.getMessage());
            }
        }
        state = YamlConfiguration.loadConfiguration(stateFile);
    }

    public FileConfiguration config() {
        return config;
    }

    public String msg(String path) {
        String prefix = ColorUtil.legacy(messages.getString("prefix", ""));
        String raw = messages.getString(path, path);
        return prefix + ColorUtil.legacy(raw);
    }

    public String rawMsg(String path) {
        return ColorUtil.legacy(messages.getString(path, path));
    }

    // ---- persisted runtime state ----

    public int getSavedBorderSize() {
        return state.getInt("border-size", config.getInt("border.default-size", 100));
    }

    public void saveBorderSize(int size) {
        state.set("border-size", size);
        saveState();
    }

    public String getSavedMode() {
        return state.getString("active-mode", null);
    }

    public void saveMode(String modeName) {
        state.set("active-mode", modeName);
        saveState();
    }

    public void clearActiveEvent() {
        state.set("active-mode", null);
        saveState();
    }

    private void saveState() {
        try {
            state.save(stateFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save state.yml: " + e.getMessage());
        }
    }
}
