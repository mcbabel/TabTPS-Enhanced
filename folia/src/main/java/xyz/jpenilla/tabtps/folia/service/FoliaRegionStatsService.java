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
package xyz.jpenilla.tabtps.folia.service;

import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import xyz.jpenilla.tabtps.common.User;
import xyz.jpenilla.tabtps.common.service.RegionStatsService;
import xyz.jpenilla.tabtps.folia.TabTPSFolia;

public final class FoliaRegionStatsService implements RegionStatsService {
  private final TabTPSFolia plugin;
  private final FoliaTickTimeService tickTimeService;

  public FoliaRegionStatsService(final @NonNull TabTPSFolia plugin, final @NonNull FoliaTickTimeService tickTimeService) {
    this.plugin = plugin;
    this.tickTimeService = tickTimeService;
  }

  private List<Double> collectRegionTps() {
    return this.tickTimeService.sortedRegionTps();
  }

  @Override
  public double lowestRegionTps() {
    final List<Double> tps = this.collectRegionTps();
    if (tps.isEmpty()) return 20.0;
    return tps.get(0);
  }

  @Override
  public double medianRegionTps() {
    final List<Double> tps = this.collectRegionTps();
    if (tps.isEmpty()) return 20.0;
    final int size = tps.size();
    if (size % 2 == 0) {
      return (tps.get(size / 2 - 1) + tps.get(size / 2)) / 2.0;
    }
    return tps.get(size / 2);
  }

  @Override
  public double highestRegionTps() {
    final List<Double> tps = this.collectRegionTps();
    if (tps.isEmpty()) return 20.0;
    return tps.get(tps.size() - 1);
  }

  @Override
  public @NonNull List<Region> regions() {
    return this.tickTimeService.regions();
  }

  @Override
  public boolean teleport(final @NonNull User<?> user, final @NonNull String worldName, final int blockX, final int blockZ) {
    final Object base = user.base();
    final World world = Bukkit.getWorld(worldName);
    if (!(base instanceof Player) || world == null) {
      return false;
    }
    final Player player = (Player) base;
    final Location target = new Location(world, blockX + 0.5, world.getMinHeight(), blockZ + 0.5);
    Bukkit.getRegionScheduler().execute(this.plugin, target, () -> {
      final Location safe = safeLocation(world, blockX, blockZ);
      if (safe != null) {
        player.teleportAsync(safe);
      }
    });
    return true;
  }

  private static @Nullable Location safeLocation(final World world, final int x, final int z) {
    final int top = world.getEnvironment() == World.Environment.NETHER
      ? Math.min(world.getMaxHeight(), 125)
      : world.getHighestBlockYAt(x, z) + 1;
    for (int y = top; y > world.getMinHeight(); y--) {
      final Block feet = world.getBlockAt(x, y, z);
      if (feet.isPassable() && !feet.isLiquid() && feet.getRelative(0, 1, 0).isPassable() && feet.getRelative(0, -1, 0).getType().isSolid()) {
        return new Location(world, x + 0.5, y, z + 0.5);
      }
    }
    final Block highest = world.getHighestBlockAt(x, z);
    return highest.getType() == Material.AIR ? null : highest.getLocation().add(0.5, 1, 0.5);
  }
}
