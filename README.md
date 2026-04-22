# BlockParty

![Java 21](https://img.shields.io/badge/Java-21-007396)
![Spigot/Paper](https://img.shields.io/badge/Server-Spigot%2FPaper-orange)
![Minecraft 1.21.4](https://img.shields.io/badge/Minecraft-1.21.4-3C8527)
![WorldEdit Required](https://img.shields.io/badge/Dependency-WorldEdit-blue)

BlockParty is a WorldEdit-powered Minecraft mini-game for Spigot/Paper. Players race to stand on the correct block before the floor disappears. The last player standing wins.

## Table of Contents

- [Features](#features)
- [Requirements](#requirements)
- [Quick Start](#quick-start)
- [Commands](#commands)
- [Gameplay Flow](#gameplay-flow)
- [Configuration](#configuration)
- [Region Editing](#region-editing)
- [Troubleshooting](#troubleshooting)

## Features

- WorldEdit-based arena creation from your current selection
- Auto-generated block list from the selected region
- Region management commands for create, edit, delete, info, and list
- Join, leave, start, stop, reload, help, and stats commands
- Auto-start when enough players join
- Optional force-start for admins
- Configurable round timing and waiting phases
- Player stats stored in a SQLite database
- Hotbar restoration after games

## Requirements

- Java 21
- Spigot/Paper 1.21.4 or compatible
- [WorldEdit](https://enginehub.org/worldedit/) installed and enabled on the server

## Quick Start

1. Build the plugin with Maven:

```powershell
mvn clean package
```

2. Copy the jar from `target/` into your server's `plugins/` folder.
3. Install WorldEdit.
4. Start the server once to generate the BlockParty data files.
5. Edit `plugins/BlockParty/config.yml` if you want to change timings, messages, or stats settings.

## Commands

Main command: `/blockparty`  
Alias: `/bp`

### Region Management

- `/blockparty create <region_name> [minPlayers]` - Create a region from your current WorldEdit selection
- `/blockparty edit <region_name> <option> <value>` - Edit a region option
- `/blockparty delete <region_name>` - Delete a region
- `/blockparty info <region_name>` - Show region details
- `/blockparty list` - List all saved regions

### Gameplay

- `/blockparty join <region_name>` - Join a BlockParty game
- `/blockparty leave` - Leave your current game
- `/blockparty start <region_name> [force]` - Start a game manually
- `/blockparty stop <region_name>` - Stop an active game

### Admin / Utility

- `/blockparty reload` - Reload config and stop active games
- `/blockparty stats <player>` - View player stats
- `/blockparty help` - Show help

## Gameplay Flow

1. Create an arena using a WorldEdit selection.
2. Players join the arena with `/blockparty join <region_name>`.
3. When the minimum player count is reached, the game waits briefly for more players.
4. A round starts and a block is selected.
5. Players must stand on that block before the timer expires.
6. All other blocks are removed after the round timer.
7. Players below the arena floor are eliminated after the elimination check delay.
8. The round timer decreases each round.
9. The last player standing wins.

## Configuration

The default config is generated at `plugins/BlockParty/config.yml`.

### Important Settings

- `game.initial_round_time` - Starting time for the first round
- `game.minimum_round_time` - Lowest allowed round timer
- `game.time_decrease_per_round` - How much the timer decreases each round
- `game.delay_between_rounds` - Delay between rounds in ticks
- `game.preparation_time` - Countdown before the game starts
- `game.waiting_for_players_time` - Waiting time after minimum players are reached
- `game.elimination_check_delay` - Delay before checking for eliminations
- `regions.default_min_players` - Default minimum players for new regions
- `database.type` - Database type (`sqlite`)
- `database.filename` - Stats database file name
- `stats.enabled` - Enable or disable stats tracking
- `features.restore_blocks_after_game` - Restore arena blocks after the game ends

## Region Editing

`/blockparty edit <region_name> <option> <value>` currently supports:

- `name` - Rename the region
- `minPlayers` - Change the minimum player requirement
- `blocks` - Replace the saved block list with a comma-separated list

## Troubleshooting

- If `/blockparty create` fails, make sure you have a valid WorldEdit selection.
- If the game will not start, check that the region has enough players and that WorldEdit is installed.
- If stats are not recording, verify that `stats.enabled` is set to `true` in `config.yml`.
- If you reload or disable the plugin, active games are stopped cleanly.

