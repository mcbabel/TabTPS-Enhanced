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
package xyz.jpenilla.tabtps.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;

public final class TabTPSVelocity {
  static final MinecraftChannelIdentifier CHANNEL = MinecraftChannelIdentifier.create("tabtps", "stats");
  private static final int PROTOCOL = 1;

  private final ProxyServer server;
  private final Logger logger;
  private final Map<String, ServerStats> stats = new ConcurrentHashMap<>();

  @Inject
  public TabTPSVelocity(final ProxyServer server, final Logger logger) {
    this.server = server;
    this.logger = logger;
  }

  @Subscribe
  public void onInitialize(final ProxyInitializeEvent event) {
    this.server.getChannelRegistrar().register(CHANNEL);
    this.server.getCommandManager().register(
      this.server.getCommandManager().metaBuilder("networktps").aliases("ntps").plugin(this).build(),
      new NetworkCommand(this.server, this.stats)
    );
  }

  @Subscribe
  public void onPluginMessage(final PluginMessageEvent event) {
    if (!CHANNEL.equals(event.getIdentifier())) {
      return;
    }
    event.setResult(PluginMessageEvent.ForwardResult.handled());
    if (!(event.getSource() instanceof ServerConnection)) {
      return;
    }
    final String name = ((ServerConnection) event.getSource()).getServerInfo().getName();
    try {
      this.stats.put(name, read(event.getData()));
    } catch (final IOException | RuntimeException e) {
      this.logger.debug("Ignoring malformed stats from {}", name, e);
    }
  }

  private static ServerStats read(final byte[] data) throws IOException {
    try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(data))) {
      final int protocol = in.readUnsignedByte();
      if (protocol != PROTOCOL) {
        throw new IOException("Unknown protocol " + protocol);
      }
      return new ServerStats(
        in.readDouble(),
        in.readDouble(),
        in.readInt(),
        in.readInt(),
        in.readInt(),
        in.readDouble(),
        in.readLong(),
        in.readUTF(),
        System.currentTimeMillis()
      );
    }
  }
}
