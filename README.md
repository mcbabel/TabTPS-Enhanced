<img src="assets/logo-256.png" width="128">

# TabTPS-Folia

Shows TPS, MSPT and other server stats in the tab list, action bar and boss bar. On Folia every player sees the values of the region they are in, not a server-wide average.

Started as a Folia fork of [TabTPS by jpenilla](https://github.com/jpenilla/TabTPS). Since 1.2.0 it also runs on Canvas, Paper, Purpur, Leaf and Spigot, and since 1.3.0 as a Forge mod.

Download: [Modrinth](https://modrinth.com/plugin/tabtps-folia) or [GitHub releases](https://github.com/MCbabel/TabTPS-Folia/releases)

## Platforms

### Plugin

One jar for all of these servers, requires **Java 17+** and Minecraft **1.20 – 26.3**:

| Server | Tick data | Tested on |
|---|---|---|
| [Folia](https://papermc.io/software/folia) | per region | 1.20.1, 1.20.2, 1.20.4, 1.20.6, 1.21.4, 1.21.5, 1.21.6, 1.21.8, 1.21.11, 26.1.2, 26.2, 26.3 |
| [Canvas](https://canvasmc.io) (Folia fork) | per region | 26.3 |
| [Paper](https://papermc.io/software/paper) | global | 1.20.1, 26.3 |
| [Purpur](https://purpurmc.org) | global | 1.20.1, 1.21.4, 1.21.10, 26.3 |
| [Leaf](https://www.leafmc.one) | global | 1.21.4, 26.3 |
| [Spigot](https://www.spigotmc.org) | global | 1.20.1, 1.21.1, 1.21.4, 26.3 |

The plugin detects at startup whether the server ticks regions and picks its tick data source accordingly. Spigot has no Paper API, so there the global TPS/MSPT are read directly from the server.

### Forge mod

A Forge mod needs one jar per Minecraft version:

| Minecraft | Forge | Java | Tested on |
|---|---|---|---|
| 26.3 | 66+ | 25 | dedicated server, singleplayer |
| 1.20.1 | 47+ | 17 | dedicated server, singleplayer |

Put the jar into the `mods/` folder. It only runs on the server side: players do not need it to join a server, and in singleplayer it runs on the built-in server. The config lives in `config/tabtps/`. Permissions go through Forge's permission API, so permission mods that hook into it (for example LuckPerms) can grant them; without one, the display and command permissions default to operators. In singleplayer the world owner always has every permission, so the displays work without cheats.

## Features

### Regional TPS/MSPT

On Folia, each player sees the TPS and MSPT of **their current region**, not a global average. When a player moves between regions, the displayed values update automatically.

On servers without regions (Paper, Purpur, Leaf, Spigot) there is a single global tick loop, so `tps` and `mspt` show the server-wide values and the region modules below report `N/A`.

### Modules

These modules can be used in display configs:

| Module | Description | Without regions |
|---|---|---|
| `tps` | TPS of the player's current region | global TPS |
| `mspt` | MSPT of the player's current region | global MSPT |
| `lowest_region_tps` | Lowest TPS across all active regions | `N/A` |
| `median_region_tps` | Median TPS across all active regions | `N/A` |
| `highest_region_tps` | Highest TPS across all active regions | `N/A` |
| `ping` | Player ping | same |
| `cpu` | CPU usage | same |
| `memory` | Memory usage | same |
| `players` | Online player count | same |
| `uptime` | Time since the server started, e.g. `14:15:16` or `4d 14:15:16` | same |
| `mob_caps` | Mob cap usage of the player's world for every category | `N/A` on Folia and Canvas |
| `mob_cap_<category>` | One category, e.g. `mob_cap_monster`, `mob_cap_creature` | `N/A` on Folia and Canvas |

Mob cap categories: `monster`, `creature`, `ambient`, `axolotls`, `underground_water_creature`, `water_creature`, `water_ambient`. On Forge the values come straight from Minecraft's spawner, on Paper, Purpur, Leaf and Spigot they are counted once a second from the server's entities and the chunks around players. Folia ticks mob caps per region and does not allow counting a whole world, so there the mob cap modules show `N/A`.

### Live Information Displays

#### Tab menu
* Command: ``/tabtps toggle tab``
* ![tab menu](https://i.imgur.com/93NmuUA.png)

#### Action bar
* Command: ``/tabtps toggle actionbar``
* ![action bar](https://i.imgur.com/aMzzNRR.png)

#### Boss bar
* Command: ``/tabtps toggle bossbar``
* ![boss bar](https://i.postimg.cc/xCJnGYfb/bossbar.png)

Configure display modules and themes in `plugins/TabTPS/display-configs/` and `plugins/TabTPS/themes/` (`config/tabtps/` on Forge).

### Optional settings

Everything below is off by default; without changing a config TabTPS behaves exactly like before.

#### Custom text
Each display has a `format` (tab list: `header-format` and `footer-format`) that replaces its module list with your own [MiniMessage](https://docs.advntr.dev/minimessage/format.html) text. Module names work as tags for their value, `<name_label>` gives the translated label:
```hocon
format="<gray>The server runs at <tps> TPS, your ping is <ping>, up for <uptime>"
footer-format="<tps_label>: <tps> <dark_gray>| <gray><mob_caps_label>: <mob_cap_monster>"
```

#### More color levels
Themes take optional `tps-levels` and `mspt-levels`, the boss bar takes optional `levels` by fill percent:
```hocon
tps-levels=[{threshold=19.5, color=green, secondary-color=dark_green}, {threshold=17, color=yellow, secondary-color=gold}, {threshold=0, color=dark_red, secondary-color=red}]
levels=[{percent=0, color=BLUE}, {percent=30, color=GREEN}, {percent=60, color=YELLOW}, {percent=90, color=RED}]
```
The boss bar also follows `/tick rate` on servers that support it, so a full bar always means the target tick time.

#### Boss bar delay
`login-delay-seconds` in the boss bar settings waits that many seconds after a player joins before the boss bar appears, e.g. so boss bars of other plugins come first.

#### Plain console output
`console-colors=false` in `main.conf` turns off colors and gradients for command output in the server console. In the console `/memory` also prints the used, allocated and maximum memory behind each bar.

### PlaceholderAPI

If [PlaceholderAPI](https://modrinth.com/plugin/placeholderapi) is installed, TabTPS registers these placeholders by itself (plugin only, not Forge):

| Placeholder | Value |
|---|---|
| `%tabtps_tps%`, `%tabtps_tps_5s%`, `_1m`, `_5m`, `_15m` | TPS (of the player's region on Folia) |
| `%tabtps_mspt%` | MSPT (of the player's region on Folia) |
| `%tabtps_ping%` | Player ping |
| `%tabtps_players%`, `%tabtps_max_players%` | Player count |
| `%tabtps_uptime%` | Server uptime |
| `%tabtps_lowest_region_tps%`, `%tabtps_median_region_tps%`, `%tabtps_highest_region_tps%` | Region TPS on Folia, `N/A` elsewhere |
| `%tabtps_cpu_system%`, `%tabtps_cpu_process%` | CPU usage in percent |
| `%tabtps_memory_used%`, `%tabtps_memory_allocated%`, `%tabtps_memory_max%` | Memory in MB |

Add `_colored` to `tps`, `mspt`, `ping` and the region placeholders to get TabTPS' colors, e.g. `%tabtps_tps_colored%`.

### Commands

#### Tick Info command
* Command: ``/tickinfo`` or ``/mspt`` (on Paper-based servers ``/mspt`` is Paper's own command)
* On Folia and Canvas: region TPS overview (Lowest/Median/Highest). Without regions: the standard tick time breakdown
* Permission required: ``tabtps.tps``
* ![tps command](https://i.imgur.com/w69C8r1.png)

#### Memory command
* Command: ``/memory``, `/mem`, or ``/ram``
* Permission required: ``tabtps.tps``
* ![memory command](https://i.imgur.com/eYeUNMc.png)

#### Ping command
* Commands: ``/ping``, `/ping [username]`, or ``/pingall``
* Permissions: ``tabtps.ping`` to view your own ping, ``tabtps.ping.others`` to view other users ping.
* ![ping command](https://i.imgur.com/0agY7lB.png)
* ![ping all](https://i.imgur.com/t1lBt2b.png)

#### Toggle commands
* Commands: ``/tabtps toggle tab``, ``/tabtps toggle actionbar``, ``/tabtps toggle bossbar``, each optionally with a player name, e.g. ``/tabtps toggle bossbar Steve``
* Permission ``tabtps.toggle.others`` is needed to toggle displays of other players
* Works with ``/execute as <player> run tabtps toggle ...`` from command blocks, the console or data packs; the permissions of whoever runs the command count

#### Mob caps command
* Command: ``/tabtps mobcaps``
* Lists the mob caps of every world and category
* Permission required: ``tabtps.mobcaps``

#### Reload command
* Command: ``/tabtps reload``
* Permission required: ``tabtps.reload``

### Metrics

The plugin sends anonymous usage statistics to [bStats](https://bstats.org). Server owners can turn this off in `plugins/bStats/config.yml`.

### Update checker

On startup TabTPS asks Modrinth once whether a newer version exists for your platform and Minecraft version. If there is one, it prints a hint in the console, and players with the `tabtps.updatenotify` permission (operators by default) get a short chat message with a download link when they join. `update-notify-on-join=false` in `main.conf` turns off the chat message, `update-checker=false` turns off the whole check.

## Building

```bash
./gradlew :tabtps-folia:shadowJar
```

The built JAR will be at `folia/build/libs/tabtps-folia-*.jar`.

The Forge mods build with `./gradlew :tabtps-forge-26-3:build` and `./gradlew :tabtps-forge-1-20-1:build` and end up in `forge/<version>/build/libs/tabtps-forge-mc<version>-*.jar`. For 1.20.1 that jar is renamed to Forge's runtime (SRG) names; the `-mojmap` jar next to it is only for Forge's development client.

### Notes on the build

The plugin compiles against the **oldest** supported API, `folia-api` 1.20.1, and targets Java 17,
so one jar runs on every server from 1.20 to 26.3. Newer Folia internals are only reached through
reflection, and the reflection goes through names that are the same on every version: Bukkit worlds,
`CraftWorld#getHandle` and Folia's own `io.papermc.paper.threadedregions` classes. That matters
because Folia up to 1.20.4 runs with obfuscated Spigot names, and the CraftBukkit package carries a
version suffix there. `com.mojang:brigadier` is a separate compile-only dependency because the
1.20.1 API does not ship it yet.

The plugin shades and relocates its own Adventure 4, because neither `adventure-platform-bukkit`
(4.4.1 is the newest) nor `cloud-minecraft-extras` (2.0.1) has an Adventure 5 release yet, while
Folia moved to Adventure 5 in 26.2. That is why `net.kyori` and `org.slf4j` are excluded from the
`folia-api` dependency: every library that `shadowJar` relocates has to be compiled against the
exact version that gets bundled. Compiling against a 26.2+ API does not work for the same reason,
because `Player` there extends Adventure 5 types such as `ObjectContentsLike`. Revisit this once
`adventure-platform-bukkit` and `cloud-minecraft-extras` ship Adventure 5 builds.

### Testing

`testservers/test.sh` starts real servers with the built jar, runs the TabTPS console commands and prints one result line per server. It downloads each server from `testservers/servers.txt` (Spigot through BuildTools), keeps only the log in `testservers/logs/` and deletes the server afterwards.

```bash
testservers/test.sh folia-26.3 purpur-1.21.10
testservers/test.sh all
```

`forge-*` entries install the server with the Forge installer and put the mod into `mods/`. `testservers/forge-client` starts Forge's development client straight into a singleplayer world, for example `./gradlew -p testservers/forge-client runClient`, or with `-PforgeVersion=1.20.1-47.4.28 -PjavaVersion=17 -Pmappings=1.20.1` for 1.20.1. It loads whatever jar is in `testservers/forge-client/run/<forge version>/mods/` and expects a world called `tabtps-test` in `saves/`.

Releases are published to Modrinth by `.github/workflows/modrinth.yml` as soon as a GitHub release is published.

## Credits

- Original [TabTPS](https://github.com/jpenilla/TabTPS) by [jpenilla](https://github.com/jpenilla) (MIT License)
- Folia support by [MCbabel](https://github.com/MCbabel)

## License

[MIT License](license.txt) — Copyright (c) 2020-2024 Jason Penilla, Copyright (c) 2026 MCbabel
