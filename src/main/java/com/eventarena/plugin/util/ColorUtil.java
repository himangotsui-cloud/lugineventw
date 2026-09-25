package com.eventarena.plugin.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.ChatColor;

/**
 * Central place for turning config strings into something displayable.
 * Supports classic '&' colour codes (what most server owners paste into
 * config files) and MiniMessage tags (Paper ships Adventure/MiniMessage
 * natively, so this works out of the box on any supported Paper version).
 */
public final class ColorUtil {

    private static final MiniMessage MINI = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY =
            LegacyComponentSerializer.legacyAmpersand();

    private ColorUtil() {
    }

    /** Legacy '&'-coded string -> coloured string, for places that still need a String (scoreboards, item names). */
    public static String legacy(String input) {
        if (input == null) return "";
        return ChatColor.translateAlternateColorCodes('&', input);
    }

    /** Legacy or MiniMessage string -> Adventure Component, for chat/titles. */
    public static Component component(String input) {
        if (input == null) return Component.empty();
        if (input.contains("<") && input.contains(">")) {
            try {
                return MINI.deserialize(input);
            } catch (Exception ignored) {
                // fall through to legacy parsing below
            }
        }
        return LEGACY.deserialize(legacy(input));
    }

    public static String placeholders(String input, Object... kv) {
        String result = input;
        for (int i = 0; i + 1 < kv.length; i += 2) {
            result = result.replace(String.valueOf(kv[i]), String.valueOf(kv[i + 1]));
        }
        return result;
    }
}
