package com.eventarena.plugin.scoreboard;

import com.eventarena.plugin.EventArenaPlugin;
import com.eventarena.plugin.util.ColorUtil;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Keeps one Scoreboard/Objective per player and only rewrites the lines that
 * changed, instead of recreating the scoreboard every tick (which flickers).
 */
public class EventScoreboardManager {

    private final EventArenaPlugin plugin;
    private final Map<UUID, Scoreboard> boards = new HashMap<>();
    private final Map<UUID, String[]> lastLines = new HashMap<>();

    public EventScoreboardManager(EventArenaPlugin plugin) {
        this.plugin = plugin;
    }

    public void update(Player player) {
        if (!plugin.eventManager().hasEvent()) {
            clear(player);
            return;
        }

        Scoreboard board = boards.computeIfAbsent(player.getUniqueId(), id ->
                plugin.getServer().getScoreboardManager().getNewScoreboard());

        Objective objective = board.getObjective("event_sb");
        if (objective == null) {
            objective = board.registerNewObjective("event_sb", "dummy",
                    ColorUtil.legacy("&c&lEVENT"));
            objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        }

        String[] lines = buildLines();
        String[] previous = lastLines.get(player.getUniqueId());

        if (previous != null) {
            for (String old : previous) {
                board.resetScores(old);
            }
        }

        int score = lines.length;
        for (String line : lines) {
            objective.getScore(line).setScore(score--);
        }

        lastLines.put(player.getUniqueId(), lines);

        if (player.getScoreboard() != board) {
            player.setScoreboard(board);
        }
    }

    private String[] buildLines() {
        var em = plugin.eventManager();
        String mode = em.mode() != null ? ColorUtil.legacy(em.mode().displayName()) : "-";
        return new String[]{
                ColorUtil.legacy("&7PvP: &f" + mode),
                ColorUtil.legacy("&7Players: &f" + em.activeCount() + "/" + em.totalCount()),
                ColorUtil.legacy("&7Border: &f" + plugin.borderManager().currentSize()),
                ColorUtil.legacy("&7Status: &f" + em.state().name()),
                // Scoreboard entries must be unique per line; pad with invisible colour codes.
                ColorUtil.legacy("&r"),
                ColorUtil.legacy("&8play.example.com")
        };
    }

    public void clear(Player player) {
        Scoreboard main = plugin.getServer().getScoreboardManager().getMainScoreboard();
        if (player.getScoreboard() != main) {
            player.setScoreboard(main);
        }
        boards.remove(player.getUniqueId());
        lastLines.remove(player.getUniqueId());
    }
}
