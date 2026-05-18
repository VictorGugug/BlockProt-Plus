<div align="center">

# BlockProt — SP26-ZV

[![CI](https://img.shields.io/github/actions/workflow/status/VictorGugug/BlockProt-SP26-ZV/ci.yml?branch=main&style=flat-square&label=CI)](https://github.com/VictorGugug/BlockProt-SP26-ZV/actions)
[![Release](https://img.shields.io/github/v/release/VictorGugug/BlockProt-SP26-ZV?style=flat-square&color=brightgreen&label=Release)](https://github.com/VictorGugug/BlockProt-SP26-ZV/releases)
[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg?style=flat-square)](LICENSE)
[![Java](https://img.shields.io/badge/Java-25-orange?style=flat-square)](https://openjdk.org/projects/jdk/25/)
[![Paper](https://img.shields.io/badge/Paper-1.21%2B%20%7C%2026.x-white?style=flat-square)](https://papermc.io/)

**Fork created and maintained by [Zar](https://github.com/VictorGugug)**

*Java 25 · Paper 26.x · MySQL index · per-world config · access audit · auto-backup*

</div>

---

> Block protection plugin for Paper/Spigot servers.  
> Players lock chests, furnaces, and other blocks through a modern GUI — no commands to memorize.  
> This fork extends the original NBT core with production-grade features for large or long-running servers.

![Main menu](https://raw.githubusercontent.com/VictorGugug/BlockProt-SP26-ZV/main/images/main_menu.png)
![Friend settings](https://raw.githubusercontent.com/VictorGugug/BlockProt-SP26-ZV/main/images/friend_settings.png)
![Player settings](https://raw.githubusercontent.com/VictorGugug/BlockProt-SP26-ZV/main/images/user_settings.png)
![Redstone settings](https://raw.githubusercontent.com/VictorGugug/BlockProt-SP26-ZV/main/images/redstone_settings.png)

---

## What is BlockProt?

BlockProt lets players protect chests, furnaces, and many other blocks using a modern GUI —
no commands to memorize. This fork targets Paper/Spigot servers on Minecraft 1.21+ and the
new year-based 26.x version family.

---

## Installing

### Pre-built JAR

Download the latest JAR from [Releases](https://github.com/VictorGugug/BlockProt-SP26-ZV/releases)
and drop it in your `plugins/` folder. Requires **Java 25** and Paper/Spigot 1.21+.

### Build from source

```bash
# Requires JDK 25
git clone https://github.com/VictorGugug/BlockProt-SP26-ZV.git
cd BlockProt-SP26-ZV
./gradlew :blockprot-spigot:shadowJar
# Output → spigot/build/libs/BlockProt-VERSION.jar
```

```powershell
# Windows
.\gradlew.bat :blockprot-spigot:shadowJar
```

---

## SP26-ZV Features — by Zar

All additions are **disabled by default** in `config.yml`. The upstream NBT protection core
works exactly as in the original — nothing changes unless you opt in.

### 1. Java 25 / Paper 26.x Compatibility

- Compiles and runs on Java 25 (class file `69.0`).
- Detects both the classic `1.x` and the new year-based `26.x` version scheme at runtime.
- Validates Java version, Paper availability, and typed inventory view support on startup.
- Logs a one-line diagnostic to console and the session log file.

### 2. Persistent Session Logging

- Creates one log file per server session under `plugins/BlockProt/logs/`.
- Every line is timestamped. Logs plugin version, server version, and compatibility check results.
- Logs `PASS`, `FAIL`, or `WARN` for each startup check.
- Does **not** spam the console; detailed information stays in the log file.

### 3. Hybrid MySQL / NBT Backend *(optional)*

NBT remains the source of truth for per-block ownership and friend lists. MySQL/MariaDB is
used as an optional index for fast lookups, auditing, and cross-server global trust.

- Connection pool via HikariCP (shaded to avoid conflicts).
- MySQL Connector/J bundled inside the shadow JAR.
- Tables: `blockprot_block_index`, `blockprot_global_trust`.
- All SQL operations are asynchronous.
- In-memory cache for global trust, loaded at startup.
- Disabled by default (`mysql.enabled: false`).

### 4. Master Friend List & `/bp friends addall`

- `/bp friends` - opens the global default-friends GUI.
- `/bp friends addall <player>` - adds the target player to **every** block the executor owns,
  and to their global default-friend list. Resolves offline players via Mojang API.
- When `localized_command_aliases: true` and Spanish is selected, `/bp amigos` and
  `/bp amigos agregartodos <player>` are also accepted. Canonical English commands
  always continue to work.
- Reports how many blocks were updated via an action bar message.

### 5. SQLite Access Audit Log

- Database: `plugins/BlockProt/blockprot_audit.sqlite`.
- Records `ACCESS_DENIED` and `ACCESS_GRANTED` events with player UUID, name, location, and timestamp.
- All writes are asynchronous (`CompletableFuture`).
- Indexed by location, player, and timestamp.
- Automatic pruning when the table exceeds 50 000 entries.
- In-game GUI (`AuditInventory`) shows access history per block.
- Admin teleport button inside the audit GUI.

### 6. Automatic Backup & Safe Migration

- On startup, detects pre-existing plugin data (`data.yml`, `blockprot.db`, etc.).
- If prior data is found, creates a ZIP backup under `plugins/BlockProt/backups/` **before** any
  migration step runs.
- Rotates backups; keeps a maximum of 10 ZIP files.
- `/bp reload` always triggers a forced backup first.
- Does not back up session log files or the `backups/` folder itself.

### 7. Inactivity Cleanup *(optional)*

- Configurable via `inactivity_cleanup_days` in `config.yml` (default `-1` = disabled).
- When enabled, runs once at startup and removes protections from blocks owned by players who
  have not logged in for the configured number of days.
- Notifies online admins with a summary message.

### 8. Per-World Configuration (`worlds.yml`)

- Each world can have its own lists of lockable tile entities, blocks, shulker boxes, and doors.
- `enabled: false` disables all block protection in that world.
- Worlds not listed in `worlds.yml` fall back to `config.yml` globals.
- At startup, missing worlds are added automatically with `enabled: true` and lists inherited
  from `config.yml` (non-destructive; existing entries are never overwritten).
- A broken `worlds.yml` is replaced with the bundled default and a warning is logged.

### 9. Config File Watcher

- Monitors `config.yml`, `worlds.yml`, and all `lang/*.yml` files for changes.
- Automatically reloads the plugin configuration when a change is detected.
- Debounced (2 s) to avoid duplicate reloads while an editor is still writing.

### 10. Hardened Security Options

| Config key | Default | Description |
|---|---|---|
| `protect_locked_blocks_from_explosions` | `true` | Prevents explosions from destroying NBT-protected blocks |
| `block_protected_block_piston_movement` | `true` | Prevents pistons from pushing or pulling NBT-protected blocks |

### 11. `/bp help` Command

- `/bp help` (also `/bp ayuda`) lists all available subcommands with a short description.

### 12. WorldEdit / FAWE Paste Auto-Lock *(optional)*

- Watches for `//paste` commands issued by players.
- After a configurable delay (`delay_ticks`, default 20), scans a bounded radius around the
  paste origin and automatically applies NBT protection to any unprotected lockable block found.
- Applies the player's existing default-friend list to each newly locked block.
- Capped at `max_blocks_per_paste` to prevent server lag on large pastes.
- All activity is recorded to the session log; nothing is printed to console.
- Disabled by default (`worldedit_paste_autolock.enabled: false`).

### 13. Floodgate / Geyser Bedrock Support

- Resolves friend names with configurable Bedrock username prefixes so that Bedrock players
  (connected via Floodgate/Geyser) can be added as friends without guessing the server prefix.
- Prefixes are listed under `bedrock_username_prefixes` in `config.yml`.
- Defaults cover the most common prefix characters (`.`, `*`, `_`).

### 14. Automatic Config & Lang Key Merging — Self-Repair

The plugin detects and repairs broken or outdated configuration files automatically, without ever destroying administrator settings.

**Key merge (`config.yml` and `lang/*.yml`):**
- On every startup and `/bp reload`, the plugin compares the disk copy against the version bundled in the JAR.
- Any key present in the JAR but missing from disk is added with the default value. Keys that already exist are never touched.
- Merge counts are logged to console; per-key detail goes only to the session log.

**`worlds.yml` syntax repair:**
- If `worlds.yml` cannot be parsed (YAML syntax error, truncated file, etc.), BlockProt replaces it with the bundled default so the server can start cleanly.
- Before overwriting, the broken file is copied next to itself with a timestamp suffix, e.g. `worlds.yml.2026-05-16_14-30-broken`, so the administrator can inspect and recover the original settings.
- If the copy itself fails (disk full, permissions, etc.), a warning is printed to console and the repair proceeds anyway — the server start is never blocked by the backup step.
- The session log always records the full path of the backup file or the reason the backup failed.

**Backup guarantee before any reload:**
- `/bp reload` runs a full ZIP backup of the plugin data folder synchronously before calling `reloadConfigAndTranslations()`. The backup completes before any file can be repaired or overwritten.
- The config file watcher also triggers a synchronous backup before each auto-reload, so files saved with syntax errors from a live editor are always captured first.

### 15. New Configuration Keys

```yaml
# ── Per-world protection ──────────────────────────────────────────────────
worlds_config_enabled: false   # Enable worlds.yml per-world overrides

# ── Localized command aliases ─────────────────────────────────────────────
localized_command_aliases: true

# ── Quick feature toggle ─────────────────────────────────────────────────
optional_features_enable_all: false  # Enables safe optional modules; does NOT enable MySQL

# ── MySQL hybrid index ─────────────────────────────────────────────────────
mysql:
  enabled: false
  host: "127.0.0.1"
  port: 3306
  database: "blockprot"
  username: "blockprot"
  password: ""
  jdbc_url: ""          # Overrides host/port/database when set
  pool:
    maximum_pool_size: 10
    minimum_idle: 2
    connection_timeout_ms: 10000

# ── Security hardening ────────────────────────────────────────────────────
protect_locked_blocks_from_explosions: true
block_protected_block_piston_movement: true

# ── WorldEdit paste auto-lock ─────────────────────────────────────────────
worldedit_paste_autolock:
  enabled: false
  radius: 24
  max_blocks_per_paste: 5000
  delay_ticks: 20

# ── Floodgate / Geyser Bedrock username prefixes ──────────────────────────
bedrock_username_prefixes:
  - "."
  - "*"
  - "_"

# ── Inactivity cleanup ────────────────────────────────────────────────────
inactivity_cleanup_days: -1   # -1 = disabled
```

### 16. New Files Added vs. Upstream

| File | Description |
|------|-------------|
| `BlockProtLogger.java` | Persistent session log writer |
| `VersionCompat.java` | Runtime version detection (1.x and 26.x) |
| `VersionValidator.java` | Startup compatibility checks |
| `audit/AuditLogger.java` | SQLite access audit log |
| `commands/FriendsAddAllCommand.java` | `/bp friends addall` |
| `commands/HelpCommand.java` | `/bp help` |
| `config/WorldsConfig.java` | Per-world lockable-block configuration |
| `inventories/AuditInventory.java` | In-game audit log viewer |
| `inventories/ChatInput.java` | Chat-based player input helper |
| `inventories/AnvilInput.java` | Anvil GUI input fallback |
| `listeners/WorldEditPasteListener.java` | WorldEdit/FAWE paste auto-lock |
| `storage/HybridDatabase.java` | MySQL/MariaDB index with HikariCP |
| `tasks/BackupTask.java` | Pre-operation ZIP backup |
| `tasks/ConfigFileWatcher.java` | Auto-reload on config file change |
| `tasks/InactivityCleanupTask.java` | Remove protections of inactive players |

---

## Translating

If you know a language that is not yet supported, or you found a translation error, contributions
are welcome. Language files are in `spigot/src/main/resources/lang/`.

---

## Contact / Support

This fork is created and maintained by **Zar**. For bugs or questions,
[open an issue](https://github.com/VictorGugug/BlockProt-SP26-ZV/issues) in this repository.

---

## Developing Addons

The fork exposes the same `BlockProtAPI` as the upstream plugin:

```java
BlockNBTHandler handler = BlockProtAPI.getInstance().getBlockHandler(block);
PlayerSettingsHandler playerHandler = BlockProtAPI.getInstance().getPlayerSettings(player);
```

SP26-ZV classes (`HybridDatabase`, `AuditLogger`, `WorldsConfig`, etc.) are internal and
not part of the public API.

---

## Dependencies Added by SP26-ZV

| Library | Version | Notes |
|---------|---------|-------|
| HikariCP | 7.0.2 | Shaded to `de.sean.blockprot.bukkit.shaded.hikari` |
| MySQL Connector/J | 9.7.0 | Bundled in shadow JAR |
| slf4j-api | runtime | Required by HikariCP |

---

## Build Verification

```powershell
.\gradlew.bat :blockprot-spigot:compileJava
.\gradlew.bat :blockprot-spigot:shadowJar
.\gradlew.bat :blockprot-spigot:build
```

Output JAR: `spigot/build/libs/BlockProt-1.2.9.jar`

### Version naming

The version is controlled by two properties in `gradle.properties`:

| Property | Purpose |
|---|---|
| `blockProtVersion` | Base version number, e.g. `1.2.9` |
| `versionSuffix` | Optional qualifier appended with a dash. Leave blank for a release build. |

Examples:

| `blockProtVersion` | `versionSuffix` | Output JAR |
|---|---|---|
| `1.2.9` | *(blank)* | `BlockProt-1.2.9.jar` |
| `1.2.9` | `SNAPSHOT` | `BlockProt-1.2.9-SNAPSHOT.jar` |
| `1.2.9` | `beta.1` | `BlockProt-1.2.9-beta.1.jar` |
| `1.2.9` | `fix.1` | `BlockProt-1.2.9-fix.1.jar` |

The suffix can also be passed on the command line without editing the file:

```bash
./gradlew :blockprot-spigot:shadowJar -PversionSuffix=SNAPSHOT
```

Builds from non-`main` branches append the branch name automatically.

---

## License

This project is licensed under the **GNU General Public License v3**. See [`LICENSE`](LICENSE) for details.

---

<sub>Based on <a href="https://github.com/spnda/BlockProt">BlockProt</a> by spnda — original copyright notices are preserved in each source file as required by GPL v3.</sub>
