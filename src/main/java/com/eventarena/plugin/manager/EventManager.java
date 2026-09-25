package com.eventarena.plugin.manager;

import com.eventarena.plugin.EventArenaPlugin;
import com.eventarena.plugin.model.EventPlayerData;
import com.eventarena.plugin.state.EventMode;
import com.eventarena.plugin.state.EventState;
import com.eventarena.plugin.util.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.FireworkEffect;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.scheduler.BukkitTask;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * The single authority over event state. Every other class (commands,
 * listeners, GUI, scoreboard) reads from here rather than keeping its own
 * flags - this is what makes the plugin hard to desync.
 */
public class EventManager {

    private final EventArenaPlugin plugin;

    private volatile EventState state = EventState.IDLE;
    private EventMode mode;
    private final Map<UUID, EventPlayerData> participants = new LinkedHashMap<>();
    private BukkitTask countdownTask;
    private BukkitTask scoreboardTask;

    public EventManager(EventArenaPlugin plugin) {
        this.plugin = plugin;
    }

    public EventState state() {
        return state;
    }

    public EventMode mode() {
        return mode;
    }

    public int maxPlayers() {
        return plugin.configManager().config().getInt("event.max-players", 50);
    }

    public int minPlayers() {
        return plugin.configManager().config().getInt("event.min-players", 2);
    }

    public int activeCount() {
        int count = 0;
        for (EventPlayerData d : participants.values()) {
            if (!d.isEliminated()) count++;
        }
        return count;
    }

    public int totalCount() {
        return participants.size();
    }

    public boolean hasEvent() {
        return state != EventState.IDLE;
    }

    public boolean isParticipant(UUID uuid) {
        return participants.containsKey(uuid);
    }

    public EventPlayerData data(UUID uuid) {
        return participants.get(uuid);
    }

    // ------------------------------------------------------------------
    // Creation
    // ------------------------------------------------------------------

    public boolean createEvent(EventMode mode) {
        if (state != EventState.IDLE) return false;
        this.state = EventState.CREATING;
        this.mode = mode;
        participants.clear();

        plugin.worldManager().getOrCreateWorld();
        plugin.configManager().saveMode(mode.name());

        this.state = EventState.WAITING;
        startScoreboardTask();
        return true;
    }

    public void stopEvent(String reasonMessageKey) {
        if (state == EventState.IDLE) return;
        cancelCountdown();
        stopScoreboardTask();

        for (UUID uuid : participants.keySet()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                restorePlayer(p, participants.get(uuid));
                p.sendMessage(ColorUtil.legacy(plugin.configManager().msg(reasonMessageKey)));
            }
        }
        participants.clear();
        plugin.configManager().clearActiveEvent();
        this.state = EventState.IDLE;
        this.mode = null;
    }

    // ------------------------------------------------------------------
    // Join / leave
    // ------------------------------------------------------------------

    public JoinResult join(Player player) {
        if (state == EventState.IDLE) return JoinResult.NO_EVENT;
        if (state == EventState.ACTIVE || state == EventState.ENDING) return JoinResult.ALREADY_STARTED;
        if (participants.containsKey(player.getUniqueId())) return JoinResult.ALREADY_JOINED;
        if (totalCount() >= maxPlayers()) return JoinResult.FULL;

        EventPlayerData data = new EventPlayerData(player.getUniqueId());
        snapshotPlayer(player, data);
        participants.put(player.getUniqueId(), data);

        World world = plugin.worldManager().getOrCreateWorld();
        player.teleport(plugin.worldManager().arenaCenter());
        player.setGameMode(GameMode.ADVENTURE);
        player.getInventory().clear();

        return JoinResult.SUCCESS;
    }

    public boolean leave(Player player) {
        EventPlayerData data = participants.remove(player.getUniqueId());
        if (data == null) return false;
        restorePlayer(player, data);
        return true;
    }

    private void snapshotPlayer(Player player, EventPlayerData data) {
        data.setPreJoinLocation(player.getLocation());
        data.setPreJoinGameMode(player.getGameMode());
        data.setPreJoinInventory(player.getInventory().getContents());
        data.setPreJoinArmor(player.getInventory().getArmorContents());
        data.setPreJoinHealth(player.getHealth());
        data.setPreJoinFood(player.getFoodLevel());
    }

    private void restorePlayer(Player player, EventPlayerData data) {
        player.setGameMode(data.getPreJoinGameMode() != null ? data.getPreJoinGameMode() : GameMode.SURVIVAL);
        player.getInventory().clear();
        if (data.getPreJoinInventory() != null) player.getInventory().setContents(data.getPreJoinInventory());
        if (data.getPreJoinArmor() != null) player.getInventory().setArmorContents(data.getPreJoinArmor());
        double maxHealth = player.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH) != null
                ? player.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).getValue() : 20.0;
        player.setHealth(Math.min(data.getPreJoinHealth() > 0 ? data.getPreJoinHealth() : maxHealth, maxHealth));
        player.setFoodLevel(data.getPreJoinFood() > 0 ? data.getPreJoinFood() : 20);
        player.setFireTicks(0);
        player.setFallDistance(0);
        for (var effect : player.getActivePotionEffects()) {
            player.removePotionEffect(effect.getType());
        }
        if (data.getPreJoinLocation() != null) {
            player.teleport(data.getPreJoinLocation());
        }
    }

    // ------------------------------------------------------------------
    // Start / countdown
    // ------------------------------------------------------------------

    public StartResult startEvent() {
        if (state != EventState.WAITING) return StartResult.WRONG_STATE;
        if (totalCount() < minPlayers()) return StartResult.NOT_ENOUGH_PLAYERS;

        state = EventState.STARTING;
        int seconds = plugin.configManager().config().getInt("event.countdown-seconds", 15);

        broadcastToParticipants(ColorUtil.placeholders(
                plugin.configManager().msg("starting-countdown"), "%seconds%", String.valueOf(seconds)));

        countdownTask = new org.bukkit.scheduler.BukkitRunnable() {
            int remaining = seconds;

            @Override
            public void run() {
                if (state != EventState.STARTING) {
                    this.cancel();
                    return;
                }
                if (remaining <= 0) {
                    this.cancel();
                    reallyStart();
                    return;
                }
                remaining--;
            }
        }.runTaskTimer(plugin, 20L, 20L);

        return StartResult.STARTING;
    }

    private void reallyStart() {
        state = EventState.ACTIVE;
        Random random = new Random();
        int spreadRadius = plugin.configManager().config().getInt("event.spread-radius", 40);
        Location center = plugin.worldManager().arenaCenter();

        for (Map.Entry<UUID, EventPlayerData> entry : participants.entrySet()) {
            Player p = Bukkit.getPlayer(entry.getKey());
            if (p == null) continue;
            EventPlayerData data = entry.getValue();
            data.setEliminated(false);

            double angle = random.nextDouble() * Math.PI * 2;
            double dist = random.nextDouble() * spreadRadius;
            Location spawn = center.clone().add(Math.cos(angle) * dist, 0, Math.sin(angle) * dist);
            spawn.setY(findSafeY(spawn));

            p.teleport(spawn);
            p.setGameMode(GameMode.SURVIVAL);
            p.getInventory().clear();
            p.setHealth(20.0);
            p.setFoodLevel(20);
            p.setFireTicks(0);
            for (var effect : p.getActivePotionEffects()) {
                p.removePotionEffect(effect.getType());
            }
            plugin.kitManager().giveKit(p, mode);
        }

        broadcastToParticipants(plugin.configManager().msg("event-started"));
    }

    private double findSafeY(Location loc) {
        World world = loc.getWorld();
        return world.getMinHeight() + 2;
    }

    private void cancelCountdown() {
        if (countdownTask != null) {
            countdownTask.cancel();
            countdownTask = null;
        }
    }

    // ------------------------------------------------------------------
    // Death / elimination
    // ------------------------------------------------------------------

    public void eliminate(Player player) {
        EventPlayerData data = participants.get(player.getUniqueId());
        if (data == null || data.isEliminated()) return;
        data.setEliminated(true);

        player.setGameMode(GameMode.SPECTATOR);
        player.sendMessage(plugin.configManager().msg("eliminated"));

        broadcastToParticipants(ColorUtil.placeholders(
                plugin.configManager().msg("eliminated-broadcast"),
                "%player%", player.getName(),
                "%remaining%", String.valueOf(activeCount())
        ));

        checkWinCondition();
    }

    private void checkWinCondition() {
        if (state != EventState.ACTIVE) return;
        if (activeCount() > 1) return;

        state = EventState.ENDING;
        Player winner = null;
        for (Map.Entry<UUID, EventPlayerData> entry : participants.entrySet()) {
            if (!entry.getValue().isEliminated()) {
                winner = Bukkit.getPlayer(entry.getKey());
                break;
            }
        }

        if (winner != null) {
            announceWinner(winner);
        }

        // give a short beat before resetting so the winner announcement lands
        Bukkit.getScheduler().runTaskLater(plugin, this::finishEvent, 60L);
    }

    private void announceWinner(Player winner) {
        String msg = ColorUtil.placeholders(
                plugin.configManager().config().getString("victory.message", "&6%player% won!"),
                "%player%", winner.getName(),
                "%mode%", mode.displayName()
        );
        Bukkit.broadcastMessage(ColorUtil.legacy(msg));

        String soundName = plugin.configManager().config().getString("victory.sound", "ENTITY_ENDER_DRAGON_GROWL");
        try {
            Sound sound = Sound.valueOf(soundName);
            for (UUID uuid : participants.keySet()) {
                Player p = Bukkit.getPlayer(uuid);
                if (p != null) p.playSound(p.getLocation(), sound, 1f, 1f);
            }
        } catch (IllegalArgumentException ignored) {
        }

        if (plugin.configManager().config().getBoolean("victory.firework", true)) {
            spawnVictoryFirework(winner.getLocation());
        }
    }

    private void spawnVictoryFirework(Location loc) {
        Firework fw = (Firework) loc.getWorld().spawnEntity(loc, org.bukkit.entity.EntityType.FIREWORK_ROCKET);
        FireworkMeta meta = fw.getFireworkMeta();
        meta.addEffect(FireworkEffect.builder()
                .withColor(org.bukkit.Color.YELLOW, org.bukkit.Color.ORANGE)
                .with(FireworkEffect.Type.BURST)
                .trail(true)
                .build());
        meta.setPower(1);
        fw.setFireworkMeta(meta);
    }

    private void finishEvent() {
        state = EventState.FINISHED;
        for (Map.Entry<UUID, EventPlayerData> entry : participants.entrySet()) {
            Player p = Bukkit.getPlayer(entry.getKey());
            if (p != null) {
                restorePlayer(p, entry.getValue());
            }
        }
        participants.clear();
        plugin.configManager().clearActiveEvent();
        stopScoreboardTask();

        state = EventState.RESETTING;
        plugin.worldManager().dropArena(() -> state = EventState.IDLE);
    }

    // ------------------------------------------------------------------
    // Revive
    // ------------------------------------------------------------------

    public boolean reviveOne(Player target) {
        EventPlayerData data = participants.get(target.getUniqueId());
        if (data == null || !data.isEliminated()) return false;
        data.setEliminated(false);
        target.setGameMode(GameMode.ADVENTURE);
        target.setHealth(20.0);
        target.setFoodLevel(20);
        target.teleport(plugin.worldManager().arenaCenter());
        target.sendMessage(plugin.configManager().msg("revived"));
        // Deliberately NOT giving a kit here - spec requires manual re-arming only.
        return true;
    }

    public int reviveAll() {
        int count = 0;
        for (Map.Entry<UUID, EventPlayerData> entry : participants.entrySet()) {
            if (entry.getValue().isEliminated()) {
                Player p = Bukkit.getPlayer(entry.getKey());
                if (p != null && reviveOne(p)) count++;
            }
        }
        return count;
    }

    // ------------------------------------------------------------------
    // Disconnect handling
    // ------------------------------------------------------------------

    public void handleDisconnect(Player player) {
        EventPlayerData data = participants.get(player.getUniqueId());
        if (data == null) return;
        data.setDisconnected(true);
        // Treat a disconnect mid-fight as an elimination so the state machine
        // can't be exploited by rage-quitting to "pause" a loss, and so the
        // win check still fires correctly.
        if (state == EventState.ACTIVE && !data.isEliminated()) {
            data.setEliminated(true);
            broadcastToParticipants(ColorUtil.placeholders(
                    plugin.configManager().msg("eliminated-broadcast"),
                    "%player%", player.getName(),
                    "%remaining%", String.valueOf(activeCount())
            ));
            checkWinCondition();
        } else if (state == EventState.WAITING || state == EventState.STARTING) {
            participants.remove(player.getUniqueId());
        }
    }

    // ------------------------------------------------------------------
    // Utility
    // ------------------------------------------------------------------

    public void broadcastToParticipants(String message) {
        for (UUID uuid : participants.keySet()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) p.sendMessage(message);
        }
    }

    private void startScoreboardTask() {
        stopScoreboardTask();
        scoreboardTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (UUID uuid : participants.keySet()) {
                Player p = Bukkit.getPlayer(uuid);
                if (p != null) plugin.scoreboardManager().update(p);
            }
        }, 0L, 20L);
    }

    private void stopScoreboardTask() {
        if (scoreboardTask != null) {
            scoreboardTask.cancel();
            scoreboardTask = null;
        }
    }

    public Map<UUID, EventPlayerData> participantsView() {
        return participants;
    }

    public enum JoinResult {SUCCESS, NO_EVENT, ALREADY_JOINED, FULL, ALREADY_STARTED}

    public enum StartResult {STARTING, NOT_ENOUGH_PLAYERS, WRONG_STATE}
}
