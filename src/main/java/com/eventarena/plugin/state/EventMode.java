package com.eventarena.plugin.state;

import org.bukkit.Material;

import java.util.List;

public enum EventMode {
    CRYSTAL_PVP(
            "&d&lCrystal PvP",
            Material.END_CRYSTAL,
            List.of("&7Crystal combat", "&7Competitive PvP", "&7Event arena", "", "&eClick to select")
    ),
    MACE_PVP(
            "&6&lMace PvP",
            Material.MACE,
            List.of("&7Mace combat", "&7High-impact PvP", "&7Event arena", "", "&eClick to select")
    ),
    SWORD_PVP(
            "&b&lSword PvP",
            Material.DIAMOND_SWORD,
            List.of("&7Sword & netherite combat", "&7Classic PvP", "&7Event arena", "", "&eClick to select")
    ),
    AXE_PVP(
            "&c&lAxe PvP",
            Material.DIAMOND_AXE,
            List.of("&7Axe combat", "&7Heavy-hitting PvP", "&7Event arena", "", "&eClick to select")
    );

    private final String displayName;
    private final Material icon;
    private final List<String> lore;

    EventMode(String displayName, Material icon, List<String> lore) {
        this.displayName = displayName;
        this.icon = icon;
        this.lore = lore;
    }

    public String displayName() {
        return displayName;
    }

    public Material icon() {
        return icon;
    }

    public List<String> lore() {
        return lore;
    }
}
