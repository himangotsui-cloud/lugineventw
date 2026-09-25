package com.eventarena.plugin.model;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

/**
 * Snapshot of a player's state taken the moment they join the event, so we
 * can fully and safely restore them afterwards (this is what prevents the
 * "players stuck in spectator" / "lost inventory" class of bugs).
 */
public class EventPlayerData {

    private final UUID uuid;
    private boolean eliminated = false;
    private boolean disconnected = false;

    // pre-join snapshot, restored on leave/revive-out/event-end
    private Location preJoinLocation;
    private GameMode preJoinGameMode;
    private ItemStack[] preJoinInventory;
    private ItemStack[] preJoinArmor;
    private double preJoinHealth;
    private int preJoinFood;

    public EventPlayerData(UUID uuid) {
        this.uuid = uuid;
    }

    public UUID uuid() {
        return uuid;
    }

    public boolean isEliminated() {
        return eliminated;
    }

    public void setEliminated(boolean eliminated) {
        this.eliminated = eliminated;
    }

    public boolean isDisconnected() {
        return disconnected;
    }

    public void setDisconnected(boolean disconnected) {
        this.disconnected = disconnected;
    }

    public Location getPreJoinLocation() {
        return preJoinLocation;
    }

    public void setPreJoinLocation(Location preJoinLocation) {
        this.preJoinLocation = preJoinLocation;
    }

    public GameMode getPreJoinGameMode() {
        return preJoinGameMode;
    }

    public void setPreJoinGameMode(GameMode preJoinGameMode) {
        this.preJoinGameMode = preJoinGameMode;
    }

    public ItemStack[] getPreJoinInventory() {
        return preJoinInventory;
    }

    public void setPreJoinInventory(ItemStack[] preJoinInventory) {
        this.preJoinInventory = preJoinInventory;
    }

    public ItemStack[] getPreJoinArmor() {
        return preJoinArmor;
    }

    public void setPreJoinArmor(ItemStack[] preJoinArmor) {
        this.preJoinArmor = preJoinArmor;
    }

    public double getPreJoinHealth() {
        return preJoinHealth;
    }

    public void setPreJoinHealth(double preJoinHealth) {
        this.preJoinHealth = preJoinHealth;
    }

    public int getPreJoinFood() {
        return preJoinFood;
    }

    public void setPreJoinFood(int preJoinFood) {
        this.preJoinFood = preJoinFood;
    }
}
