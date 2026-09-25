# EventArena

A Paper PvP event/arena plugin: Crystal PvP, Mace PvP, Sword PvP, Axe PvP, run out of a
dedicated flat world with a configurable border, spectator-on-death, last-player-standing
detection, and a revive system.

## Requirements

- Java 17+
- Maven 3.8+
- A Paper server, 1.20.4 or newer (see "Version compatibility" below)

## Building

```bash
mvn clean package
```

The output jar is `target/event-arena-1.0.0.jar`. Drop it in your server's `plugins/`
folder and restart.

**Important — I could not run this build myself.** The sandbox this was written in has
no network access to Maven Central / PaperMC's repository, so I was not able to actually
compile it end-to-end. I hand-checked every file (structure, imports, method signatures,
brace/paren balance) and it follows completely standard Paper API patterns, but you should
run `mvn clean package` yourself and fix any last mismatches against the exact Paper API
version you target — I'd rather tell you that plainly than pretend it was verified when it
wasn't.

## First run

On first enable the plugin creates `plugins/EventArena/` with `config.yml`, `messages.yml`,
`kits.yml`, and (once you make an event) `state.yml` for the persisted border size.

## Commands

| Command | Description | Permission |
|---|---|---|
| `/event` | Show current event status | `event.use` |
| `/event make` | Open the mode-selection GUI | `event.make` |
| `/event announce <msg>` | Broadcast a formatted announcement | `event.announce` |
| `/event join` | Join the current event | `event.use` |
| `/event leave` | Leave the current event | `event.use` |
| `/event start` | Start the countdown | `event.make` |
| `/event stop` | Force-stop and reset | `event.make` |
| `/event status` | Same as bare `/event` | `event.use` |
| `/border <size>` | Change playable border | `event.border` |
| `/drop` | Clear the arena down to bedrock | `event.drop` |
| `/revive all` | Revive every eliminated player | `event.revive` |
| `/revive <player>` | Revive one player | `event.revive` |

`event.admin` grants all of the above. All commands tab-complete.

## Event lifecycle (state machine)

```
IDLE -> CREATING -> WAITING -> STARTING -> ACTIVE -> ENDING -> FINISHED -> RESETTING -> IDLE
```

`EventManager` is the single owner of this state; nothing else holds its own flags, which
is what the spec asked for to avoid state-desync bugs.

## Configuration

- `config.yml` — world/border sizes, min/max players, countdown length, spread radius,
  block place/break toggles, victory sound/firework, announce format.
- `kits.yml` — per-mode armor + hotbar items, amounts, names, lore, enchants. Edit freely;
  invalid entries are skipped with a warning in the console log rather than crashing.
- `messages.yml` — every player-facing string, with `%placeholder%` support.

## Known limitations / honest notes on the spec

- **"400×400 world with a 100×100 playable border"** — Bukkit/Paper only supports a single
  `WorldBorder` per world; there's no supported API for a second, larger hard boundary. I
  implemented the *playable* border (the one from `/border`) as the real `WorldBorder`, and
  documented 400 as the intended flat-world generation footprint rather than a second
  enforced wall. This is called out in `config.yml` directly above `world.size`.
- **MiniMessage** — Paper ships Adventure/MiniMessage natively, so `ColorUtil` supports it
  for any config string containing `<tags>`, and falls back to legacy `&`-codes otherwise.
- **Death handling** — deaths are intercepted, drops/xp are cleared, and the player is
  force-switched to spectator on respawn rather than actually leaving the server's normal
  death flow untouched — this is what makes "spectate but don't get removed" work reliably
  across a death event.
- **Anti-exploit coverage** — GUI clicks (including shift-click, drag, double-click) are
  fully cancelled in `/event make`; block break/place, item drop, and damage are all gated
  by event state and elimination status; a disconnect mid-fight counts as an elimination so
  it can't be used to dodge a loss or desync the win check; a restart mid-event resets to
  `IDLE` on next boot rather than trying to resume combat state it can no longer trust.
- **Version compatibility** — no NMS and no version-sniffing is used anywhere; everything
  goes through stable Bukkit/Paper API (`WorldCreator`, `WorldBorder`, `Scoreboard`,
  `Inventory`, standard events). The one thing to watch on very new/very old Paper versions
  is the vanilla flat-world generator-settings JSON format in `EventWorldManager`, which is
  a stable, documented vanilla format but is the single most "version-shaped" string in the
  project.

## Permissions

```
event.admin    (default: op)  - everything below
event.use      (default: true) - /event, join, leave, status
event.make     (default: op)  - /event make, start, stop
event.border   (default: op)
event.drop     (default: op)
event.revive   (default: op)
event.announce (default: op)
```
