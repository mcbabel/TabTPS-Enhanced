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

import com.mojang.brigadier.arguments.StringArgumentType;
import io.leangen.geantyref.TypeToken;
import java.util.stream.Collectors;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.parser.ParserDescriptor;
import xyz.jpenilla.tabtps.common.command.Commander;
import xyz.jpenilla.tabtps.common.command.Commands;
import xyz.jpenilla.tabtps.common.command.commands.PingCommand;
import xyz.jpenilla.tabtps.forge.TabTPSForge;

public final class ForgePingCommand extends PingCommand {
  private final TabTPSForge platform;

  public ForgePingCommand(final @NonNull TabTPSForge platform, final @NonNull Commands commands) {
    super(platform.tabTPS(), commands);
    this.platform = platform;
  }

  @Override
  public void register() {
    this.platform.commandManager().brigadierManager().registerMapping(
      new TypeToken<PlayerTargets.Parser>() {},
      builder -> builder.toConstant(StringArgumentType.word()).cloudSuggestions()
    );
    this.registerPingTargetsCommand(ParserDescriptor.of(new PlayerTargets.Parser(this.platform), PlayerTargets.class), this::onPingTargets);
  }

  private void onPingTargets(final @NonNull CommandContext<Commander> context) {
    final PlayerTargets targets = context.get("target");
    this.pingTargets(
      context.sender(),
      targets.players().stream()
        .map(this.platform.userService()::user)
        .collect(Collectors.toList()),
      targets.input(),
      context.get("page")
    );
  }
}
