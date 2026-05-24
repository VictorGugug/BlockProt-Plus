<div align="center">

# BlockProt Reloaded

[![CI](https://img.shields.io/github/actions/workflow/status/VictorGugug/BlockProt-Reloaded/ci.yml?branch=main&style=flat-square&label=CI)](https://github.com/VictorGugug/BlockProt-Reloaded/actions)
[![Release](https://img.shields.io/github/v/release/VictorGugug/BlockProt-Reloaded?style=flat-square&color=brightgreen&label=Release)](https://github.com/VictorGugug/BlockProt-Reloaded/releases)
[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg?style=flat-square)](LICENSE)
[![Java](https://img.shields.io/badge/Java-25+-orange?style=flat-square)](https://openjdk.org/projects/jdk/25/)
[![Paper](https://img.shields.io/badge/Paper-1.20%2B%20%7C%2026.x-white?style=flat-square)](https://papermc.io/)

**Fork created and maintained by [Zar](https://github.com/VictorGugug)**

*Java 25 · Paper 26.x · MySQL index · per-world config · access audit · pet protection · auto-backup · ownership transfer · timed access · statistics TP*

</div>

---

> Block protection plugin for Paper/Spigot servers.
> Players lock chests, furnaces, and other blocks through a modern GUI — no commands to memorize.
> This fork extends the original NBT core with production-grade features for large or long-running servers.

---

## Screenshots

### Block Lock Menu
![Block lock](https://raw.githubusercontent.com/VictorGugug/BlockProt-Reloaded/main/images/main_menu.png)

The main interface for locking blocks. Sneak + right-click any lockable block to open. Two-row inventory with functional buttons (top) and utility buttons (bottom).

---

### Friend Settings
![Friend settings](https://raw.githubusercontent.com/VictorGugug/BlockProt-Reloaded/main/images/friend_settings.png)

Manage friends with different permission levels (Read, Write, Manager). Add or remove friends from your protected blocks.

---

### Player Settings
![Player settings](https://raw.githubusercontent.com/VictorGugug/BlockProt-Reloaded/main/images/user_settings.png)

Configure personal preferences: lock-on-place behavior, hint toggles, and manage your global friends list.

---

### Redstone Settings
![Redstone settings](https://raw.githubusercontent.com/VictorGugug/BlockProt-Reloaded/main/images/redstone_settings.png)

Control redstone behavior for your protected blocks. Toggle redstone, piston, and hopper interactions.

---

### Block Info
![Block info](https://raw.githubusercontent.com/VictorGugug/BlockProt-Reloaded/main/images/block_info.png)

View detailed information about the current block: owner, friends, protection status, and additional metadata.

---

### Access Log Overview
![Access log](https://raw.githubusercontent.com/VictorGugug/BlockProt-Reloaded/main/images/access_log.png)

See a summary of all access attempts to your protected blocks. View when friends opened or interacted with your blocks.

---

### Access Log Detail
![Inside log](https://raw.githubusercontent.com/VictorGugug/BlockProt-Reloaded/main/images/inside_log.png)

Deep dive into individual access records with timestamps, player names, and action types (opened, item taken, item placed, access denied).

---

### Timed Access
![Timed access](https://raw.githubusercontent.com/VictorGugug/BlockProt-Reloaded/main/images/timed_access.png)

Grant temporary access to friends. Set a time limit and access is automatically revoked when the timer expires.

---

### Admin Player Block-List
![Admin view](https://raw.githubusercontent.com/VictorGugug/BlockProt-Reloaded/main/images/admin_view.png)

Admin tool to view all blocks owned by any player. Click to teleport to each block. Shows real block icons and lock timestamps.

---

## Installing

### Pre-built JAR

Download the latest JAR from [Releases](https://github.com/VictorGugug/BlockProt-Reloaded/releases)
and drop it in your `plugins/` folder. Requires **Java 25** and **Paper/Spigot 1.20+**.

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

**Version suffix** (`-PversionSuffix=...` or edit `versionSuffix` in `gradle.properties`):

| Value | Output |
|---|---|
| *(blank)* | `BlockProt-1.3.0.jar` — stable release |
| `SNAPSHOT` | `BlockProt-1.3.0-SNAPSHOT.jar` |
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

Each entry shows the **real block icon** (White Shulker Box, Barrel, Copper Chest, Shelf, etc.) and a title with the block type and coordinates. Each entry also shows **how long ago the block was locked** (e.g. `Locked 3 days ago`) when a timestamp is available. Click any entry to **teleport** to that block (requires `blockprot.blocks.tp`).

Broken blocks (AIR at that location) are automatically filtered from the list.

### User menu (`/bp user`, requires `use_menus: true`)

Three-row inventory. Items span slots 10–16 of the middle row (27-slot inventory):

| Slot | Item | Material | Action |
|---|---|---|---|
| 10 | My Settings | Writable Book | Lock-on-place, hints, global friends |
| 11 | Friends | Player Head | Default friend list |
| 12 | Statistics | Book | Block statistics |
| 13 | Transfer block | Ender Pearl | Sends chat hint: look at a block → `/bp transfer <player>` |
| 14 | Timed access | Clock | Sends chat hint: look at a block → `/bp timed <player> <seconds>` |
| 16 | About | Nether Star | Plugin/fork info |

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

Six-row GUI listing every block owned by the selected player. Each entry shows the **real block icon** with coordinates and time since locking. Click any entry to **teleport** (requires `blockprot.blocks.tp`). Supports pagination. Back button (Barrier) returns to admin menu when opened from there.

Requires `blockprot.user.admin`.

---

## Commands

Extra commands are **disabled by default** when `use_menus: true`.
Set `use_menus: true` in `config.yml` to activate the GUI menus (`/bp user`, `/bp admin`) and disable the CLI subcommands.

| Command | Permission | Available when |
|---|---|---|
| `/bp user` | `blockprot.user` | `use_menus: true` |
| `/bp admin` | `blockprot.user.admin` | `use_menus: true` |
| `/bp transfer <player>` | `blockprot.user` | `use_menus: false` |
| `/bp timed <player> <seconds>` | `blockprot.user` | `use_menus: false` |
| `/bp friends addall <player>` | `blockprot.user` | `use_menus: false` |
| `/bp stats` | `blockprot.user` | `use_menus: false` |
| `/bp info <player>` | `blockprot.user.admin` | `use_menus: false` |
| `/bp reload` | `blockprot.user.admin` | `use_menus: false` |
| `/bp update` | `blockprot.user.admin` | `use_menus: false` |
| `/bp integrations` | `blockprot.user.admin` | `use_menus: false` |
| `/bp debug <run\|...>` | `blockprot.user.admin` | `use_menus: false` |
| `/bp disablehints` | `blockprot.user` | `use_menus: false` |
| `/bp about` | any | Always |
| `/bp help` | any | Always |

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
| `blockprot.lockmax` | `false` | Check for specific lock limits defined by the `blockprot.locklimit.<number>` nodes |
| `blockprot.locklimit.<number>` | `false` | Limit the maximum number of locked blocks for a player to `<number>` |

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

#### 1. Java 25 / Paper 1.20–26.x Compatibility

Compiles against Paper/Spigot 1.20.6 API and runs on any version from 1.20 through 26.1.x.
Detects both `1.x` and year-based `26.x` server version schemes at runtime.
APIs introduced after 1.20.6 (typed inventory views in 1.21.4, sign editor in 1.20) are accessed
via `VersionCompat` checks and reflection — zero `NoClassDefFoundError` on older servers.
Validates Java version, Paper availability, and typed inventory view support on startup.

#### 2. Persistent Session Logging

One timestamped log file per server session under `plugins/BlockProt/logs/`. Logs plugin version, server version, and compatibility results. Console stays clean.

#### 3. Hybrid MySQL / NBT Backend *(optional)*

NBT remains the source of truth. MySQL/MariaDB is an optional index for fast lookups via HikariCP connection pool. All SQL is asynchronous with an in-memory trust cache. Configure in `mysql/mysql.yml`.

#### 4. Separate Block Definitions (`blocks.yml`)

Lockable block lists live in `blocks.yml`, not `config.yml`. Old lists are migrated automatically. Edit and run `/bp reload` — no restart needed.

#### 5. Master Friend List & `/bp friends addall`

`/bp friends addall <player>` adds a player to every block you own at once.

#### 6. SQLite Access Audit Log

Stored at `mysql/blockprot_audit.sqlite`. Records `ACCESS_DENIED`, `ACCESS_GRANTED`, `OPENED`, `ITEM_TAKEN`, `ITEM_PLACED`. All writes asynchronous; automatic pruning at 50,000 entries. In-game GUI accessible from the block lock (Clock button, slot 13). **Owner access is never logged** — only friends and other players appear in the log.

#### 7. Automatic Backup on Version Upgrade

A config backup is created under `plugins/BlockProt/backups/` **only when the plugin version changes** (upgrade). No backup is created on routine restarts. The stored version is tracked in `config.yml` as `last_known_version`.

#### 8. Inactivity Cleanup *(optional)*

`inactivity_cleanup_days: -1` (disabled). Removes protections from blocks owned by long-inactive players on startup. Asynchronous execution prevents server thread hang.

#### 9. Per-World Configuration (`worlds.yml`)

Each world can override lockable block lists and enable/disable protection entirely. Missing worlds added automatically on startup.

#### 10. Config File Watcher

Watches `config.yml`, `worlds.yml`, `blocks.yml`, `mysql/mysql.yml`, and `lang/*.yml`. Auto-reloads on change, debounced 2 seconds to prevent race conditions.

#### 11. Security Options

| Key | Default | Description |
|---|---|---|
| `protect_locked_blocks_from_explosions` | `true` | Explosions cannot destroy locked blocks |
| `block_protected_block_piston_movement` | `true` | Pistons cannot move locked blocks |
| `allow_break_protected_blocks` | `false` | Any player can break (for reinforcement servers) |
| `respect_spawn_protection` | `true` | No locking inside spawn-protection radius |
| `clear_protection_on_shulker_break` | `false` | Shulker drops without lock NBT |

#### 12. WorldEdit / FAWE Paste Auto-Lock *(optional)*

Auto-locks unprotected blocks near a `//paste` origin. `worldedit_paste_autolock.enabled: false` by default. Configurable radius and max blocks per paste.

#### 13. Floodgate / Geyser Bedrock Support

Configurable `bedrock_username_prefixes` for Bedrock player name resolution. Players joining via Bedrock clients are correctly identified.

#### 14. Self-Repair: Config & Lang Key Merging

Missing keys added from JAR defaults on every startup and `/bp reload`. Obsolete keys removed automatically. Translation files are auto-updated with missing entries.

#### 15. Colored Particle Effects & Sounds

Lock → green dust ring + sound. Unlock → red dust ring + sound. Shulker boxes use shulker open/close sounds. Toggleable via `block_lock_effects` and `block_lock_sounds`.

#### 16. Pet Protection *(default: disabled)*

Protects tamed animals (wolves, cats, parrots, horses, llamas, etc.).

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

`/bp timed <player> <seconds>` — grants temporary access to a looked-at block. Access revoked automatically when the timer elapses. Configurable max duration via `timed_access_max_duration_days`.

#### 21. Admin Player Block-List (`/bp info <player>`)

Opens a full-page GUI showing every block the player currently owns, with the **correct block icon** (shulker box, barrel, copper chest, shelf, etc.), coordinates, and time since locking. Click any entry to teleport. Works for **offline players**. Requires `blockprot.user.admin`.

Also accessible from the admin menu (Player Head button, slot 16) via an in-game name search.

#### 22. Stats: Real Block Icons + Lock Timestamp

The statistics list reads the **live block type** at each stored location. Shulker boxes show their correct coloured icon; copper chests, barrels, trapped chests, shelves, decorated pots, and all other block types display their real material. Each entry shows **how long ago the block was locked** (e.g. `Locked 3 days ago`) when a timestamp is available — blocks locked before this feature was added show no time. Stale entries (blocks that no longer exist) are filtered out automatically.

#### 23. Stat Cleanup on Block Break

When a shulker box or any protected block is broken by its owner, the entry is immediately removed from the statistics file. No stale entries remain.

#### 24. GUI / Command Mode Toggle (`use_menus`)

```yaml
# config.yml
use_menus: false   # default
```

| `use_menus` | `/bp user`, `/bp admin` (GUI) | Extra CLI commands (transfer, timed, stats, etc.) | Tab-complete shows |
|---|---|---|---|
| `false` (default) | ❌ Hidden from tab-complete and disabled | ✅ Active | CLI subcommands |
| `true` | ✅ Active | ❌ Hidden from tab-complete and disabled | `user` / `admin` |

#### 25. Sign Editor Input (zero NMS)

Where text input is needed (player name search, block rename) the plugin uses the native sign editor (`player.openSign()`) introduced in Bukkit 1.20 instead of the anvil GUI. No XP cost display, no item required, cleaner UX. The sign is never written to the world — `SignChangeEvent` is cancelled and only line 0 is captured. On pre-1.20 servers the anvil GUI is used as fallback. No NMS, no external libraries.

#### 26. Comprehensive Block Coverage

BlockProt Reloaded protects every storage and interactable block in the game, covering all versions from 1.20 through 26.1.x. All lists are defined in `blocks.yml` and can be changed without restarting — just run `/bp reload`.

**Storage blocks (tile entities):**

| Category | Blocks |
|---|---|
| Standard chests | Chest, Trapped Chest, Ender Chest |
| Copper chests *(1.21.9 / 26.1+)* | Copper Chest + Copper Trapped Chest × 4 oxidation stages × waxed/unwaxed = **16 variants** |
| Shulker boxes | All 17 variants (undyed + 16 colours). Protection data survives breaking and re-placing. |
| Furnaces | Furnace, Smoker, Blast Furnace |
| Transport | Hopper, Dispenser, Dropper |
| Misc storage | Barrel, Brewing Stand |
| Shelves *(1.21.9 / 26.1+)* | Oak, Spruce, Birch, Jungle, Acacia, Dark Oak, Mangrove, Cherry, Pale Oak, Bamboo, Crimson, Warped — **12 variants**. Stores 3 item stacks displayed on the front face; interacting swaps your hand item with a slot. |
| Decorated Pot *(1.20+)* | Stores 1 item stack; hopper-compatible |
| Chiseled Bookshelf *(1.20+)* | Stores up to 6 books; redstone-readable slot access |
| Crafter *(1.21+)* | Automated crafting block; full 3×3 item grid |
| Jukebox *(1.21+)* | Stores 1 music disc; hopper-accessible |
| Lectern | Holds and displays a single book |
| Beehive / Bee Nest | Protect honey and honeycomb production |

**Interactive blocks (non-storage):**

| Block | Why protect it |
|---|---|
| **Dragon Egg** | Right-clicking teleports it away — locking prevents other players from stealing or teleporting your dragon egg |
| Composter | Block others from depositing items or stealing bone meal |
| Cauldron (all variants) | Water, Lava, Powder Snow — block filling or draining by others |
| Bell | Prevent unwanted ringing |
| Note Block | Protect creative music builds from pitch changes by others |
| Enchanting Table | Block others from using your table and spending your XP levels |
| Grindstone | Block disenchanting of items by others |
| Stonecutter | Block use by others |
| Loom | Block banner design changes by others |
| Cartography Table | Block map editing by others |
| Smithing Table | Block others from using your smithing table |
| Anvils | Anvil, Chipped Anvil, Damaged Anvil |

**Doors, trapdoors, and gates:**

| Category | Variants |
|---|---|
| Wooden doors | Oak, Spruce, Birch, Jungle, Acacia, Dark Oak, Mangrove, Cherry, Pale Oak, Bamboo, Crimson, Warped |
| Iron door | Iron Door |
| Copper doors *(1.21+)* | 4 oxidation stages + waxed/unwaxed = **9 variants** |
| Wooden trapdoors | All 12 wood variants |
| Iron trapdoor | Iron Trapdoor |
| Copper trapdoors *(1.21+)* | 4 oxidation stages + waxed/unwaxed = **9 variants** |
| Fence gates | All 12 wood variants |

> All blocks added in versions newer than 1.20 are registered via `Material.matchMaterial()` at startup — the plugin never crashes on older servers where those materials do not exist yet. Unknown names are silently skipped.

#### 27. Auto-Drop to Inventory

Shulker boxes and other valuable items drop directly into the owner's inventory when broken, preventing item theft. Configurable per-block via `auto_drop_to_inventory` section.

#### 28. Hopper Protection & Caching

Hoppers and other transport blocks respect block protections. Event caching with TTL prevents lag on hopper-heavy setups.

#### 29. Folia Support

Plugin is Folia-compatible, allowing async chunk handling on modern Paper versions.

#### 30. PlaceholderAPI Integration

Exposes block lock stats and protection status as PlaceholderAPI placeholders for use in scoreboards, tab lists, and other plugins. Requires **PlaceholderAPI** plugin to be installed.

**Available Placeholders:**

| Placeholder | Type | Description |
|---|---|---|
| `%blockprot_global_block_count%` | Global | Total blocks locked on the entire server |
| `%blockprot_own_block_count%` | Per-player | Number of blocks locked by the player |
| `%blockprot_default_friends%` | Per-player | Comma-separated list of a player's default friends |

**Usage Examples:**

Scoreboard (via plugins like Tablist):
```
%blockprot_own_block_count% blocks locked
Global: %blockprot_global_block_count%
Friends: %blockprot_default_friends%
```

Chat (via plugins like LiteBans, TAB, etc.):
```
Player {player} has {blockprot_own_block_count} protected blocks
Server total: {blockprot_global_block_count}
```

#### 31. Towny, WorldGuard, Lands, ClaimChunk Integration

Respects town permissions, region flags, claim ownership, and other protection plugins' restrictions. Cannot lock blocks in areas you don't own.

#### 32. SkinsRestorer Support

Displays correct player head icons in offline-mode servers using SkinsRestorer's cache.

---

## Configuration reference

### `config.yml`

```yaml
# ── 1. General ──────────────────────────────────────────────────────
language_file: translations_en.yml
fallback_string: "Unknown translation"
replace_translations: true
notify_op_of_updates: false
localized_command_aliases: true
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

# ── 6. Timed Access ──────────────────────────────────────────────────
timed_access_max_duration_days: 90

# ── 7. WorldEdit Integration ─────────────────────────────────────────
worldedit_paste_autolock:
  enabled: false
  radius: 24
  max_blocks_per_paste: 5000
  delay_ticks: 20

# ── 8. Auto-Drop to Inventory ────────────────────────────────────────
auto_drop_to_inventory:
  enabled: true
  blocks:
    - SHULKER_BOX
    - WHITE_SHULKER_BOX
    # ... (all 17 shulker variants)

# ── 9. Menus & Commands ──────────────────────────────────────────────
use_menus: false
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

The full default file is generated at `plugins/BlockProt/blocks.yml` on first start. Below is an annotated summary of every section.

```yaml
# ── Tile entities (storage + interactive blocks with NBT) ─────────────
lockable_tile_entities:
  # Standard & copper chests
  - CHEST
  - TRAPPED_CHEST
  - ENDER_CHEST
  - COPPER_CHEST                        # 1.21.9+
  - EXPOSED_COPPER_CHEST                # 1.21.9+
  - WEATHERED_COPPER_CHEST              # 1.21.9+
  - OXIDIZED_COPPER_CHEST               # 1.21.9+
  - WAXED_COPPER_CHEST                  # 1.21.9+
  - WAXED_EXPOSED_COPPER_CHEST          # 1.21.9+
  - WAXED_WEATHERED_COPPER_CHEST        # 1.21.9+
  - WAXED_OXIDIZED_COPPER_CHEST         # 1.21.9+
  - COPPER_TRAPPED_CHEST                # 1.21.9+
  # ... (8 more copper trapped chest variants)

  # Shelves (all 12 wood variants — 1.21.9+)
  - OAK_SHELF
  - SPRUCE_SHELF
  - BIRCH_SHELF
  - JUNGLE_SHELF
  - ACACIA_SHELF
  - DARK_OAK_SHELF
  - MANGROVE_SHELF
  - CHERRY_SHELF
  - PALE_OAK_SHELF
  - BAMBOO_SHELF
  - CRIMSON_SHELF
  - WARPED_SHELF

  # Furnaces
  - FURNACE
  - SMOKER
  - BLAST_FURNACE

  # Transport
  - HOPPER
  - DISPENSER
  - DROPPER

  # Misc storage
  - BARREL
  - BREWING_STAND
  - DECORATED_POT          # 1.20+ — stores 1 stack, hopper-compatible
  - CHISELED_BOOKSHELF     # 1.20+ — stores 6 books, redstone-readable
  - CRAFTER                # 1.21+ — automated 3x3 crafting
  - JUKEBOX                # stores 1 disc, hopper-accessible in 1.21+
  - LECTERN
  - BEEHIVE
  - BEE_NEST

# ── Shulker boxes (all 17 variants) ─────────────────────────────────
lockable_shulker_boxes:
  - SHULKER_BOX
  - WHITE_SHULKER_BOX
  - LIGHT_GRAY_SHULKER_BOX
  - GRAY_SHULKER_BOX
  - BLACK_SHULKER_BOX
  - BROWN_SHULKER_BOX
  - RED_SHULKER_BOX
  - ORANGE_SHULKER_BOX
  - YELLOW_SHULKER_BOX
  - LIME_SHULKER_BOX
  - GREEN_SHULKER_BOX
  - CYAN_SHULKER_BOX
  - LIGHT_BLUE_SHULKER_BOX
  - BLUE_SHULKER_BOX
  - PURPLE_SHULKER_BOX
  - MAGENTA_SHULKER_BOX
  - PINK_SHULKER_BOX

# ── Interactive blocks (non-storage) ────────────────────────────────
lockable_blocks:
  # Special
  - DRAGON_EGG             # prevents teleportation theft
  - COMPOSTER
  - CAULDRON
  - WATER_CAULDRON
  - LAVA_CAULDRON
  - POWDER_SNOW_CAULDRON
  - BELL
  - NOTE_BLOCK
  # Workstations
  - GRINDSTONE
  - STONECUTTER
  - LOOM
  - CARTOGRAPHY_TABLE
  - SMITHING_TABLE
  - ENCHANTING_TABLE
  # Anvils
  - ANVIL
  - CHIPPED_ANVIL
  - DAMAGED_ANVIL
  # Fence gates (12 wood variants)
  - OAK_FENCE_GATE
  # ... (all 12 wood variants)
  # Trapdoors (wooden + iron + copper)
  - OAK_TRAPDOOR
  - IRON_TRAPDOOR
  - COPPER_TRAPDOOR         # 1.21+
  # ... (all oxidation variants)

# ── Doors ────────────────────────────────────────────────────────────
lockable_doors:
  # Wooden doors (12 variants)
  - OAK_DOOR
  # ...
  # Iron door
  - IRON_DOOR
  # Copper doors 1.21+ (9 variants)
  - COPPER_DOOR
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
| **Floodgate / Geyser** | Bedrock player name resolution |
| **Folia** | Async chunk handling support |

---

## Compatibility

| | |
|---|---|
| **Minecraft** | 1.20, 1.20.x, 1.21, 1.21.x, 26.1.x |
| **Server** | Paper, Spigot, Folia |
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