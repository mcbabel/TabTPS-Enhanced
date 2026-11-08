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

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;

import static net.kyori.adventure.text.Component.newline;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.AQUA;
import static net.kyori.adventure.text.format.NamedTextColor.BLUE;
import static net.kyori.adventure.text.format.NamedTextColor.DARK_GRAY;
import static net.kyori.adventure.text.format.NamedTextColor.GRAY;
import static net.kyori.adventure.text.format.NamedTextColor.WHITE;

final class NetworkCommand implements SimpleCommand {
  static final String PERMISSION = "tabtps.network";
  private static final long STALE_MILLIS = Duration.ofSeconds(30).toMillis();
  private static final Component PREFIX = text()
    .append(text("[", WHITE), text("TabTPS", BLUE), text("]", WHITE))
    .build();

  private final ProxyServer server;
  private final Map<String, ServerStats> stats;

  NetworkCommand(final ProxyServer server, final Map<String, ServerStats> stats) {
    this.server = server;
    this.stats = stats;
  }

  @Override
  public boolean hasPermission(final Invocation invocation) {
    return invocation.source().hasPermission(PERMISSION);
  }

  @Override
  public void execute(final Invocation invocation) {
    final CommandSource source = invocation.source();
    final List<RegisteredServer> servers = new ArrayList<>(this.server.getAllServers());
    servers.sort(Comparator.comparing(registered -> registered.getServerInfo().getName()));
    source.sendMessage(text()
      .append(PREFIX, text(" Network: ", GRAY))
      .append(text(servers.size(), WHITE), text(servers.size() == 1 ? " server, " : " servers, ", GRAY))
      .append(text(this.server.getPlayerCount(), WHITE), text(this.server.getPlayerCount() == 1 ? " player" : " players", GRAY))
      .build());
    final long now = System.currentTimeMillis();
    for (final RegisteredServer registered : servers) {
      source.sendMessage(this.line(registered, now, source instanceof Player));
    }
  }

  private Component line(final RegisteredServer registered, final long now, final boolean clickable) {
    final String name = registered.getServerInfo().getName();
    final int players = registered.getPlayersConnected().size();
    final ServerStats stats = this.stats.get(name);
    final TextComponent.Builder line = text().append(text(" " + name, WHITE));

    if (stats == null) {
      line.append(text("  " + players + (players == 1 ? " player" : " players"), GRAY))
        .append(text("  no data", DARK_GRAY));
    } else {
      final long age = now - stats.receivedAt();
      line.append(text("  TPS ", GRAY), text(format(stats.tps()), tpsColor(stats.tps())))
        .append(text("  MSPT ", GRAY), text(format(stats.mspt()), msptColor(stats.mspt())))
        .append(text("  " + players + "/" + stats.maxPlayers(), GRAY));
      if (stats.regions() >= 0) {
        line.append(text("  " + stats.regions() + " regions, slowest ", GRAY), text(format(stats.lowestRegionTps()), tpsColor(stats.lowestRegionTps())));
      }
      if (age > STALE_MILLIS) {
        line.append(text("  (" + duration(age / 1000) + " old)", DARK_GRAY));
      }
      line.hoverEvent(HoverEvent.showText(text()
        .append(text(stats.software(), AQUA))
        .append(newline(), text("Uptime: ", GRAY), text(duration(stats.uptimeSeconds()), WHITE))
        .append(clickable ? Component.empty().append(newline(), text("Click to join", GRAY)) : Component.empty())
        .build()));
    }
    if (clickable) {
      line.clickEvent(ClickEvent.runCommand("/server " + name));
    }
    return line.build();
  }

  private static String format(final double value) {
    return String.format(Locale.ROOT, "%.2f", value);
  }

  private static TextColor tpsColor(final double tps) {
    if (tps >= 19.5) {
      return NamedTextColor.GREEN;
    }
    return tps >= 15 ? NamedTextColor.YELLOW : NamedTextColor.RED;
  }

  private static TextColor msptColor(final double mspt) {
    if (mspt <= 40) {
      return NamedTextColor.GREEN;
    }
    return mspt <= 50 ? NamedTextColor.YELLOW : NamedTextColor.RED;
  }

  private static String duration(final long seconds) {
    final long days = seconds / 86400;
    final long hours = seconds % 86400 / 3600;
    final long minutes = seconds % 3600 / 60;
    if (days > 0) {
      return days + "d " + hours + "h";
    }
    if (hours > 0) {
      return hours + "h " + minutes + "m";
    }
    return minutes > 0 ? minutes + "m" : seconds + "s";
  }
}
