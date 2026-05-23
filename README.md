<div align="center">

<img src="https://raw.githubusercontent.com/VictorGugug/BlockProt-Reloaded/main/images/BP.png" alt="BlockProt Reloaded" width="520"/>

# BlockProt Reloaded

[![CI](https://img.shields.io/github/actions/workflow/status/VictorGugug/BlockProt-Reloaded/ci.yml?branch=main&style=flat-square&label=CI)](https://github.com/VictorGugug/BlockProt-Reloaded/actions)
[![Release](https://img.shields.io/github/v/release/VictorGugug/BlockProt-Reloaded?style=flat-square&color=brightgreen&label=Release)](https://github.com/VictorGugug/BlockProt-Reloaded/releases)
[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg?style=flat-square)](LICENSE)
[![Java](https://img.shields.io/badge/Java-25+-orange?style=flat-square)](https://openjdk.org/projects/jdk/25/)
[![Paper](https://img.shields.io/badge/Paper-1.21%2B%20%7C%2026.x-white?style=flat-square)](https://papermc.io/)

**Fork created and maintained by [Zar](https://github.com/VictorGugug)**

*Java 25 · Paper 26.x · MySQL index · per-world config · access audit · pet protection · auto-backup · ownership transfer · timed access · statistics TP*

</div>

---

> Block protection plugin for Paper/Spigot servers.
> Players lock chests, furnaces, and other blocks through a modern GUI — no commands to memorize.
> This fork extends the original NBT core with production-grade features for large or long-running servers.

---

## Screenshots

| Block Lock menu | Friend settings |
|:---------------:|:---------------:|
| ![Block lock](https://raw.githubusercontent.com/VictorGugug/BlockProt-Reloaded/main/images/main_menu.png) | ![Friend settings](https://raw.githubusercontent.com/VictorGugug/BlockProt-Reloaded/main/images/friend_settings.png) |

| Player settings | Redstone settings |
|:---------------:|:-----------------:|
| ![Player settings](https://raw.githubusercontent.com/VictorGugug/BlockProt-Reloaded/main/images/user_settings.png) | ![Redstone settings](https://raw.githubusercontent.com/VictorGugug/BlockProt-Reloaded/main/images/redstone_settings.png) |

| Block info | Access log overview |
|:----------:|:-------------------:|
| ![Block info](https://raw.githubusercontent.com/VictorGugug/BlockProt-Reloaded/main/images/block_info.png) | ![Access log](https://raw.githubusercontent.com/VictorGugug/BlockProt-Reloaded/main/images/access_log.png) |

| Access log detail | Timed access |
|:-----------------:|:------------:|
| ![Inside log](https://raw.githubusercontent.com/VictorGugug/BlockProt-Reloaded/main/images/inside_log.png) | ![Timed access](https://raw.githubusercontent.com/VictorGugug/BlockProt-Reloaded/main/images/timed_access.png) |

---

## Installing

### Pre-built JAR

Download the latest JAR from [Releases](https://github.com/VictorGugug/BlockProt-Reloaded/releases)
and drop it in your `plugins/` folder. Requires **Java 25** and **Paper/Spigot 1.21+**.

### Build from source

```bash
git clone https://github.com/VictorGugug/BlockProt-Reloaded.git
cd BlockProt-Reloaded
./gradlew :blockprot-spigot:shadowJar
```

```powershell
# Windows
.\gradlew.bat :blockprot-spigot:shadowJar -PversionSuffix=SNAPSHOT
```

Output → `spigot/build/libs/BlockProt-VERSION.jar`

**Version suffix** (`-PversionSuffix=...` or `gradle.properties → versionSuffix`):

| Value | Output |
|---|---|
| *(blank)* | `BlockProt-1.3.0.jar` — stable release |
| `SNAPSHOT` | `BlockProt-1.3.0-SNAPSHOT-N.jar` — auto-increments, never reuses a number |
| `alpha.1` | `BlockProt-1.3.0-alpha.1.jar` |
| `beta.1` | `BlockProt-1.3.0-beta.1.jar` |
| `rc.1` | `BlockProt-1.3.0-rc.1.jar` |
| `exp` | `BlockProt-1.3.0-exp.jar` — experimental |

---

## File layout

```
plugins/BlockProt/
├── config.yml              ← Main configuration
├── blocks.yml              ← Lockable block lists (generated on first start)
├── worlds.yml              ← Per-world overrides (optional)
├── mysql/
│   ├── mysql.yml               ← MySQL/Storage configuration
│   └── blockprot_audit.sqlite  ← SQLite access audit log
├── lang/
│   └── translations_*.yml
├── logs/
│   └── session-YYYY-MM-DD.log
└── backups/
    └── config-backup-YYYY-MM-DD_HH-MM.yml   ← Created on plugin version upgrade
```

`blocks.yml` is generated automatically. If your old `config.yml` had lockable block lists, those values are migrated and removed from `config.yml` automatically.

---

## GUI overview

### Block lock (sneak + right-click any lockable block)

Two-row inventory. Top row holds functional buttons; bottom row holds utility buttons.

**Top row (slots 0–8):**

| Slot | Item | Condition |
|---|---|---|
| 0 | Block icon | Lock / Unlock toggle |
| 1 | Redstone | Redstone / piston / hopper settings |
| 2 | Player Head | Manage friends |
| 3 | Name Tag | Set custom block name |
| 4 | Ender Pearl | Transfer block ownership |

**Bottom row (slots 9–17):**

| Slot | Item | Condition |
|---|---|---|
| 9 | Spyglass — Inspect contents | Admin + non-owner + block has inventory |
| 13 | Clock — Access log | Owner or admin + audit logger active |
| 14 | Knowledge Book — Paste config | Manager + clipboard has data |
| 15 | Paper — Copy config | Manager |
| 16 | Compass — Block info | Manager |
| 17 | **Barrier — Back** | Always |

Copying sends **"Configuration copied."** in the action bar.
Pasting sends **"Configuration pasted."** in the action bar.

### Statistics list

Each entry shows the **real block icon** (White Shulker Box, Barrel, Copper Chest, etc.) and a title with the block type and coordinates. Click any entry to **teleport** to that block (requires `blockprot.blocks.tp`).

Broken blocks (AIR at that location) are automatically filtered from the list.

### User menu (`/bp user`, requires `use_menus: true`)

Three-row inventory. Items in the middle row:

| Item | Material | Action |
|---|---|---|
| My Settings | Writable Book | Lock-on-place, hints, global friends |
| Friends | Player Head | Default friend list |
| Statistics | Book | Block statistics |
| Transfer block | Ender Pearl | Hint: use `/bp transfer <player>` |
| Timed access | Clock | Hint: use `/bp timed <player> <seconds>` |
| About | Nether Star | Plugin/fork info |

### Admin menu (`/bp admin`, requires `use_menus: true` + `blockprot.user.admin`)

Three-row inventory. Items in the middle row:

| Slot | Item | Action |
|---|---|---|
| 11 | Comparator | Reload config + translations |
| 12 | Spyglass | Check for updates |
| 13 | Chain | List active integrations |
| 14 | Book | Open server statistics |
| 15 | Command Block | Run diagnostics (`/bp debug run`) |
| 16 | **Player Head** | **Open player block-list GUI** |

### Player block-list (`/bp info <player>` or Admin menu → Player Head)

Six-row GUI listing every block owned by the selected player. Each entry shows the **real block icon** with coordinates. Click any entry to **teleport** (requires `blockprot.blocks.tp`). Supports pagination. Back button (Barrier) returns to admin menu when opened from there.

Requires `blockprot.user.admin`.

---

## Commands

Extra commands are **disabled by default** when `use_menus: false`.
Set `use_menus: true` in `config.yml` to activate the GUI menus (`/bp`, `/bp user`, `/bp admin`) and disable the CLI subcommands.

| Command | Permission | Available when |
|---|---|---|
| `/bp user` | `blockprot.user` | `use_menus: true` |
| `/bp admin` | `blockprot.user.admin` | `use_menus: true` |
| `/bp transfer <player>` | `blockprot.user` | `use_menus: false` (default) |
| `/bp timed <player> <seconds>` | `blockprot.user` | `use_menus: false` |
| `/bp friends addall <player>` | `blockprot.user` | `use_menus: false` |
| `/bp stats` | `blockprot.user` | `use_menus: false` |
| `/bp info <player>` | `blockprot.user.admin` | always |
| `/bp reload` | `blockprot.user.admin` | always |
| `/bp update` | `blockprot.user.admin` | always |
| `/bp integrations` | `blockprot.user.admin` | always |
| `/bp debug <run\|...>` | `blockprot.user.admin` | always |
| `/bp disablehints` | `blockprot.user` | always |
| `/bp about` | any | always |
| `/bp help` | any | always |

Alias: `/blockprot`.

> **`/bp info <player>`** opens a live GUI for Player senders and falls back to chat output for Console.
> Works for **offline players** too — reads their stats directly from the NBT file.

---

## Permissions

| Permission | Default | Description |
|---|---|---|
| `blockprot.user` | `true` | All standard user actions (lock, friends, settings, stats, etc.) |
| `blockprot.user.admin` | `op` | Admin actions: reload, debug, info, integrations, update |
| `blockprot.bypass` | `false` | Bypass all block protections |
| `blockprot.blocks.tp` | `op` | Teleport to a block from the statistics or admin block-list |

---

## Features

### Core block protection (upstream)

- Sneak + right-click any lockable block to open the protection GUI.
- Add friends with **Read / Write / Manager** permission levels.
- Redstone, hopper, and piston protection toggles per block.
- Copy / paste protection settings between blocks.
- Per-player default friend list applied to all new locks.

---

### BlockProt Reloaded additions

#### 1. Java 25 / Paper 26.x Compatibility

Compiles and runs on Java 25. Detects both `1.x` and year-based `26.x` server version schemes at runtime. Validates Java version, Paper availability, and typed inventory view support on startup.

#### 2. Persistent Session Logging

One timestamped log file per server session under `plugins/BlockProt/logs/`. Logs plugin version, server version, and compatibility results. Console stays clean.

#### 3. Hybrid MySQL / NBT Backend *(optional)*

NBT remains the source of truth. MySQL/MariaDB is an optional index for fast lookups via HikariCP connection pool. All SQL is asynchronous with an in-memory trust cache. Configure in `mysql/mysql.yml`.

#### 4. Separate Block Definitions (`blocks.yml`)

Lockable block lists live in `blocks.yml`, not `config.yml`. Old lists are migrated automatically. Edit and run `/bp reload` — no restart needed.

#### 5. Master Friend List & `/bp friends addall`

`/bp friends addall <player>` adds a player to every block you own at once.

#### 6. SQLite Access Audit Log

Stored at `mysql/blockprot_audit.sqlite`. Records `ACCESS_DENIED`, `ACCESS_GRANTED`, `OPENED`, `ITEM_TAKEN`, `ITEM_PLACED`. All writes asynchronous; automatic pruning at 50 000 entries. In-game GUI accessible from the block lock (Clock button, slot 13). **Owner access is never logged** — only friends and other players appear in the log.

#### 7. Automatic Backup on Version Upgrade

A config backup is created under `plugins/BlockProt/backups/` **only when the plugin version changes** (upgrade). No backup is created on routine restarts. The stored version is tracked in `config.yml` as `last_known_version`.

#### 8. Inactivity Cleanup *(optional)*

`inactivity_cleanup_days: -1` (disabled). Removes protections from blocks owned by long-inactive players on startup.

#### 9. Per-World Configuration (`worlds.yml`)

Each world can override lockable block lists and enable/disable protection entirely. Missing worlds added automatically.

#### 10. Config File Watcher

Watches `config.yml`, `worlds.yml`, `blocks.yml`, `mysql/mysql.yml`, and `lang/*.yml`. Auto-reloads on change, debounced 2 s.

#### 11. Security Options

| Key | Default | Description |
|---|---|---|
| `protect_locked_blocks_from_explosions` | `true` | Explosions cannot destroy locked blocks |
| `block_protected_block_piston_movement` | `true` | Pistons cannot move locked blocks |
| `allow_break_protected_blocks` | `false` | Any player can break (for reinforcement servers) |
| `respect_spawn_protection` | `true` | No locking inside spawn-protection radius |
| `clear_protection_on_shulker_break` | `false` | Shulker drops without lock NBT |

#### 12. WorldEdit / FAWE Paste Auto-Lock *(optional)*

Auto-locks unprotected blocks near a `//paste` origin. `worldedit_paste_autolock.enabled: false` by default.

#### 13. Floodgate / Geyser Bedrock Support

Configurable `bedrock_username_prefixes` for Bedrock player name resolution.

#### 14. Self-Repair: Config & Lang Key Merging

Missing keys added from JAR defaults on every startup and `/bp reload`. Obsolete keys removed automatically.

#### 15. Colored Particle Effects & Sounds

Lock → green dust ring + sound. Unlock → red dust ring + sound. Shulker boxes use shulker open/close sounds.

#### 16. Pet Protection *(default: disabled)*

Protects tamed animals (wolves, cats, parrots, horses, llamas …).

| Toggle | Description |
|---|---|
| `enabled` | Master switch (default **false**) |
| `auto_protect_on_tame` | Protect automatically when tamed |
| `no_damage` | Block other players from damaging the pet |
| `no_interact` | Block right-click (feeding, naming, sitting) |
| `no_leash` | Block leash/unleash by others |
| `no_pickup` | Block parrot shoulder-pickup by others |

Right-click your pet while holding the configured `menu_item` (default: Stick) to open the settings GUI.

#### 17. Update Checker

Queries GitHub Releases API once per session. Detects SNAPSHOT, alpha, beta, rc builds. Console warning + optional op join message when outdated.

#### 18. Ownership Transfer (`/bp transfer` or GUI)

`/bp transfer <player>` — look at any block you own and run the command. The target becomes new owner; original owner is added as friend automatically.

#### 19. Copy-Paste (fixed, GitHub #268)

Pasting **replaces** the friend list (not appends), matching expected behavior. Owner is never overwritten.

#### 20. Time-Limited Friend Access (`/bp timed`)

`/bp timed <player> <seconds>` — grants temporary access to a looked-at block. Access revoked automatically when the timer elapses.

#### 21. Admin Player Block-List (`/bp info <player>`)

Opens a full-page GUI showing every block the player currently owns, with the **correct block icon** (shulker box, barrel, copper chest, etc.) and coordinates. Click any entry to teleport. Works for **offline players**. Requires `blockprot.user.admin`.

Also accessible from the admin menu (Player Head button, slot 16) via an in-game name search.

#### 22. Stats: Real Block Icons

The statistics list reads the **live block type** at each stored location. Shulker boxes show their correct coloured icon; copper chests, barrels, trapped chests, etc. all display their real material. Stale entries (blocks destroyed without being removed from stats) are filtered out automatically.

#### 23. Stat Cleanup on Block Break

When a shulker box or any protected block is broken by its owner, the entry is immediately removed from the statistics file. No stale entries remain.

#### 24. GUI / Command Mode Toggle (`use_menus`)

```yaml
# config.yml
use_menus: false   # default
```

| `use_menus` | `/bp`, `/bp user`, `/bp admin` | Extra CLI commands |
|---|---|---|
| `false` (default) | ❌ Disabled | ✅ Active |
| `true` | ✅ Active | ❌ Disabled |

---

## Configuration reference

### `config.yml`

```yaml
# ── 1. General ──────────────────────────────────────────────────────
language_file: translations_en.yml
fallback_string: "Unknown translation"
replace_translations: true
notify_op_of_updates: false
excluded_worlds: []
worlds_config_enabled: false
bedrock_username_prefixes: [".", "*", "_"]
inactivity_cleanup_days: -1          # -1 = disabled

# ── 2. Player & Protection Defaults ─────────────────────────────────
lock_on_place_by_default: true
public_is_friend_by_default: false
player_max_locked_block_count: -1    # -1 = unlimited
lock_hint_cooldown_in_seconds: 10
friend_search_similarity: 0.5        # 0.0–1.0
disable_friend_functionality: false
redstone_disallowed_by_default: false

# ── 3. Safety & Protection Behavior ─────────────────────────────────
protect_locked_blocks_from_explosions: true
block_protected_block_piston_movement: true
clear_protection_on_shulker_break: false
allow_break_protected_blocks: false
respect_spawn_protection: true

# ── 4. Pet Protection ────────────────────────────────────────────────
pet_protection:
  enabled: false
  auto_protect_on_tame: true
  menu_item: STICK

# ── 5. Effects ────────────────────────────────────────────────────────
block_lock_effects: true
block_lock_sounds: true

# ── 6. Mode toggle ────────────────────────────────────────────────────
# false (default): all extra CLI commands active, GUI menus disabled.
# true: GUI menus active (/bp user, /bp admin), CLI commands disabled.
use_menus: false

timed_access_max_duration_days: 90

worldedit_paste_autolock:
  enabled: false
  radius: 24
  max_blocks_per_paste: 5000
  delay_ticks: 20
```

### `mysql/mysql.yml`

```yaml
mysql:
  enabled: false
  host: "127.0.0.1"
  port: 3306
  database: "blockprot"
  username: "blockprot"
  password: ""
```

### `blocks.yml`

```yaml
lockable_tile_entities:
  - CHEST
  - TRAPPED_CHEST
  - FURNACE
  - SMOKER
  - BLAST_FURNACE
  - HOPPER
  - BARREL
  - BREWING_STAND
  - DISPENSER
  - DROPPER
  - LECTERN
  - BEEHIVE
  - BEE_NEST
  - OAK_SIGN
  # ... (all sign variants)
lockable_shulker_boxes:
  - SHULKER_BOX
  - WHITE_SHULKER_BOX
  # ... (all 16 colors)
lockable_blocks:
  - ANVIL
  - OAK_FENCE_GATE
  - OAK_TRAPDOOR
  # ...
lockable_doors:
  - OAK_DOOR
  - IRON_DOOR
  # ...
```

---

## Integrations

| Plugin | Notes |
|---|---|
| **Towny** | Respects town and nation permissions |
| **WorldGuard** | Honors region flags |
| **PlaceholderAPI** | Exposes stats and protection status as placeholders |
| **Lands** | Supports Lands claim permission checks |
| **ClaimChunk** | Prevents locking in chunks you don't own |
| **SkinsRestorer** | Correct player heads in offline-mode servers |
| **WorldEdit / FAWE** | Optional paste auto-lock |

---

## Compatibility

| | |
|---|---|
| **Minecraft** | 1.21, 1.21.1, 1.21.4, 26.1.x |
| **Server** | Paper, Spigot |
| **Java** | 25+ required |
| **MySQL** | MySQL 8+, MariaDB 10.5+ (optional) |
| **Languages** | EN, ES, DE, FR, IT, PT-BR, RU, JA, KO, ZH-CN, ZH-TW, CS, SK, PL, TR |

---

## Translating

Language files in `spigot/src/main/resources/lang/`. All values support legacy color codes (`§a`, `§l`, etc.).
The English file (`translations_en.yml`) is the reference — all other files are auto-updated with missing keys on startup.

---

## Contact / Support

Maintained by **Zar**.
[Open an issue](https://github.com/VictorGugug/BlockProt-Reloaded/issues) for bugs or suggestions.

---

## License

Licensed under the **GNU General Public License v3**. See [`LICENSE`](LICENSE) for details.

<sub>Based on <a href="https://github.com/spnda/BlockProt">BlockProt</a> by spnda — original copyright notices preserved as required by GPL v3.</sub>
