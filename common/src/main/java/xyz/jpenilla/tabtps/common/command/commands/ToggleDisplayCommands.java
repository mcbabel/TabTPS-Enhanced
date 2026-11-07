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
import java.util.List;
import java.util.function.Function;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.incendo.cloud.Command;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.permission.PredicatePermission;
import org.incendo.cloud.suggestion.SuggestionProvider;
import xyz.jpenilla.tabtps.common.Messages;
import xyz.jpenilla.tabtps.common.TabTPS;
import xyz.jpenilla.tabtps.common.User;
import xyz.jpenilla.tabtps.common.command.Commander;
import xyz.jpenilla.tabtps.common.command.Commands;
import xyz.jpenilla.tabtps.common.command.TabTPSCommand;
import xyz.jpenilla.tabtps.common.config.DisplayConfig;
import xyz.jpenilla.tabtps.common.display.DisplayHandler;
import xyz.jpenilla.tabtps.common.util.Constants;
import xyz.jpenilla.tabtps.common.util.TranslatableProvider;

import static net.kyori.adventure.text.Component.space;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.event.ClickEvent.runCommand;
import static net.kyori.adventure.text.format.NamedTextColor.GRAY;
import static net.kyori.adventure.text.format.NamedTextColor.GREEN;
import static net.kyori.adventure.text.format.NamedTextColor.RED;
import static net.kyori.adventure.text.format.TextDecoration.ITALIC;
import static org.incendo.cloud.minecraft.extras.RichDescription.richDescription;
import static org.incendo.cloud.parser.standard.StringParser.stringParser;

public final class ToggleDisplayCommands extends TabTPSCommand {
  public ToggleDisplayCommands(final @NonNull TabTPS tabTPS, final @NonNull Commands commands) {
    super(tabTPS, commands);
  }

  @Override
  public void register() {
    this.register("tab", DisplayConfig::tabSettings, User::tab, Messages.COMMAND_TOGGLE_TAB_DESCRIPTION, Messages.COMMAND_TOGGLE_TAB_ENABLED, Messages.COMMAND_TOGGLE_TAB_DISABLED);
    this.register("actionbar", DisplayConfig::actionBarSettings, User::actionBar, Messages.COMMAND_TOGGLE_ACTIONBAR_DESCRIPTION, Messages.COMMAND_TOGGLE_ACTIONBAR_ENABLED, Messages.COMMAND_TOGGLE_ACTIONBAR_DISABLED);
    this.register("bossbar", DisplayConfig::bossBarSettings, User::bossBar, Messages.COMMAND_TOGGLE_BOSSBAR_DESCRIPTION, Messages.COMMAND_TOGGLE_BOSSBAR_ENABLED, Messages.COMMAND_TOGGLE_BOSSBAR_DISABLED);
  }

  private void register(
    final @NonNull String display,
    final @NonNull Function<DisplayConfig, DisplayConfig.DisplaySettings> settings,
    final @NonNull Function<User<?>, DisplayHandler<?>> handler,
    final @NonNull TranslatableProvider description,
    final @NonNull TranslatableProvider enabled,
    final @NonNull TranslatableProvider disabled
  ) {
    final Command.Builder<Commander> toggle = this.commands.rootBuilder().literal("toggle").literal(display);
    final String command = "/tabtps toggle " + display;

    this.commands.register(toggle
      .senderType(User.TYPE)
      .permission(PredicatePermission.<Commander>of(sender -> !(sender instanceof User) || this.togglePermission((User<?>) sender, settings)))
      .commandDescription(richDescription(description.plain()))
      .handler(context -> this.toggle(context.sender(), handler.apply(context.sender()), command, enabled, disabled)));

    this.commands.register(toggle
      .required("player", stringParser(), SuggestionProvider.blockingStrings((context, input) -> this.playerNames()))
      .permission(Constants.PERMISSION_COMMAND_TOGGLE_OTHERS)
      .commandDescription(richDescription(description.plain()))
      .handler(context -> this.toggleOther(context, settings, handler, command, enabled, disabled)));
  }

  private @NonNull List<String> playerNames() {
    final List<String> names = new ArrayList<>();
    for (final User<?> user : this.tabTPS.platform().userService().onlineUsers()) {
      names.add(user.name());
    }
    return names;
  }

  private void toggleOther(
    final @NonNull CommandContext<Commander> context,
    final @NonNull Function<DisplayConfig, DisplayConfig.DisplaySettings> settings,
    final @NonNull Function<User<?>, DisplayHandler<?>> handler,
    final @NonNull String command,
    final @NonNull TranslatableProvider enabled,
    final @NonNull TranslatableProvider disabled
  ) {
    final String name = context.get("player");
    User<?> target = null;
    for (final User<?> user : this.tabTPS.platform().userService().onlineUsers()) {
      if (user.name().equalsIgnoreCase(name)) {
        target = user;
        break;
      }
    }
    if (target == null || !this.togglePermission(target, settings)) {
      context.sender().sendMessage(text()
        .append(Constants.PREFIX)
        .append(space())
        .append(Messages.MISC_COMMAND_TEXT_NO_PLAYERS_FOUND.styled(RED, text(name))));
      return;
    }
    final Component feedback = this.toggle(target, handler.apply(target), command, enabled, disabled);
    if (!(context.sender() instanceof User) || !((User<?>) context.sender()).uuid().equals(target.uuid())) {
      context.sender().sendMessage(feedback.append(text(" (" + target.name() + ")", GRAY)));
    }
  }

  private boolean togglePermission(final @NonNull User<?> sender, final @NonNull Function<DisplayConfig, DisplayConfig.DisplaySettings> function) {
    return this.tabTPS.findDisplayConfig(sender)
      .map(config -> function.apply(config).allow())
      .orElse(false);
  }

  private @NonNull Component toggle(
    final @NonNull User<?> user,
    final @NonNull DisplayHandler<?> display,
    final @NonNull String command,
    final @NonNull TranslatableProvider enabled,
    final @NonNull TranslatableProvider disabled
  ) {
    final Component feedback;
    if (display.enabled()) {
      display.stopDisplay();
      display.enabled(false);
      feedback = feedbackMessage(command, disabled, RED);
    } else {
      display.enabled(true);
      display.startDisplay();
      feedback = feedbackMessage(command, enabled, GREEN);
    }
    user.sendMessage(feedback);
    user.markDirty();
    return feedback;
  }

  private static @NonNull Component feedbackMessage(final @NonNull String command, final @NonNull TranslatableProvider translatable, final @NonNull TextColor color) {
    return text()
      .append(Constants.PREFIX)
      .append(space())
      .append(translatable.build(tr -> {
        tr.color(color);
        tr.decorate(ITALIC);
        tr.hoverEvent(Messages.MISC_TEXT_CLICK_TO_TOGGLE.styled(GREEN));
        tr.clickEvent(runCommand(command));
      }))
      .build();
  }
}
