<div align="center">

# BlockProt — Plus

[![CI](https://img.shields.io/github/actions/workflow/status/VictorGugug/BlockProt-Plus/ci.yml?branch=main&style=flat-square&label=CI)](https://github.com/VictorGugug/BlockProt-Plus/actions)
[![Release](https://img.shields.io/github/v/release/VictorGugug/BlockProt-Plus?style=flat-square&color=brightgreen&label=Release)](https://github.com/VictorGugug/BlockProt-Plus/releases)
[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg?style=flat-square)](LICENSE)
[![Java](https://img.shields.io/badge/Java-25+-orange?style=flat-square)](https://openjdk.org/projects/jdk/25/)
[![Paper](https://img.shields.io/badge/Paper-1.21%2B%20%7C%2026.x-white?style=flat-square)](https://papermc.io/)

**Fork created and maintained by [Zar](https://github.com/VictorGugug)**

*Java 25 · Paper 26.x · MySQL index · per-world config · access audit · auto-backup*

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

## What is BlockProt Plus?

BlockProt lets players protect chests, furnaces, and many other blocks using a modern GUI —
no commands to memorize. This fork targets Paper/Spigot servers on Minecraft 1.21+ and the
new year-based 26.x version family.

> **This README is the authoritative feature reference.**
> The Modrinth and Hangar pages contain a summary and link here for the full detail,
> so this is the only file that needs updating when new features are added.

---

## Installing

### Pre-built JAR

Download the latest JAR from [Releases](https://github.com/VictorGugug/BlockProt-Plus/releases)
and drop it in your `plugins/` folder. Requires **Java 25** and Paper/Spigot 1.21+.

### Build from source

```bash
# Requires JDK 25
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

## Features added vs. upstream

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
- Does not spam the console; detailed information stays in the log file.

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

- `/bp friends` — opens the global default-friends GUI.
- `/bp friends addall <player>` — adds the target player to every block the executor owns
  and to their global default-friend list. Resolves offline players via Mojang API.
- Spanish aliases available when `localized_command_aliases: true`.
- Reports how many blocks were updated via an action bar message.

### 5. SQLite Access Audit Log

- Database: `plugins/BlockProt/blockprot_audit.sqlite`.
- Records `ACCESS_DENIED` and `ACCESS_GRANTED` events with player UUID, name, location, and timestamp.
- All writes are asynchronous (`CompletableFuture`).
- Automatic pruning when the table exceeds 50 000 entries.
- In-game GUI (`AuditInventory`) shows access history per block, with admin teleport button.

### 6. Automatic Backup & Safe Migration

- On startup, detects pre-existing plugin data and creates a ZIP backup under
  `plugins/BlockProt/backups/` before any migration step runs.
- Rotates backups; keeps a maximum of 10 ZIP files.
- `/bp reload` always triggers a forced backup first.
- Config file watcher also backs up before each auto-reload.

### 7. Inactivity Cleanup *(optional)*

- `inactivity_cleanup_days` in `config.yml` (default `-1` = disabled).
- When enabled, runs once at startup and removes protections from blocks owned by players who
  have not logged in for the configured number of days.
- Notifies online admins with a summary message.

### 8. Per-World Configuration (`worlds.yml`)

- Each world can have its own lists of lockable tile entities, blocks, shulker boxes, and doors.
- `enabled: false` disables all block protection in that world.
- Worlds not listed fall back to `config.yml` globals.
- Missing worlds are added automatically on startup (non-destructive).
- A broken `worlds.yml` is replaced with the bundled default; a timestamped backup is kept.

### 9. Config File Watcher

- Monitors `config.yml`, `worlds.yml`, and all `lang/*.yml` files for changes.
- Automatically reloads the plugin configuration when a change is detected.
- Debounced (2 s) to avoid duplicate reloads while an editor is still writing.

### 10. Hardened Security Options

| Config key | Default | Description |
|---|---|---|
| `protect_locked_blocks_from_explosions` | `true` | Prevents explosions from destroying NBT-protected blocks |
| `block_protected_block_piston_movement` | `true` | Prevents pistons from pushing or pulling NBT-protected blocks |
| `allow_break_protected_blocks` | `false` | Any player can break a protected block (for reinforcement-plugin servers) |
| `respect_spawn_protection` | `true` | Denies locking inside the server's spawn-protection radius; ops always bypass |
| `clear_protection_on_shulker_break` | `false` | Breaking your own shulker drops it without protection NBT so the recipient can re-lock it |

### 11. `/bp help` Command

- `/bp help` (also `/bp ayuda`) lists all available subcommands with a short description.

### 12. WorldEdit / FAWE Paste Auto-Lock *(optional)*

- Watches for `//paste` commands and after a configurable delay scans a bounded radius,
  automatically locking any unprotected lockable block near the paste origin.
- Applies the player's existing default-friend list to each newly locked block.
- Capped at `max_blocks_per_paste` to prevent server lag.
- Disabled by default (`worldedit_paste_autolock.enabled: false`).

### 13. Floodgate / Geyser Bedrock Support

- Resolves friend names with configurable Bedrock username prefixes so Bedrock players
  (connected via Floodgate/Geyser) can be added as friends without guessing the prefix.
- Configured via `bedrock_username_prefixes` in `config.yml`.

### 14. Automatic Config & Lang Key Merging — Self-Repair

- On every startup and `/bp reload`, the plugin compares disk files against the JAR bundle.
- Any key missing from disk is added with the default value; existing keys are never touched.
- If `worlds.yml` cannot be parsed, it is replaced with the bundled default and a timestamped
  broken-file copy is kept so the admin can recover their settings.

### 15. ClaimChunk Integration

- Prevents players from locking blocks inside chunks they do not own.
- Optional `restrict_access_to_chunk_owner`: only the chunk owner can access unprotected
  containers inside their claimed chunk.
- Friend filtering: only the chunk owner is offered as a friend candidate inside a claim.
- Activates automatically when ClaimChunk is present on the server.

### 16. `BlockProtLockEvent` & `BlockProtUnlockEvent`

- Two new cancellable Bukkit events fired before any lock or unlock operation.
- `Cause` enum: `MANUAL`, `LOCK_ON_PLACE`, `CLAIM_AUTO_LOCK`, `WORLDEDIT_PASTE`, `API`.
- Useful for economy plugins, quest systems, region managers, and custom audit tools.

### 17. Shulker Box Protection Improvements

- Bug fix for upstream issue #344: `NbtApiException` console spam when a shulker is placed
  or broken while its `TileEntity` is not yet initialised. Both `onBlockPlace` and
  `onShulkerBoxBreak` now guard with an `instanceof TileState` check before any NBT access.
- `clear_protection_on_shulker_break: true` drops the shulker without lock NBT so the
  recipient can open and re-lock it as their own.

### 18. Spawn Protection Respect

- Upstream issue #303: players cannot lock blocks inside the server's spawn-protection radius.
- Controlled by `respect_spawn_protection: true` in `config.yml`.
- Ops and `blockprot.admin` always bypass.

### 19. Allow Breaking Protected Blocks

- Upstream issue #324: `allow_break_protected_blocks: false` in `config.yml`.
- When enabled, any player can break a protected block regardless of ownership.
- Intended for servers using a separate reinforcement plugin for break resistance.

### 20. Update Checker — GitHub Releases

- Queries the **GitHub Releases API** for this fork instead of the upstream SpigotMC resource.
- Admins receive a clickable in-game message pointing to the releases page when a new version
  is available.
- Result is cached per session; the API is only called once per server start.

### 21. Auto-Publish to Modrinth & Hangar

- `.github/workflows/publish.yml` builds the shadow JAR and publishes it to **Modrinth** and
  **Hangar** automatically when a GitHub Release is published.
- Release channel (`release` / `beta` / `alpha`) is inferred from the version suffix.
- Requires `MODRINTH_TOKEN`, `MODRINTH_PROJECT_ID`, and `HANGAR_TOKEN` repository secrets.

---

## Commands

| Command | Description |
|---|---|
| `/bp help` | List all subcommands |
| `/bp settings` | Open player settings GUI |
| `/bp friends` | Open global friend list GUI |
| `/bp friends addall <player>` | Add player to every block you own |
| `/bp stats` | View your protection statistics |
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
| `blockprot.locklimit.<N>` | Set per-player lock limit (e.g. `.50`) | false |
| `blockprot.debug` | Execute debug commands | false |

---

## Integrations

| Plugin | Notes |
|---|---|
| **Towny** | Respects town and nation permissions |
| **WorldGuard** | Honors region flags |
| **PlaceholderAPI** | Exposes stats and protection status as placeholders |
| **Lands** | Supports Lands claim permission checks |
| **ClaimChunk** | Prevents locking in chunks you don't own |
| **Floodgate / Geyser** | Resolves Bedrock usernames with configurable prefixes |
| **WorldEdit / FAWE** | Optional paste auto-lock for `//paste` |

---

## Compatibility

| | |
|---|---|
| **Minecraft** | 1.21, 1.21.1, 1.21.4, 26.x |
| **Server** | Paper, Spigot |
| **Java** | 25+ required |
| **MySQL** | MySQL 8+, MariaDB 10.5+ (optional) |
| **Languages** | EN, ES, DE, FR, IT, PT-BR, RU, JA, KO, ZH-CN, ZH-TW, CS, SK, PL, TR |

---

## Configuration Reference

```yaml
# ── Core ──────────────────────────────────────────────────────────────────
worlds_config_enabled: false
localized_command_aliases: true
lock_on_place_by_default: true
public_is_friend_by_default: false
player_max_locked_block_count: -1
lock_hint_cooldown_in_seconds: 10
friend_search_similarity: 0.5
disable_friend_functionality: false
notify_op_of_updates: false
redstone_disallowed_by_default: false

# ── Security hardening ────────────────────────────────────────────────────
protect_locked_blocks_from_explosions: true
block_protected_block_piston_movement: true
allow_break_protected_blocks: false
respect_spawn_protection: true
clear_protection_on_shulker_break: false

# ── Optional MySQL index ──────────────────────────────────────────────────
mysql:
  enabled: false
  host: "127.0.0.1"
  port: 3306
  database: "blockprot"
  username: "blockprot"
  password: ""
  jdbc_url: ""           # Overrides host/port/database when set
  pool:
    maximum_pool_size: 10
    minimum_idle: 2
    connection_timeout_ms: 10000

# ── WorldEdit paste auto-lock ─────────────────────────────────────────────
worldedit_paste_autolock:
  enabled: false
  radius: 24
  max_blocks_per_paste: 5000
  delay_ticks: 20

# ── Floodgate / Geyser ───────────────────────────────────────────────────
bedrock_username_prefixes: [".", "*", "_"]

# ── Inactivity cleanup ────────────────────────────────────────────────────
inactivity_cleanup_days: -1   # -1 = disabled

# ── Optional feature preset ───────────────────────────────────────────────
optional_features_enable_all: false
```

---

## New files added vs. upstream

| File | Description |
|------|-------------|
| `BlockProtLogger.java` | Persistent session log writer |
| `VersionCompat.java` | Runtime version detection (1.x and 26.x) |
| `VersionValidator.java` | Startup compatibility checks |
| `audit/AuditLogger.java` | SQLite access audit log |
| `commands/FriendsAddAllCommand.java` | `/bp friends addall` |
| `commands/HelpCommand.java` | `/bp help` |
| `config/WorldsConfig.java` | Per-world lockable-block configuration |
| `events/BlockProtLockEvent.java` | Cancellable lock event with cause |
| `events/BlockProtUnlockEvent.java` | Cancellable unlock event with cause |
| `integrations/ClaimChunkIntegration.java` | ClaimChunk support |
| `inventories/AuditInventory.java` | In-game audit log viewer |
| `inventories/ChatInput.java` | Chat-based player input helper |
| `inventories/AnvilInput.java` | Anvil GUI input fallback |
| `listeners/WorldEditPasteListener.java` | WorldEdit/FAWE paste auto-lock |
| `storage/HybridDatabase.java` | MySQL/MariaDB index with HikariCP |
| `tasks/BackupTask.java` | Pre-operation ZIP backup |
| `tasks/ConfigFileWatcher.java` | Auto-reload on config file change |
| `tasks/InactivityCleanupTask.java` | Remove protections of inactive players |
| `util/PlayerNameResolver.java` | Offline player UUID resolution via Mojang API |
| `util/TemporaryActionBar.java` | Repeating action bar message utility |

---

## Dependencies added by BlockProt Plus

| Library | Version | Notes |
|---------|---------|-------|
| HikariCP | 7.0.2 | Shaded to `de.sean.blockprot.bukkit.shaded.hikari` |
| MySQL Connector/J | 9.7.0 | Bundled in shadow JAR |
| slf4j-api | runtime | Required by HikariCP |

---

## Translating

Language files are in `spigot/src/main/resources/lang/`. Contributions welcome.

All message values support Minecraft legacy color and formatting codes (`§a`, `§b`, `§l`, etc.).

---

## Developing addons

The fork exposes the same `BlockProtAPI` as the upstream plugin:

```java
BlockNBTHandler handler = BlockProtAPI.getInstance().getBlockHandler(block);
PlayerSettingsHandler playerHandler = BlockProtAPI.getInstance().getPlayerSettings(player);
```

Internal classes (`HybridDatabase`, `AuditLogger`, `WorldsConfig`, etc.) are not part of the public API.

---

## Build verification

```powershell
.\gradlew.bat :blockprot-spigot:compileJava
.\gradlew.bat :blockprot-spigot:shadowJar
.\gradlew.bat :blockprot-spigot:build
```

Output JAR: `spigot/build/libs/BlockProt-VERSION.jar`

| `blockProtVersion` | `versionSuffix` | Output JAR |
|---|---|---|
| `1.2.9` | *(blank)* | `BlockProt-1.2.9.jar` |
| `1.2.9` | `SNAPSHOT` | `BlockProt-1.2.9-SNAPSHOT.jar` |
| `1.2.9` | `beta.1` | `BlockProt-1.2.9-beta.1.jar` |
| `1.2.9` | `fix.1` | `BlockProt-1.2.9-fix.1.jar` |

---

## Contact / Support

This fork is created and maintained by **Zar**.
[Open an issue](https://github.com/VictorGugug/BlockProt-Plus/issues) for bugs or questions.

---

## License

Licensed under the **GNU General Public License v3**. See [`LICENSE`](LICENSE) for details.

---

<sub>Based on <a href="https://github.com/spnda/BlockProt">BlockProt</a> by spnda — original copyright notices are preserved in each source file as required by GPL v3.</sub>
