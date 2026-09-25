package com.eventarena.plugin.gui;

import com.eventarena.plugin.state.EventMode;
import com.eventarena.plugin.util.ColorUtil;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.Bukkit;

import java.util.List;

public final class EventGui {

    public static final String TITLE = ColorUtil.legacy("&8&lSelect Event Mode");
    private static final int SIZE = 27;

    private EventGui() {
    }

    public static Inventory build() {
        Inventory inv = Bukkit.createInventory(null, SIZE, TITLE);

        ItemStack filler = named(Material.GRAY_STAINED_GLASS_PANE, " ", List.of());
        for (int i = 0; i < SIZE; i++) {
            inv.setItem(i, filler);
        }

        int[] slots = {11, 13, 15, 20};
        EventMode[] modes = EventMode.values();
        for (int i = 0; i < modes.length && i < slots.length; i++) {
            EventMode mode = modes[i];
            inv.setItem(slots[i], named(mode.icon(), mode.displayName(), mode.lore()));
        }

        // Recenter if we only used 4 of a 27-slot inventory oddly - simpler: also place on row 2 evenly.
        inv.setItem(10, named(Material.LIME_STAINED_GLASS_PANE, "&a", List.of()));
        inv.setItem(16, named(Material.LIME_STAINED_GLASS_PANE, "&a", List.of()));

        return inv;
    }

    private static ItemStack named(Material material, String name, List<String> lore) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ColorUtil.legacy(name));
            meta.setLore(lore.stream().map(ColorUtil::legacy).toList());
            stack.setItemMeta(meta);
        }
        return stack;
    }

    public static EventMode modeForSlot(int slot) {
        return switch (slot) {
            case 11 -> EventMode.CRYSTAL_PVP;
            case 13 -> EventMode.MACE_PVP;
            case 15 -> EventMode.SWORD_PVP;
            case 20 -> EventMode.AXE_PVP;
            default -> null;
        };
    }
}
