package com.eventarena.plugin.manager;

import com.eventarena.plugin.EventArenaPlugin;
import com.eventarena.plugin.state.EventMode;
import com.eventarena.plugin.util.ColorUtil;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * Reads kits.yml so admins can edit items/enchants/armor/lore without
 * touching code, and applies the right kit for the active EventMode.
 */
public class EventKitManager {

    private final EventArenaPlugin plugin;
    private FileConfiguration kits;

    public EventKitManager(EventArenaPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        File file = new File(plugin.getDataFolder(), "kits.yml");
        if (!file.exists()) {
            plugin.saveResource("kits.yml", false);
        }
        kits = YamlConfiguration.loadConfiguration(file);
    }

    public void giveKit(Player player, EventMode mode) {
        PlayerInventory inv = player.getInventory();
        inv.clear();
        inv.setHelmet(null);
        inv.setChestplate(null);
        inv.setLeggings(null);
        inv.setBoots(null);

        ConfigurationSection section = kits.getConfigurationSection(mode.name());
        if (section == null) {
            plugin.getLogger().warning("No kit configured for mode " + mode.name());
            return;
        }

        ConfigurationSection armor = section.getConfigurationSection("armor");
        if (armor != null) {
            inv.setHelmet(parseSimpleItem(armor.getString("helmet")));
            inv.setChestplate(parseSimpleItem(armor.getString("chestplate")));
            inv.setLeggings(parseSimpleItem(armor.getString("leggings")));
            inv.setBoots(parseSimpleItem(armor.getString("boots")));
        }

        List<Map<?, ?>> items = section.getMapList("items");
        for (Map<?, ?> raw : items) {
            try {
                int slot = ((Number) raw.get("slot")).intValue();
                String materialName = String.valueOf(raw.get("material"));
                Material material = Material.matchMaterial(materialName);
                if (material == null) {
                    plugin.getLogger().warning("Unknown material in kits.yml: " + materialName);
                    continue;
                }
                int amount = raw.containsKey("amount") ? ((Number) raw.get("amount")).intValue() : 1;
                ItemStack stack = new ItemStack(material, Math.max(1, amount));

                Object nameObj = raw.get("name");
                Object loreObj = raw.get("lore");
                if (nameObj != null || loreObj != null) {
                    ItemMeta meta = stack.getItemMeta();
                    if (nameObj != null && meta != null) {
                        meta.setDisplayName(ColorUtil.legacy(String.valueOf(nameObj)));
                    }
                    if (loreObj instanceof List<?> loreList && meta != null) {
                        meta.setLore(loreList.stream().map(o -> ColorUtil.legacy(String.valueOf(o))).toList());
                    }
                    stack.setItemMeta(meta);
                }

                Object enchantsObj = raw.get("enchants");
                if (enchantsObj instanceof Map<?, ?> enchantMap) {
                    for (Map.Entry<?, ?> entry : enchantMap.entrySet()) {
                        Enchantment enchant = Enchantment.getByName(String.valueOf(entry.getKey()));
                        if (enchant == null) continue;
                        int level = ((Number) entry.getValue()).intValue();
                        stack.addUnsafeEnchantment(enchant, level);
                    }
                }

                if (slot >= 0 && slot < 36) {
                    inv.setItem(slot, stack);
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to parse a kit item for " + mode.name() + ": " + e.getMessage());
            }
        }
    }

    private ItemStack parseSimpleItem(String materialName) {
        if (materialName == null || materialName.equalsIgnoreCase("AIR")) {
            return null;
        }
        Material material = Material.matchMaterial(materialName);
        if (material == null) return null;
        return new ItemStack(material);
    }
}
