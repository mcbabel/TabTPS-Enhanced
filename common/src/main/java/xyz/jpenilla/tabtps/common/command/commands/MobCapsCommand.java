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
package xyz.jpenilla.tabtps.common.command.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.incendo.cloud.context.CommandContext;
import xyz.jpenilla.tabtps.common.Messages;
import xyz.jpenilla.tabtps.common.TabTPS;
import xyz.jpenilla.tabtps.common.command.Commander;
import xyz.jpenilla.tabtps.common.command.Commands;
import xyz.jpenilla.tabtps.common.command.TabTPSCommand;
import xyz.jpenilla.tabtps.common.config.Theme;
import xyz.jpenilla.tabtps.common.service.MobCapService;
import xyz.jpenilla.tabtps.common.util.Components;
import xyz.jpenilla.tabtps.common.util.Constants;

import static net.kyori.adventure.text.Component.space;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.GRAY;
import static net.kyori.adventure.text.format.NamedTextColor.RED;
import static net.kyori.adventure.text.format.NamedTextColor.WHITE;
import static net.kyori.adventure.text.format.TextDecoration.ITALIC;
import static org.incendo.cloud.minecraft.extras.RichDescription.richDescription;

public final class MobCapsCommand extends TabTPSCommand {
  public MobCapsCommand(final @NonNull TabTPS tabTPS, final @NonNull Commands commands) {
    super(tabTPS, commands);
  }

  @Override
  public void register() {
    this.commands.registerSubcommand(builder -> builder.literal("mobcaps")
      .permission(Constants.PERMISSION_COMMAND_MOBCAPS)
      .commandDescription(richDescription(Messages.COMMAND_MOBCAPS_DESCRIPTION.plain()))
      .handler(this::executeMobCaps));
  }

  private void executeMobCaps(final @NonNull CommandContext<Commander> ctx) {
    final MobCapService service = this.tabTPS.platform().mobCapService();
    if (service == null || service.worlds().isEmpty()) {
      ctx.sender().sendMessage(Components.ofChildren(
        Constants.PREFIX,
        space(),
        Messages.COMMAND_MOBCAPS_TEXT_UNSUPPORTED.styled(RED)
      ));
      return;
    }
    ctx.sender().sendMessage(Components.ofChildren(
      Constants.PREFIX,
      space(),
      Messages.COMMAND_MOBCAPS_TEXT_HEADER.styled(GRAY, ITALIC)
    ));
    final Theme.Colors colors = Theme.DEFAULT.colorScheme();
    for (final MobCapService.WorldMobCaps world : service.worlds()) {
      ctx.sender().sendMessage(text(" " + world.name(), WHITE));
      for (final MobCapService.Category category : world.categories()) {
        ctx.sender().sendMessage(Components.ofChildren(
          text("   " + category.name() + ": ", GRAY),
          count(category.current(), category.max(), colors),
          text("/", colors.textSecondary()),
          Components.gradient(String.valueOf(category.max()), colors.goodPerformance(), colors.goodPerformanceSecondary())
        ));
      }
    }
  }

  private static @NonNull Component count(final int current, final int max, final Theme.@NonNull Colors colors) {
    final TextColor first;
    final TextColor second;
    if (max > 0 && current >= max) {
      first = colors.lowPerformance();
      second = colors.lowPerformanceSecondary();
    } else if (max > 0 && current * 4 >= max * 3) {
      first = colors.mediumPerformance();
      second = colors.mediumPerformanceSecondary();
    } else {
      first = colors.goodPerformance();
      second = colors.goodPerformanceSecondary();
    }
    return Components.gradient(String.valueOf(current), first, second);
  }
}
