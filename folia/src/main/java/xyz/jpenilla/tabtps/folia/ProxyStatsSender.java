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
package xyz.jpenilla.tabtps.folia;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;
import xyz.jpenilla.tabtps.common.service.RegionStatsService;
import xyz.jpenilla.tabtps.common.service.TickTimeService;

final class ProxyStatsSender {
  static final String CHANNEL = "tabtps:stats";
  private static final int PROTOCOL = 1;

  private final TabTPSFolia plugin;
  private final boolean regionized;
  private final String software;

  ProxyStatsSender(final @NonNull TabTPSFolia plugin, final boolean regionized, final @NonNull String software) {
    this.plugin = plugin;
    this.regionized = regionized;
    this.software = software;
  }

  void start() {
    this.plugin.getServer().getMessenger().registerOutgoingPluginChannel(this.plugin, CHANNEL);
    this.plugin.tabTPS().executor().scheduleAtFixedRate(this::sendSafely, 5, 5, TimeUnit.SECONDS);
  }

  private void sendSafely() {
    try {
      this.send();
    } catch (final IOException | RuntimeException e) {
      this.plugin.logger().debug("Failed to send stats to the proxy", e);
    }
  }

  private void send() throws IOException {
    if (!this.plugin.tabTPS().configManager().pluginSettings().sendStatsToProxy()) {
      return;
    }
    final Iterator<? extends Player> players = this.plugin.getServer().getOnlinePlayers().iterator();
    if (!players.hasNext()) {
      return;
    }
    final Player player = players.next();
    final byte[] data = this.payload();
    if (this.regionized) {
      player.getScheduler().run(this.plugin, task -> player.sendPluginMessage(this.plugin, CHANNEL, data), null);
    } else {
      this.plugin.getServer().getScheduler().runTask(this.plugin, () -> {
        if (player.isOnline()) {
          player.sendPluginMessage(this.plugin, CHANNEL, data);
        }
      });
    }
  }

  private byte @NonNull [] payload() throws IOException {
    final TickTimeService ticks = this.plugin.tickTimeService();
    final RegionStatsService regionStats = this.plugin.regionStatsService();
    final List<RegionStatsService.Region> regions = regionStats == null ? null : regionStats.regions();

    final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    try (DataOutputStream out = new DataOutputStream(bytes)) {
      out.writeByte(PROTOCOL);
      out.writeDouble(ticks.displayTps());
      out.writeDouble(ticks.averageMspt());
      out.writeInt(this.plugin.userService().onlinePlayers());
      out.writeInt(this.plugin.maxPlayers());
      out.writeInt(regions == null ? -1 : regions.size());
      out.writeDouble(regionStats == null ? -1 : regionStats.lowestRegionTps());
      out.writeLong(this.plugin.tabTPS().uptime().getSeconds());
      out.writeUTF(this.software);
    }
    return bytes.toByteArray();
  }
}
