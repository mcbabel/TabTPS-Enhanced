/*
 * This file is part of TabTPS, licensed under the MIT License.
 *
 * Copyright (c) 2020-2024 Jason Penilla
 * Copyright (c) 2026 MCbabel (Folia support)
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

import java.util.Locale;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.kyori.adventure.text.Component;
import org.bukkit.OfflinePlayer;
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
import xyz.jpenilla.tabtps.common.util.Serializers;
import xyz.jpenilla.tabtps.common.util.TPSUtil;
import xyz.jpenilla.tabtps.folia.TabTPSFolia;

public final class TabTPSPlaceholders extends PlaceholderExpansion {
  private static final String COLORED = "_colored";
  private static final String UNAVAILABLE = "N/A";

  private final TabTPSFolia plugin;

  public TabTPSPlaceholders(final @NonNull TabTPSFolia plugin) {
    this.plugin = plugin;
  }

  @Override
  public @NonNull String getIdentifier() {
    return "tabtps";
  }

  @Override
  public @NonNull String getAuthor() {
    return String.join(", ", this.plugin.getDescription().getAuthors());
  }

  @Override
  public @NonNull String getVersion() {
    return this.plugin.getDescription().getVersion();
  }

  @Override
  public boolean persist() {
    return true;
  }

  @Override
  public @Nullable String onRequest(final @Nullable OfflinePlayer offlinePlayer, final @NonNull String params) {
    final String key = params.toLowerCase(Locale.ROOT);
    final boolean colored = key.endsWith(COLORED);
    final String name = colored ? key.substring(0, key.length() - COLORED.length()) : key;
    final Player player = offlinePlayer == null ? null : offlinePlayer.getPlayer();
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
        return mspt(ticks.averageMspt(user), colored);
      case "ping":
        if (user == null) {
          return UNAVAILABLE;
        }
        return colored ? legacy(PingUtil.coloredPing(user, colors())) : String.valueOf(user.ping());
      case "players":
        return String.valueOf(this.plugin.userService().onlinePlayers());
      case "max_players":
        return String.valueOf(this.plugin.maxPlayers());
      case "uptime":
        return UptimeModule.format(this.plugin.tabTPS().uptime());
      case "lowest_region_tps":
      case "median_region_tps":
      case "highest_region_tps":
        return this.regionTps(name, colored);
      case "cpu_system":
        return String.valueOf(this.plugin.tabTPS().cpuMonitor().recentSystemCpuLoadSnapshot());
      case "cpu_process":
        return String.valueOf(this.plugin.tabTPS().cpuMonitor().recentProcessCpuLoadSnapshot());
      case "memory_used":
        return String.valueOf(MemoryUtil.usedMemory());
      case "memory_allocated":
        return String.valueOf(MemoryUtil.committedMemory());
      case "memory_max":
        return String.valueOf(MemoryUtil.maxMemory());
      default:
        return null;
    }
  }

  private @NonNull String regionTps(final @NonNull String name, final boolean colored) {
    final RegionStatsService stats = this.plugin.regionStatsService();
    if (stats == null) {
      return UNAVAILABLE;
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

  private static @NonNull String tpsWindow(final double @NonNull [] recent, final int window, final boolean colored) {
    final int index = recent.length == 3 ? window - 1 : window;
    if (index < 0 || index >= recent.length) {
      return UNAVAILABLE;
    }
    return tps(recent[index], colored);
  }

  private static @NonNull String tps(final double tps, final boolean colored) {
    return colored ? legacy(TPSUtil.coloredTps(tps, colors())) : TPSUtil.formatDouble(tps);
  }

  private static @NonNull String mspt(final double mspt, final boolean colored) {
    return colored ? legacy(TPSUtil.coloredMspt(mspt, colors())) : TPSUtil.formatDouble(mspt);
  }

  private static Theme.@NonNull Colors colors() {
    return Theme.DEFAULT.colorScheme();
  }

  private static @NonNull String legacy(final @NonNull Component component) {
    return Serializers.LEGACY_SECTION.serialize(component);
  }
}
