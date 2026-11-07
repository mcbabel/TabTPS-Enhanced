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
package xyz.jpenilla.tabtps.forge.command;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.context.CommandInput;
import org.incendo.cloud.parser.ArgumentParseResult;
import org.incendo.cloud.parser.ArgumentParser;
import org.incendo.cloud.suggestion.BlockingSuggestionProvider;
import xyz.jpenilla.tabtps.common.command.Commander;
import xyz.jpenilla.tabtps.forge.TabTPSForge;

@DefaultQualifier(NonNull.class)
public record PlayerTargets(String input, List<ServerPlayer> players) {
  public static final String ALL = "*";

  public static final class Parser implements ArgumentParser<Commander, PlayerTargets>, BlockingSuggestionProvider.Strings<Commander> {
    private final TabTPSForge platform;

    public Parser(final TabTPSForge platform) {
      this.platform = platform;
    }

    @Override
    public ArgumentParseResult<PlayerTargets> parse(final CommandContext<Commander> context, final CommandInput input) {
      final String name = input.readString();
      final MinecraftServer server = this.platform.serverOrNull();
      if (server == null) {
        return ArgumentParseResult.success(new PlayerTargets(name, List.of()));
      }
      if (name.equals(ALL)) {
        return ArgumentParseResult.success(new PlayerTargets(name, List.copyOf(server.getPlayerList().getPlayers())));
      }
      final ServerPlayer player = server.getPlayerList().getPlayerByName(name);
      return ArgumentParseResult.success(new PlayerTargets(name, player == null ? List.of() : List.of(player)));
    }

    @Override
    public Iterable<String> stringSuggestions(final CommandContext<Commander> context, final CommandInput input) {
      final List<String> names = new ArrayList<>();
      names.add(ALL);
      final MinecraftServer server = this.platform.serverOrNull();
      if (server != null) {
        final String prefix = input.peekString().toLowerCase(Locale.ROOT);
        for (final ServerPlayer player : server.getPlayerList().getPlayers()) {
          final String name = player.getGameProfile().getName();
          if (name.toLowerCase(Locale.ROOT).startsWith(prefix)) {
            names.add(name);
          }
        }
      }
      return names;
    }
  }
}
