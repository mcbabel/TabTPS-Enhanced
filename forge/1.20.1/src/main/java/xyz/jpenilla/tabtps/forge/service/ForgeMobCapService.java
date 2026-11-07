/*
 * This file is part of TabTPS, licensed under the MIT License.
 *
 * Copyright (c) 2020-2024 Jason Penilla
 * Copyright (c) 2026 MCbabel (Forge support)
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
package xyz.jpenilla.tabtps.forge.service;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.NaturalSpawner;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import xyz.jpenilla.tabtps.common.User;
import xyz.jpenilla.tabtps.common.service.MobCapService;

public final class ForgeMobCapService implements MobCapService {
  private static final int CHUNK_AREA = 17 * 17;

  private volatile Map<String, WorldMobCaps> snapshot = Collections.emptyMap();

  public void update(final @NonNull MinecraftServer server) {
    final Map<String, WorldMobCaps> worlds = new LinkedHashMap<>();
    for (final ServerLevel level : server.getAllLevels()) {
      final NaturalSpawner.SpawnState state = level.getChunkSource().getLastSpawnState();
      if (state == null) {
        continue;
      }
      final int chunks = state.getSpawnableChunkCount();
      final Object2IntMap<MobCategory> counts = state.getMobCategoryCounts();
      final List<Category> categories = new ArrayList<>();
      for (final MobCategory category : MobCategory.values()) {
        if (category == MobCategory.MISC) {
          continue;
        }
        categories.add(new Category(category.getName(), counts.getInt(category), category.getMaxInstancesPerChunk() * chunks / CHUNK_AREA));
      }
      final String name = name(level);
      worlds.put(name, new WorldMobCaps(name, categories));
    }
    this.snapshot = worlds;
  }

  public void clear() {
    this.snapshot = Collections.emptyMap();
  }

  private static @NonNull String name(final @NonNull ServerLevel level) {
    return level.dimension().location().toString();
  }

  @Override
  public @NonNull List<WorldMobCaps> worlds() {
    return new ArrayList<>(this.snapshot.values());
  }

  @Override
  public @Nullable WorldMobCaps world(final @NonNull User<?> user) {
    final Object base = user.base();
    return base instanceof ServerPlayer player ? this.snapshot.get(name(player.serverLevel())) : null;
  }
}
