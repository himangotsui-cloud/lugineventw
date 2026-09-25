package com.eventarena.plugin.listener;

import com.eventarena.plugin.EventArenaPlugin;
import com.eventarena.plugin.manager.EventManager;
import com.eventarena.plugin.model.EventPlayerData;
import com.eventarena.plugin.state.EventState;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

/**
 * All the "players must not be able to exploit X" rules from the spec live
 * here in one place, keyed off EventManager's state - not local booleans.
 */
public class EventListener implements Listener {

    private final EventArenaPlugin plugin;

    public EventListener(EventArenaPlugin plugin) {
        this.plugin = plugin;
    }

    private EventManager em() {
        return plugin.eventManager();
    }

    private boolean inEvent(Player player) {
        return em().isParticipant(player.getUniqueId());
    }

    // --- Death -> elimination + spectator, never a real death ---

    @EventHandler(priority = EventPriority.HIGH)
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        if (!inEvent(player)) return;
        if (em().state() != EventState.ACTIVE) return;

        // Prevent normal death side-effects (item drop / xp drop / death screen loop)
        event.getDrops().clear();
        event.setDroppedExp(0);
        event.setDeathMessage(null);

        // Schedule elimination for the next tick, after the vanilla death has
        // been fully processed, then force an immediate respawn into spectator.
        plugin.getServer().getScheduler().runTask(plugin, () -> em().eliminate(player));
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        if (!inEvent(player)) return;
        EventPlayerData data = em().data(player.getUniqueId());
        if (data != null && data.isEliminated()) {
            event.setRespawnLocation(plugin.worldManager().arenaCenter());
            plugin.getServer().getScheduler().runTask(plugin, () -> player.setGameMode(GameMode.SPECTATOR));
        }
    }

    // --- Damage rules: no damage before ACTIVE, no damage for eliminated players ---

    @EventHandler(priority = EventPriority.HIGH)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!inEvent(player)) return;

        if (em().state() != EventState.ACTIVE) {
            event.setCancelled(true);
            return;
        }
        EventPlayerData data = em().data(player.getUniqueId());
        if (data != null && data.isEliminated()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDamageByEntity(EntityDamageByEntityEvent event) {
        // Eliminated/spectating players can never deal damage, even indirectly.
        if (event.getDamager() instanceof Player damager && inEvent(damager)) {
            EventPlayerData data = em().data(damager.getUniqueId());
            if (data != null && data.isEliminated()) {
                event.setCancelled(true);
            }
        }
    }

    // --- Block protection inside the event world ---

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (!isEventWorld(event.getPlayer())) return;
        boolean allow = plugin.configManager().config().getBoolean("event.allow-block-break", false);
        if (!allow || em().state() == EventState.ENDING || em().state() == EventState.FINISHED) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!isEventWorld(event.getPlayer())) return;
        boolean allow = plugin.configManager().config().getBoolean("event.allow-block-place", false);
        if (!allow || em().state() == EventState.ENDING || em().state() == EventState.FINISHED) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        if (!inEvent(player)) return;
        EventPlayerData data = em().data(player.getUniqueId());
        if (data != null && data.isEliminated()) {
            event.setCancelled(true);
        }
    }

    // --- Keep players inside the arena border (extra safety on top of WorldBorder) ---

    @EventHandler
    public void onTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        if (!inEvent(player)) return;
        if (em().state() != EventState.ACTIVE) return;
        if (event.getCause() == PlayerTeleportEvent.TeleportCause.PLUGIN) return;
        var world = plugin.worldManager().getEventWorld();
        if (world == null) return;
        if (!world.getWorldBorder().isInside(event.getTo())) {
            event.setCancelled(true);
        }
    }

    // --- Disconnects can't be used to dodge elimination or reset state ---

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (!inEvent(player)) return;
        em().handleDisconnect(player);
        plugin.scoreboardManager().clear(player);
    }

    private boolean isEventWorld(Player player) {
        var world = plugin.worldManager().getEventWorld();
        return world != null && player.getWorld().equals(world);
    }
}
