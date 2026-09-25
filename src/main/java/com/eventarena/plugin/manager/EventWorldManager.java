package com.eventarena.plugin.manager;

import com.eventarena.plugin.EventArenaPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.Consumer;

/**
 * Owns the event's dedicated flat world.
 *
 * NOTE on the "400x400 world / 100x100 border" requirement: Bukkit/Paper only
 * exposes a single WorldBorder per world - there is no supported API for a
 * second, larger, hard boundary. We treat 400 as the intended generation
 * footprint (documented here and in config.yml) and implement the *playable*
 * boundary players actually collide with via the normal WorldBorder, sized by
 * EventBorderManager / the /border command. This is the closest robust
 * solution without touching NMS.
 */
public class EventWorldManager {

    private final EventArenaPlugin plugin;
    private World eventWorld;
    private boolean dropInProgress = false;

    public EventWorldManager(EventArenaPlugin plugin) {
        this.plugin = plugin;
    }

    public World getOrCreateWorld() {
        String worldName = plugin.configManager().config().getString("world.name", "event_world");
        World existing = Bukkit.getWorld(worldName);
        if (existing != null) {
            eventWorld = existing;
            return existing;
        }

        WorldCreator creator = new WorldCreator(worldName);
        creator.type(WorldType.FLAT);
        creator.generateStructures(false);
        // Vanilla superflat preset: a single bedrock layer, nothing else.
        // This is standard, stable, documented Paper/Bukkit API (WorldCreator#generatorSettings)
        // and works across modern Paper versions without any NMS.
        creator.generatorSettings("{\"layers\":[{\"block\":\"minecraft:bedrock\",\"height\":1}],\"biome\":\"minecraft:plains\"}");

        eventWorld = creator.createWorld();
        if (eventWorld != null) {
            eventWorld.setSpawnLocation(0, eventWorld.getMinHeight() + 1, 0);
            eventWorld.setDifficulty(org.bukkit.Difficulty.PEACEFUL);
            eventWorld.setPVP(true);
            eventWorld.setAutoSave(false);
            eventWorld.setGameRule(org.bukkit.GameRule.DO_MOB_SPAWNING, false);
            eventWorld.setGameRule(org.bukkit.GameRule.DO_WEATHER_CYCLE, false);
            eventWorld.setGameRule(org.bukkit.GameRule.ANNOUNCE_ADVANCEMENTS, false);
            eventWorld.setTime(6000);

            int initialBorder = plugin.configManager().getSavedBorderSize();
            eventWorld.getWorldBorder().setCenter(0, 0);
            eventWorld.getWorldBorder().setSize(initialBorder);
        }
        return eventWorld;
    }

    public World getEventWorld() {
        return eventWorld;
    }

    public Location arenaCenter() {
        World w = getOrCreateWorld();
        return new Location(w, 0.5, w.getMinHeight() + 2, 0.5);
    }

    public boolean isDropInProgress() {
        return dropInProgress;
    }

    /**
     * Clears everything above the bedrock floor within the current border
     * (plus a small margin), in batches spread across ticks so it never
     * freezes the server (no giant synchronous loop).
     */
    public void dropArena(Runnable onComplete) {
        if (dropInProgress) return;
        World world = getOrCreateWorld();
        if (world == null) return;

        dropInProgress = true;
        int border = (int) world.getWorldBorder().getSize();
        int radius = Math.max(border / 2, plugin.configManager().config().getInt("border.default-size", 100) / 2) + 16;
        int minY = world.getMinHeight();
        int dropDepth = plugin.configManager().config().getInt("world.drop-depth", 100);
        int maxY = Math.min(world.getMaxHeight() - 1, minY + 1 + dropDepth);
        int chunksPerTick = Math.max(1, plugin.configManager().config().getInt("world.chunks-per-tick", 4));

        int minChunkX = -(radius >> 4) - 1;
        int maxChunkX = (radius >> 4) + 1;
        int minChunkZ = minChunkX;
        int maxChunkZ = maxChunkX;

        Deque<int[]> queue = new ArrayDeque<>();
        for (int cx = minChunkX; cx <= maxChunkX; cx++) {
            for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
                queue.add(new int[]{cx, cz});
            }
        }

        final int fMinY = minY;
        final int fMaxY = maxY;

        new BukkitRunnable() {
            @Override
            public void run() {
                if (queue.isEmpty()) {
                    dropInProgress = false;
                    this.cancel();
                    if (onComplete != null) onComplete.run();
                    return;
                }
                for (int i = 0; i < chunksPerTick && !queue.isEmpty(); i++) {
                    int[] pos = queue.poll();
                    Chunk chunk = world.getChunkAt(pos[0], pos[1]);
                    if (!chunk.isLoaded()) chunk.load(true);
                    // start one above the bedrock floor, leave the floor itself intact
                    for (int y = fMinY + 1; y <= fMaxY; y++) {
                        for (int x = 0; x < 16; x++) {
                            for (int z = 0; z < 16; z++) {
                                if (world.getBlockAt(pos[0] * 16 + x, y, pos[1] * 16 + z).getType() != Material.AIR) {
                                    world.getBlockAt(pos[0] * 16 + x, y, pos[1] * 16 + z).setType(Material.AIR, false);
                                }
                            }
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    public void forEachOnlinePlayerInWorld(Consumer<org.bukkit.entity.Player> consumer) {
        if (eventWorld == null) return;
        for (org.bukkit.entity.Player p : eventWorld.getPlayers()) {
            consumer.accept(p);
        }
    }
}
