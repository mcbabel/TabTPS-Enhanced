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
package xyz.jpenilla.tabtps.common.service;

import java.util.Collections;
import java.util.List;
import org.checkerframework.checker.nullness.qual.NonNull;
import xyz.jpenilla.tabtps.common.User;

public interface RegionStatsService {
  double lowestRegionTps();

  double medianRegionTps();

  double highestRegionTps();

  default @NonNull List<Region> regions() {
    return Collections.emptyList();
  }

  default boolean teleport(final @NonNull User<?> user, final @NonNull String world, final int blockX, final int blockZ) {
    return false;
  }

  final class Region {
    private final String world;
    private final boolean located;
    private final int blockX;
    private final int blockZ;
    private final double tps;
    private final double mspt;
    private final double utilisation;
    private final int chunks;
    private final int players;
    private final int entities;

    public Region(
      final @NonNull String world,
      final boolean located,
      final int blockX,
      final int blockZ,
      final double tps,
      final double mspt,
      final double utilisation,
      final int chunks,
      final int players,
      final int entities
    ) {
      this.world = world;
      this.located = located;
      this.blockX = blockX;
      this.blockZ = blockZ;
      this.tps = tps;
      this.mspt = mspt;
      this.utilisation = utilisation;
      this.chunks = chunks;
      this.players = players;
      this.entities = entities;
    }

    public @NonNull String world() {
      return this.world;
    }

    public boolean located() {
      return this.located;
    }

    public int blockX() {
      return this.blockX;
    }

    public int blockZ() {
      return this.blockZ;
    }

    public double tps() {
      return this.tps;
    }

    public double mspt() {
      return this.mspt;
    }

    public double utilisation() {
      return this.utilisation;
    }

    public int chunks() {
      return this.chunks;
    }

    public int players() {
      return this.players;
    }

    public int entities() {
      return this.entities;
    }
  }
}
