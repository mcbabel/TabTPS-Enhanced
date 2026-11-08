/*
 * This file is part of TabTPS, licensed under the MIT License.
 *
 * Copyright (c) 2020-2024 Jason Penilla
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package xyz.jpenilla.tabtps.folia.placeholder;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import xyz.jpenilla.tabtps.common.User;
import xyz.jpenilla.tabtps.common.config.Theme;
import xyz.jpenilla.tabtps.common.module.UptimeModule;
import xyz.jpenilla.tabtps.common.service.RegionStatsService;
import xyz.jpenilla.tabtps.common.service.TickTimeService;
import xyz.jpenilla.tabtps.common.util.MemoryUtil;
import xyz.jpenilla.tabtps.common.util.PingUtil;
import xyz.jpenilla.tabtps.common.util.TPSUtil;
import xyz.jpenilla.tabtps.folia.TabTPSFolia;

import static net.kyori.adventure.text.Component.text;

public final class PlaceholderValues {
  static final String COLORED = "_colored";
  private static final String UNAVAILABLE = "N/A";

  static final List<String> PLAYER_KEYS = Collections.unmodifiableList(Arrays.asList(
    "tps", "tps_5s", "tps_1m", "tps_5m", "tps_15m", "mspt", "ping"
  ));
  static final List<String> GLOBAL_KEYS = Collections.unmodifiableList(Arrays.asList(
    "players", "max_players", "uptime", "lowest_region_tps", "median_region_tps", "highest_region_tps",
    "cpu_system", "cpu_process", "memory_used", "memory_allocated", "memory_max"
  ));
  static final List<String> COLORED_KEYS = Collections.unmodifiableList(Arrays.asList(
    "tps", "tps_5s", "tps_1m", "tps_5m", "tps_15m", "mspt", "ping", "lowest_region_tps", "median_region_tps", "highest_region_tps"
  ));

  private final TabTPSFolia plugin;

  PlaceholderValues(final @NonNull TabTPSFolia plugin) {
    this.plugin = plugin;
  }

  @Nullable Component resolve(final @Nullable Player player, final @NonNull String params) {
    final String key = params.toLowerCase(Locale.ROOT);
    final boolean colored = key.endsWith(COLORED);
    final String name = colored ? key.substring(0, key.length() - COLORED.length()) : key;
    final User<?> user = player == null ? null : this.plugin.userService().user(player);
    final TickTimeService ticks = this.plugin.tickTimeService();

    switch (name) {
      case "tps":
        return tps(ticks.displayTps(user), colored);
      case "tps_5s":
        return tpsWindow(ticks.recentTps(user), 0, colored);
      case "tps_1m":
        return tpsWindow(ticks.recentTps(user), 1, colored);
      case "tps_5m":
        return tpsWindow(ticks.recentTps(user), 2, colored);
      case "tps_15m":
        return tpsWindow(ticks.recentTps(user), 3, colored);
      case "mspt":
        return colored ? TPSUtil.coloredMspt(ticks.averageMspt(user), colors()) : text(TPSUtil.formatDouble(ticks.averageMspt(user)));
      case "ping":
        if (user == null) {
          return text(UNAVAILABLE);
        }
        return colored ? PingUtil.coloredPing(user, colors()) : text(user.ping());
      case "players":
        return text(this.plugin.userService().onlinePlayers());
      case "max_players":
        return text(this.plugin.maxPlayers());
      case "uptime":
        return text(UptimeModule.format(this.plugin.tabTPS().uptime()));
      case "lowest_region_tps":
      case "median_region_tps":
      case "highest_region_tps":
        return this.regionTps(name, colored);
      case "cpu_system":
        return text(this.plugin.tabTPS().cpuMonitor().recentSystemCpuLoadSnapshot());
      case "cpu_process":
        return text(this.plugin.tabTPS().cpuMonitor().recentProcessCpuLoadSnapshot());
      case "memory_used":
        return text(MemoryUtil.usedMemory());
      case "memory_allocated":
        return text(MemoryUtil.committedMemory());
      case "memory_max":
        return text(MemoryUtil.maxMemory());
      default:
        return null;
    }
  }

  private @NonNull Component regionTps(final @NonNull String name, final boolean colored) {
    final RegionStatsService stats = this.plugin.regionStatsService();
    if (stats == null) {
      return text(UNAVAILABLE);
    }
    switch (name) {
      case "lowest_region_tps":
        return tps(stats.lowestRegionTps(), colored);
      case "median_region_tps":
        return tps(stats.medianRegionTps(), colored);
      default:
        return tps(stats.highestRegionTps(), colored);
    }
  }

  private static @NonNull Component tpsWindow(final double @NonNull [] recent, final int window, final boolean colored) {
    final int index = recent.length == 3 ? window - 1 : window;
    if (index < 0 || index >= recent.length) {
      return text(UNAVAILABLE);
    }
    return tps(recent[index], colored);
  }

  private static @NonNull Component tps(final double tps, final boolean colored) {
    return colored ? TPSUtil.coloredTps(tps, colors()) : text(TPSUtil.formatDouble(tps));
  }

  private static Theme.@NonNull Colors colors() {
    return Theme.DEFAULT.colorScheme();
  }
}
