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
package xyz.jpenilla.tabtps.folia.gui;

import java.util.function.Consumer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

final class PlayerTasks {
  private final Plugin plugin;
  private final boolean regionized;

  PlayerTasks(final Plugin plugin, final boolean regionized) {
    this.plugin = plugin;
    this.regionized = regionized;
  }

  void run(final Player player, final Runnable task) {
    if (this.regionized) {
      player.getScheduler().run(this.plugin, scheduled -> task.run(), null);
    } else {
      Bukkit.getScheduler().runTask(this.plugin, task);
    }
  }

  void repeat(final Player player, final Consumer<Runnable> task) {
    if (this.regionized) {
      player.getScheduler().runAtFixedRate(this.plugin, scheduled -> task.accept(scheduled::cancel), null, 20L, 20L);
    } else {
      Bukkit.getScheduler().runTaskTimer(this.plugin, scheduled -> task.accept(scheduled::cancel), 20L, 20L);
    }
  }

  void global(final Runnable task) {
    if (this.regionized) {
      Bukkit.getGlobalRegionScheduler().execute(this.plugin, task);
    } else {
      Bukkit.getScheduler().runTask(this.plugin, task);
    }
  }
}
