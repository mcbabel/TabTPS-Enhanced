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
package xyz.jpenilla.tabtps.folia.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.SpawnCategory;
import org.bukkit.plugin.Plugin;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import xyz.jpenilla.tabtps.common.User;
import xyz.jpenilla.tabtps.common.service.MobCapService;

public final class BukkitMobCapService implements MobCapService {
  private static final int CHUNK_AREA = 17 * 17;
  private static final int SPAWN_RADIUS = 8;
  private static final Map<SpawnCategory, String> CATEGORIES = new LinkedHashMap<>();

  static {
    CATEGORIES.put(SpawnCategory.MONSTER, "monster");
    CATEGORIES.put(SpawnCategory.ANIMAL, "creature");
    CATEGORIES.put(SpawnCategory.AMBIENT, "ambient");
    CATEGORIES.put(SpawnCategory.AXOLOTL, "axolotls");
    CATEGORIES.put(SpawnCategory.WATER_UNDERGROUND_CREATURE, "underground_water_creature");
    CATEGORIES.put(SpawnCategory.WATER_ANIMAL, "water_creature");
    CATEGORIES.put(SpawnCategory.WATER_AMBIENT, "water_ambient");
  }

  private volatile Map<UUID, WorldMobCaps> snapshot = Collections.emptyMap();

  public void start(final @NonNull Plugin plugin) {
    Bukkit.getScheduler().runTaskTimer(plugin, this::update, 20L, 20L);
  }

  private void update() {
    final Map<UUID, WorldMobCaps> worlds = new LinkedHashMap<>();
    for (final World world : Bukkit.getWorlds()) {
      final int chunks = spawnableChunks(world);
      final Map<SpawnCategory, Integer> counts = new EnumMap<>(SpawnCategory.class);
      for (final LivingEntity entity : world.getLivingEntities()) {
        if (entity instanceof Player || !entity.getRemoveWhenFarAway()) {
          continue;
        }
        counts.merge(entity.getSpawnCategory(), 1, Integer::sum);
      }
      final List<Category> categories = new ArrayList<>();
      CATEGORIES.forEach((category, name) -> {
        final int limit = world.getSpawnLimit(category);
        categories.add(new Category(name, counts.getOrDefault(category, 0), Math.max(0, limit) * chunks / CHUNK_AREA));
      });
      worlds.put(world.getUID(), new WorldMobCaps(world.getName(), categories));
    }
    this.snapshot = worlds;
  }

  private static int spawnableChunks(final @NonNull World world) {
    final Set<Long> chunks = new HashSet<>();
    for (final Player player : world.getPlayers()) {
      if (player.getGameMode() == GameMode.SPECTATOR) {
        continue;
      }
      final Location location = player.getLocation();
      final int chunkX = location.getBlockX() >> 4;
      final int chunkZ = location.getBlockZ() >> 4;
      for (int x = -SPAWN_RADIUS; x <= SPAWN_RADIUS; x++) {
        for (int z = -SPAWN_RADIUS; z <= SPAWN_RADIUS; z++) {
          chunks.add(((long) (chunkX + x) << 32) | ((chunkZ + z) & 0xFFFFFFFFL));
        }
      }
    }
    return chunks.size();
  }

  @Override
  public @NonNull List<WorldMobCaps> worlds() {
    return new ArrayList<>(this.snapshot.values());
  }

  @Override
  public @Nullable WorldMobCaps world(final @NonNull User<?> user) {
    final Object base = user.base();
    return base instanceof Player ? this.snapshot.get(((Player) base).getWorld().getUID()) : null;
  }
}
