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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.incendo.cloud.context.CommandContext;
import xyz.jpenilla.tabtps.common.Messages;
import xyz.jpenilla.tabtps.common.TabTPS;
import xyz.jpenilla.tabtps.common.User;
import xyz.jpenilla.tabtps.common.command.Commander;
import xyz.jpenilla.tabtps.common.command.Commands;
import xyz.jpenilla.tabtps.common.command.TabTPSCommand;
import xyz.jpenilla.tabtps.common.config.Theme;
import xyz.jpenilla.tabtps.common.service.RegionStatsService;
import xyz.jpenilla.tabtps.common.util.Components;
import xyz.jpenilla.tabtps.common.util.Constants;
import xyz.jpenilla.tabtps.common.util.TPSUtil;

import static net.kyori.adventure.text.Component.space;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.event.ClickEvent.runCommand;
import static net.kyori.adventure.text.format.NamedTextColor.DARK_GRAY;
import static net.kyori.adventure.text.format.NamedTextColor.GRAY;
import static net.kyori.adventure.text.format.NamedTextColor.GREEN;
import static net.kyori.adventure.text.format.NamedTextColor.RED;
import static net.kyori.adventure.text.format.NamedTextColor.WHITE;
import static net.kyori.adventure.text.format.TextDecoration.ITALIC;
import static org.incendo.cloud.minecraft.extras.RichDescription.richDescription;
import static org.incendo.cloud.parser.standard.IntegerParser.integerParser;
import static org.incendo.cloud.parser.standard.StringParser.stringParser;

public final class RegionsCommand extends TabTPSCommand {
  private static final int DEFAULT_COUNT = 10;

  private final RegionStatsService regions;

  public RegionsCommand(final @NonNull TabTPS tabTPS, final @NonNull Commands commands, final @NonNull RegionStatsService regions) {
    super(tabTPS, commands);
    this.regions = regions;
  }

  @Override
  public void register() {
    this.commands.registerSubcommand(builder -> builder.literal("regions")
      .optional("count", integerParser(1, 100))
      .permission(Constants.PERMISSION_COMMAND_REGIONS)
      .commandDescription(richDescription(Messages.COMMAND_REGIONS_DESCRIPTION.plain()))
      .handler(this::executeList));

    this.commands.registerSubcommand(builder -> builder.literal("regions")
      .literal("tp")
      .required("world", stringParser())
      .required("x", integerParser())
      .required("z", integerParser())
      .senderType(User.TYPE)
      .permission(Constants.PERMISSION_COMMAND_REGIONS_TELEPORT)
      .commandDescription(richDescription(Messages.COMMAND_REGIONS_TELEPORT_DESCRIPTION.plain()))
      .handler(this::executeTeleport));
  }

  private void executeList(final @NonNull CommandContext<Commander> ctx) {
    final int count = ctx.<Integer>optional("count").orElse(DEFAULT_COUNT);
    final List<RegionStatsService.Region> all = new ArrayList<>(this.regions.regions());
    if (all.isEmpty()) {
      ctx.sender().sendMessage(Components.ofChildren(
        Constants.PREFIX,
        space(),
        Messages.COMMAND_REGIONS_TEXT_NONE.styled(RED)
      ));
      return;
    }
    all.sort(Comparator.comparingDouble(RegionsCommand::load).reversed());

    final boolean canTeleport = ctx.sender() instanceof User && ctx.sender().hasPermission(Constants.PERMISSION_COMMAND_REGIONS_TELEPORT);
    ctx.sender().sendMessage(Components.ofChildren(
      Constants.PREFIX,
      space(),
      Messages.COMMAND_REGIONS_TEXT_HEADER.styled(GRAY, ITALIC, text(Math.min(count, all.size()), WHITE), text(all.size(), WHITE))
    ));
    final Theme.Colors colors = Theme.DEFAULT.colorScheme();
    for (int i = 0; i < Math.min(count, all.size()); i++) {
      ctx.sender().sendMessage(this.line(i + 1, all.get(i), colors, canTeleport));
    }
  }

  private @NonNull Component line(final int index, final RegionStatsService.@NonNull Region region, final Theme.@NonNull Colors colors, final boolean canTeleport) {
    final String location = region.located()
      ? region.world() + " " + region.blockX() + ", " + region.blockZ()
      : region.world();
    final TextComponent.Builder builder = text()
      .append(text(" " + index + ". ", DARK_GRAY))
      .append(text(location, WHITE))
      .append(text("  TPS ", GRAY))
      .append(TPSUtil.coloredTps(region.tps(), colors))
      .append(text("  MSPT ", GRAY))
      .append(TPSUtil.coloredMspt(region.mspt(), colors));
    if (region.utilisation() >= 0) {
      builder.append(text("  " + Math.round(region.utilisation() * 100) + "%", GRAY));
    }
    if (region.chunks() >= 0) {
      builder.append(text("\n    ", GRAY))
        .append(Messages.COMMAND_REGIONS_TEXT_STATS.styled(GRAY, text(region.chunks(), WHITE), text(region.players(), WHITE), text(region.entities(), WHITE)));
    }
    if (canTeleport && region.located()) {
      builder.clickEvent(runCommand("/tabtps regions tp " + region.world() + " " + region.blockX() + " " + region.blockZ()))
        .hoverEvent(Messages.COMMAND_REGIONS_TEXT_CLICK_TO_TELEPORT.styled(GREEN));
    }
    return builder.build();
  }

  private void executeTeleport(final @NonNull CommandContext<User<?>> ctx) {
    final String world = ctx.get("world");
    final int x = ctx.get("x");
    final int z = ctx.get("z");
    if (!this.regions.teleport(ctx.sender(), world, x, z)) {
      ctx.sender().sendMessage(Components.ofChildren(
        Constants.PREFIX,
        space(),
        Messages.COMMAND_REGIONS_TEXT_TELEPORT_FAILED.styled(RED, text(world))
      ));
    }
  }

  private static double load(final RegionStatsService.@NonNull Region region) {
    return region.utilisation() >= 0 ? region.utilisation() : region.mspt() / 50.0;
  }
}
