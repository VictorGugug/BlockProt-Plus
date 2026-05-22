<div align="center">

# BlockProt Reloaded

[![CI](https://img.shields.io/github/actions/workflow/status/VictorGugug/BlockProt-Reloaded/ci.yml?branch=main&style=flat-square&label=CI)](https://github.com/VictorGugug/BlockProt-Reloaded/actions)
[![Release](https://img.shields.io/github/v/release/VictorGugug/BlockProt-Reloaded?style=flat-square&color=brightgreen&label=Release)](https://github.com/VictorGugug/BlockProt-Reloaded/releases)
[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg?style=flat-square)](LICENSE)
[![Java](https://img.shields.io/badge/Java-25+-orange?style=flat-square)](https://openjdk.org/projects/jdk/25/)
[![Paper](https://img.shields.io/badge/Paper-1.21%2B%20%7C%2026.x-white?style=flat-square)](https://papermc.io/)

**Fork created and maintained by [Zar](https://github.com/VictorGugug)**

*Java 25 · Paper 26.x · MySQL index · per-world config · access audit · pet protection · auto-backup · ownership transfer · timed access · remote block access · statistics TP*

</div>

---

> Block protection plugin for Paper/Spigot servers.
> Players lock chests, furnaces, and other blocks through a modern GUI — no commands to memorize.
> This fork extends the original NBT core with production-grade features for large or long-running servers.

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
# Output → spigot/build/libs/BlockProt-VERSION.jar
```

```powershell
# Windows
.\gradlew.bat :blockprot-spigot:shadowJar
```

**SNAPSHOT builds** auto-increment a counter (1–5) by scanning the existing JARs in
`spigot/build/libs/` — deleting JARs never causes a number to be reused.

---

## File layout

```
plugins/BlockProt/
├── config.yml              ← Main configuration
├── blocks.yml              ← Lockable block lists (generated on first start)
├── worlds.yml              ← Per-world overrides (optional)
├── mysql/
│   ├── mysql.yml           ← MySQL/Storage configuration
│   └── blockprot_audit.sqlite
├── lang/
│   └── translations_*.yml
├── logs/
│   └── session-YYYY-MM-DD.log
└── backups/
    └── *.zip
```

---

## Features

### Core block protection

- Sneak + right-click any lockable block to open the protection GUI.
- Add friends with **Read / Write / Manager** permission levels.
- Redstone, hopper, and piston protection toggles per block.
- Copy / paste protection settings between blocks (action bar confirmation).
- Block name display.

### GUI overview

#### User menu (`/bp user`)

| Button | Action |
|---|---|
| My Settings | Lock-on-place toggle + hints toggle + friends |
| Friends | Global default friend list |
| Statistics | Your protected blocks with actions |
| About | Plugin info |

**Hints toggle** is inside *My Settings* — enable or disable block-placement hints there.

#### Block lock inventory (right-click a locked block)

Bottom row shortcuts (always visible):

| Slot | Item | Condition |
|---|---|---|
| 16 | User Menu (writable book) | Always |
| 15 | Admin Menu (command block) | Requires `blockprot.user.admin` |
| 14 | Copy configuration (paper) | Owner / manager |
| 13 | Paste configuration (knowledge book) | Owner / manager + clipboard |
| 12 | Inspect contents (spyglass) | Admin + non-owner + inventory block |
| 11 | Audit log (clock) | Owner or admin + logger active |
| 17 | Back | Always |

#### Statistics list

Each block entry shows two lore lines depending on your permissions:

| Click | Permission | Result |
|---|---|---|
| Left-click | `blockprot.blocks.tp` | Teleport to that block |
| Left-click | *(no permission)* | Action bar: missing permission |
| Right-click | `blockprot.remote.access` | Open block's lock menu remotely |
| Right-click | *(no permission)* | Action bar: missing permission |

---

### Commands

Commands beyond `/bp user` and `/bp admin` are **hidden by default**.
Set `commands_enabled: true` in `config.yml` to expose and enable them.

| Command | Description | Requires |
|---|---|---|
| `/bp user` | Open user GUI | `blockprot.user` |
| `/bp admin` | Open admin GUI | `blockprot.user.admin` |
| `/bp transfer <player>` | Transfer block ownership | `commands_enabled: true` |
| `/bp timed <player> <sec>` | Grant timed access | `commands_enabled: true` |
| `/bp friends addall <player>` | Add friend to all your blocks | `commands_enabled: true` |
| `/bp info <player>` | List player's blocks | `commands_enabled: true` + admin |
| `/bp stats` | Protection statistics | `commands_enabled: true` |
| `/bp reload` | Reload config | admin |
| `/bp debug` | Diagnostics | admin |
| `/bp update` | Check for updates | admin |
| `/bp disablehints` | Disable hints via command | any |
| `/bp about` | Plugin info | any |

Alias: `/blockprot`. Spanish aliases available when `localized_command_aliases: true`.

---

### Permissions

| Permission | Default | Description |
|---|---|---|
| `blockprot.user` | `true` | All standard user actions |
| `blockprot.user.admin` | `op` | Admin actions (reload, debug, info, …) |
| `blockprot.bypass` | `false` | Bypass all protection |
| `blockprot.remote.access` | `op` | Open a block's lock menu remotely from statistics |
| `blockprot.blocks.tp` | `op` | Teleport to a block from statistics |

---

### Configuration reference (`config.yml`)

```yaml
# ── 1. General ──────────────────────────────────────────────────────
language_file: translations_en.yml
fallback_string: "Unknown translation"
replace_translations: true
notify_op_of_updates: false
localized_command_aliases: true
excluded_worlds: []
worlds_config_enabled: false
inactivity_cleanup_days: -1   # -1 = disabled

# ── 2. Defaults ──────────────────────────────────────────────────────
lock_on_place_by_default: true
public_is_friend_by_default: false
player_max_locked_block_count: -1
lock_hint_cooldown_in_seconds: 10
friend_search_similarity: 0.5
disable_friend_functionality: false
redstone_disallowed_by_default: false

# ── 3. Safety ────────────────────────────────────────────────────────
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

# ── 6. Optional Features ──────────────────────────────────────────────
optional_features_enable_all: false

# Expose /bp transfer, /bp timed, /bp stats, etc. in tab-complete.
# false = only /bp user and /bp admin are shown and usable.
commands_enabled: false

timed_access_max_duration_days: 90
worldedit_paste_autolock:
  enabled: false
  radius: 24
  max_blocks_per_paste: 5000
  delay_ticks: 20
```

---

### Integrations

| Plugin | Notes |
|---|---|
| **Towny** | Respects town/nation permissions |
| **WorldGuard** | Honors region flags |
| **PlaceholderAPI** | Stats and protection status placeholders |
| **Lands** | Claim permission checks |
| **ClaimChunk** | Prevents locking in unowned chunks |
| **SkinsRestorer** | Correct player heads in offline-mode servers |
| **WorldEdit / FAWE** | Optional paste auto-lock |

---

### Compatibility

| | |
|---|---|
| **Minecraft** | 1.21, 1.21.1, 1.21.4, 26.1.x |
| **Server** | Paper, Spigot |
| **Java** | 25+ required |
| **MySQL** | MySQL 8+, MariaDB 10.5+ (optional) |
| **Languages** | EN, ES, DE, FR, IT, PT-BR, RU, JA, KO, ZH-CN, ZH-TW, CS, SK, PL, TR |

---

## License

Licensed under the **GNU General Public License v3**. See [`LICENSE`](LICENSE) for details.

<sub>Based on <a href="https://github.com/spnda/BlockProt">BlockProt</a> by spnda — original copyright notices preserved as required by GPL v3.</sub>
