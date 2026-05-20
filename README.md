<div align="center">

# BlockProt — SP26-ZV

[![CI](https://img.shields.io/github/actions/workflow/status/VictorGugug/BlockProt-Plus/ci.yml?branch=main&style=flat-square&label=CI)](https://github.com/VictorGugug/BlockProt-Plus/actions)
[![Release](https://img.shields.io/github/v/release/VictorGugug/BlockProt-Plus?style=flat-square&color=brightgreen&label=Release)](https://github.com/VictorGugug/BlockProt-Plus/releases)
[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg?style=flat-square)](LICENSE)
[![Java](https://img.shields.io/badge/Java-25+-orange?style=flat-square)](https://openjdk.org/projects/jdk/25/)
[![Paper](https://img.shields.io/badge/Paper-1.21%2B%20%7C%2026.x-white?style=flat-square)](https://papermc.io/)

**Fork created and maintained by [Zar](https://github.com/VictorGugug)**

*Java 25 · Paper 26.x · MySQL index · per-world config · access audit · pet protection · auto-backup*

</div>

---

> Block protection plugin for Paper/Spigot servers.
> Players lock chests, furnaces, and other blocks through a modern GUI — no commands to memorize.
> This fork extends the original NBT core with production-grade features for large or long-running servers.

![Main menu](https://raw.githubusercontent.com/VictorGugug/BlockProt-Plus/main/images/main_menu.png)
![Friend settings](https://raw.githubusercontent.com/VictorGugug/BlockProt-Plus/main/images/friend_settings.png)
![Player settings](https://raw.githubusercontent.com/VictorGugug/BlockProt-Plus/main/images/user_settings.png)
![Redstone settings](https://raw.githubusercontent.com/VictorGugug/BlockProt-Plus/main/images/redstone_settings.png)

---

## Installing

### Pre-built JAR

Download the latest JAR from [Releases](https://github.com/VictorGugug/BlockProt-Plus/releases)
and drop it in your `plugins/` folder. Requires **Java 25** and **Paper/Spigot 1.21+**.

### Build from source

```bash
git clone https://github.com/VictorGugug/BlockProt-Plus.git
cd BlockProt-Plus
./gradlew :blockprot-spigot:shadowJar
# Output → spigot/build/libs/BlockProt-VERSION.jar
```

```powershell
# Windows
.\gradlew.bat :blockprot-spigot:shadowJar
```

---

## File layout

After the first startup, the plugin data folder looks like this:

```
plugins/BlockProt/
├── config.yml          ← Main configuration (clean, sectioned, no legacy keys)
├── blocks.yml          ← Lockable block lists (generated/migrated on first start)
├── worlds.yml          ← Per-world overrides (optional)
├── mysql/
│   ├── mysql.yml           ← MySQL/Storage configuration
│   ├── blockprot_audit.sqlite  ← SQLite access audit log
│   └── blockprot_audit.sqlite-wal / -shm
├── lang/
│   └── translations_*.yml
├── logs/
│   └── session-YYYY-MM-DD.log
└── backups/
    └── *.zip
```

**`blocks.yml`** is generated automatically on first start. If your old `config.yml` already
had `lockable_tile_entities` / `lockable_blocks` / etc., those values are migrated into
`blocks.yml` and removed from `config.yml` automatically, preserving your configuration.

**`mysql/mysql.yml`** holds all database settings. The audit SQLite file lives there too so
all database-related files are in one place. If the old `blockprot_audit.sqlite` exists in the
plugin root, it is moved to `mysql/` automatically on the next startup.

**`config.yml`** is rewritten on every startup using the bundled template as a base, with your
existing values applied on top. This keeps the file clean, sectioned, and free of legacy or
obsolete keys (`mysql.*`, `console.*`, `lockable_*`) regardless of what was there before.

---

## Features

### Core block protection (upstream)

- Sneak + right-click any lockable block to open the protection GUI.
- Add friends with individual **Read / Write / Manager** permission levels.
- Redstone, hopper, and piston protection toggles per block.
- Copy / paste protection settings between blocks.
- Block name display and inspect-contents shortcut.
- Per-player default friend list applied automatically to new locks.

---

### SP26-ZV additions

All additions are **disabled by default** unless noted. The upstream NBT core is unchanged.

#### 1. Java 25 / Paper 26.x Compatibility

- Compiles and runs on Java 25 (class file `69.0`).
- Detects both `1.x` and year-based `26.x` version schemes at runtime.
- Validates Java version, Paper availability, and typed inventory view support on startup.

#### 2. Persistent Session Logging

- One log file per server session under `plugins/BlockProt/logs/`.
- Every line is timestamped. Logs plugin version, server version, and compatibility results.
- Detailed information stays in the log; console stays clean.

#### 3. Hybrid MySQL / NBT Backend *(optional)*

NBT remains the source of truth. MySQL/MariaDB is an optional index for fast lookups and auditing.

- Connection pool via HikariCP (shaded).
- Tables: `blockprot_block_index`, `blockprot_global_trust`.
- All SQL is asynchronous with an in-memory trust cache.
- Configuration: `mysql/mysql.yml` → `mysql.enabled: false`.

#### 4. Separate Block Definitions (`blocks.yml`)

- Lockable block lists live in `blocks.yml`, not `config.yml`.
- Generated on first start with sensible defaults.
- Old lists in `config.yml` are migrated automatically and removed.
- Edit `blocks.yml` and run `/bp reload` — no restart needed.

#### 5. Master Friend List & `/bp friends addall`

- `/bp friends` — opens the global default-friends GUI.
- `/bp friends addall <player>` — adds a player to every block you own at once.
- Resolves offline players via Mojang API.

#### 6. SQLite Access Audit Log

![Audit log screenshot](https://raw.githubusercontent.com/VictorGugug/BlockProt-Plus/main/images/audit-log.png)

- Stored at `mysql/blockprot_audit.sqlite`.
- Records `ACCESS_DENIED` / `ACCESS_GRANTED` / `OPENED` / `ITEM_TAKEN` / `ITEM_PLACED`.
- All writes asynchronous; automatic pruning at 50 000 entries.
- In-game GUI groups events by player with heads, timestamps, and coordinates.
- Old file at plugin root is migrated to `mysql/` automatically.

#### 7. Automatic Backup

- ZIP backup under `plugins/BlockProt/backups/` before any migration.
- Backup filenames include version and timestamp.
- `/bp reload` triggers a backup first.

#### 8. Inactivity Cleanup *(optional)*

- `inactivity_cleanup_days: -1` (disabled) in `config.yml`.
- Removes protections from blocks owned by long-inactive players on startup.

#### 9. Per-World Configuration (`worlds.yml`)

- Each world can override lockable block lists and enable/disable protection entirely.
- Missing worlds added automatically; broken file replaced with a backup kept.

#### 10. Config File Watcher

- Watches `config.yml`, `worlds.yml`, `blocks.yml`, `mysql/mysql.yml`, and `lang/*.yml`.
- Auto-reloads on change, debounced 2 s.

#### 11. Security Options

| Key | Default | Description |
|---|---|---|
| `protect_locked_blocks_from_explosions` | `true` | Explosions cannot destroy locked blocks |
| `block_protected_block_piston_movement` | `true` | Pistons cannot move locked blocks |
| `allow_break_protected_blocks` | `false` | Any player can break (for reinforcement servers) |
| `respect_spawn_protection` | `true` | No locking inside spawn-protection radius |
| `clear_protection_on_shulker_break` | `false` | Shulker drops without lock NBT |

#### 12. WorldEdit / FAWE Paste Auto-Lock *(optional)*

- Auto-locks unprotected blocks near a `//paste` origin.
- Applies your default-friend list to each new lock.
- `worldedit_paste_autolock.enabled: false` by default.

#### 13. Floodgate / Geyser Bedrock Support

- Configurable `bedrock_username_prefixes` for Bedrock player name resolution.

#### 14. Self-Repair: Config & Lang Key Merging

- Missing keys added from JAR defaults on every startup and `/bp reload`.
- `config.yml` is rewritten from the clean template with your values preserved.
- Obsolete keys (`mysql.*`, `console.*`, `lockable_*`) removed automatically.

#### 15. Colored Particle Effects & Sounds

- Lock → green dust ring + sound. Unlock → red dust ring + sound.
- Redstone / hopper / piston toggles → color-transition rings.
- `block_lock_effects` and `block_lock_sounds` toggles in `config.yml`.

#### 16. Skin Resolution for Offline-Mode Servers

- Async Mojang fallback for cracked/offline servers.
- SkinRestorer takes priority when installed.

#### 17. Pet Protection *(SP26-ZV, default: disabled)*

Protects tamed animals (wolves, cats, parrots, horses, llamas …) using the same ownership
model as blocks. Data stored in `PersistentDataContainer`.

| Toggle | Description |
|---|---|
| `enabled` | Master switch (default **false**) |
| `auto_protect_on_tame` | Protect automatically when tamed |
| `no_damage` | Block other players from damaging the pet |
| `no_interact` | Block right-click (feeding, naming, sitting) |
| `no_leash` | Block leash/unleash by others |
| `no_pickup` | Block parrot shoulder-pickup by others |

- Right-click your pet while holding the configured `menu_item` (default: Stick) to open the settings GUI.
- Owner, `blockprot.admin`, and `blockprot.bypass` always bypass.
- Denial message comes from the lang file (`messages.pet_denied`).
- Hot-reloadable via `/bp reload`.

#### 18. Update Checker

- Queries GitHub Releases API once per session asynchronously.
- Console warning + optional op join message when outdated.
- `/bp update` re-runs the check manually.

#### 19. `BlockProtLockEvent` & `BlockProtUnlockEvent`

- Cancellable Bukkit events with a `Cause` enum (`MANUAL`, `LOCK_ON_PLACE`, `WORLDEDIT_PASTE`, …).

#### 20. Admin Teleport from Statistics

- Admins (`blockprot.admin`) can click any block entry in **Stats → Your Blocks** to teleport to it.

---

## Commands

| Command | Description |
|---|---|
| `/bp help` | List all subcommands |
| `/bp settings` | Open player settings GUI |
| `/bp friends` | Open global friend list GUI |
| `/bp friends addall <player>` | Add player to every block you own |
| `/bp stats` | View protection statistics |
| `/bp about` | Plugin info and version |
| `/bp reload` | Reload config (backup runs first) |
| `/bp integrations` | Show active integrations |
| `/bp disablehints` | Disable new-player hints |
| `/bp update` | Check for updates manually |

Alias: `/blockprot`. Spanish aliases available when `localized_command_aliases: true`.

---

## Permissions

| Permission | Description | Default |
|---|---|---|
| `blockprot.lock` | Lock blocks | true |
| `blockprot.info` | View info on any locked block | op |
| `blockprot.admin` | Unlock and edit blocks owned by others | false |
| `blockprot.bypass` | Bypass all protection | false |
| `blockprot.lockmax` | Remove per-player block lock limit | false |
| `blockprot.locklimit.<N>` | Per-player lock limit override | false |
| `blockprot.debug` | Developer diagnostics | false |

---

## Integrations

| Plugin | Notes |
|---|---|
| **Towny** | Respects town and nation permissions |
| **WorldGuard** | Honors region flags |
| **PlaceholderAPI** | Exposes stats and protection status as placeholders |
| **Lands** | Supports Lands claim permission checks |
| **ClaimChunk** | Prevents locking in chunks you don't own |
| **Floodgate / Geyser** | Resolves Bedrock usernames |
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

## Configuration reference

### `config.yml`

```yaml
# ── 1. General ───────────────────────────────────────────────────────────────
language_file: translations_en.yml
fallback_string: "Unknown translation"
replace_translations: true
notify_op_of_updates: false
localized_command_aliases: true
excluded_worlds: []
worlds_config_enabled: false
bedrock_username_prefixes: [".", "*", "_"]
inactivity_cleanup_days: -1   # -1 = disabled

# ── 2. Player & Protection Defaults ──────────────────────────────────────────
lock_on_place_by_default: true
public_is_friend_by_default: false
player_max_locked_block_count: -1
lock_hint_cooldown_in_seconds: 10
friend_search_similarity: 0.5
disable_friend_functionality: false
redstone_disallowed_by_default: false

# ── 3. Safety & Protection Behavior ──────────────────────────────────────────
protect_locked_blocks_from_explosions: true
block_protected_block_piston_movement: true
clear_protection_on_shulker_break: false
allow_break_protected_blocks: false
respect_spawn_protection: true

# ── 4. Pet Protection (SP26-ZV) ───────────────────────────────────────────────
pet_protection:
  enabled: false
  auto_protect_on_tame: true
  menu_item: STICK

# ── 5. Effects ────────────────────────────────────────────────────────────────
block_lock_effects: true
block_lock_sounds: true

# ── 6. Optional Features ──────────────────────────────────────────────────────
optional_features_enable_all: false
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
  jdbc_url: ""   # overrides host/port/database when set
  pool:
    maximum_pool_size: 10
    minimum_idle: 2
    connection_timeout_ms: 10000
```

### `blocks.yml`

```yaml
lockable_tile_entities: [CHEST, TRAPPED_CHEST, FURNACE, ...]
lockable_shulker_boxes: [SHULKER_BOX, WHITE_SHULKER_BOX, ...]
lockable_blocks: [ANVIL, OAK_FENCE_GATE, OAK_TRAPDOOR, ...]
lockable_doors: [OAK_DOOR, IRON_DOOR, COPPER_DOOR, ...]
```

---

## Build

```powershell
.\gradlew.bat :blockprot-spigot:shadowJar
.\gradlew.bat :blockprot-spigot:shadowJar -PversionSuffix=SNAPSHOT-1
```

Output: `spigot/build/libs/BlockProt-VERSION.jar`

| `blockProtVersion` | `versionSuffix` | Output |
|---|---|---|
| `1.3.0` | *(blank)* | `BlockProt-1.3.0.jar` |
| `1.3.0` | `SNAPSHOT-1` | `BlockProt-1.3.0-SNAPSHOT-1.jar` |
| `1.3.0` | `beta.1` | `BlockProt-1.3.0-beta.1.jar` |
| `1.3.0` | `rc.1` | `BlockProt-1.3.0-rc.1.jar` |

---

## Translating

Language files in `spigot/src/main/resources/lang/`. All message values support legacy color codes (`§a`, `§l`, etc.). Contributions welcome.

---

## Contact / Support

Maintained by **Zar**.
[Open an issue](https://github.com/VictorGugug/BlockProt-Plus/issues) for bugs or questions.

---

## License

Licensed under the **GNU General Public License v3**. See [`LICENSE`](LICENSE) for details.

---

<sub>Based on <a href="https://github.com/spnda/BlockProt">BlockProt</a> by spnda — original copyright notices are preserved in each source file as required by GPL v3.</sub>
