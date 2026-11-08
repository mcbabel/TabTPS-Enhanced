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
package xyz.jpenilla.tabtps.common;

import java.io.IOException;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.stream.Stream;
import net.kyori.adventure.text.event.ClickEvent;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import xyz.jpenilla.tabtps.common.command.Commands;
import xyz.jpenilla.tabtps.common.command.TabTPSCommand;
import xyz.jpenilla.tabtps.common.command.commands.AboutCommand;
import xyz.jpenilla.tabtps.common.command.commands.GuiCommand;
import xyz.jpenilla.tabtps.common.command.commands.HelpCommand;
import xyz.jpenilla.tabtps.common.command.commands.MemoryCommand;
import xyz.jpenilla.tabtps.common.command.commands.MobCapsCommand;
import xyz.jpenilla.tabtps.common.command.commands.PingCommand;
import xyz.jpenilla.tabtps.common.command.commands.RegionsCommand;
import xyz.jpenilla.tabtps.common.command.commands.ReloadCommand;
import xyz.jpenilla.tabtps.common.command.commands.ToggleDisplayCommands;
import xyz.jpenilla.tabtps.common.config.ConfigManager;
import xyz.jpenilla.tabtps.common.config.DisplayConfig;
import xyz.jpenilla.tabtps.common.config.PluginSettings;
import xyz.jpenilla.tabtps.common.service.RegionStatsService;
import xyz.jpenilla.tabtps.common.util.CPUMonitor;
import xyz.jpenilla.tabtps.common.util.Components;
import xyz.jpenilla.tabtps.common.util.Constants;
import xyz.jpenilla.tabtps.common.util.DaemonThreadFactory;
import xyz.jpenilla.tabtps.common.util.LagAlerts;
import xyz.jpenilla.tabtps.common.util.UpdateChecker;

import static net.kyori.adventure.text.Component.space;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.AQUA;
import static net.kyori.adventure.text.format.NamedTextColor.DARK_GRAY;
import static net.kyori.adventure.text.format.NamedTextColor.GRAY;
import static net.kyori.adventure.text.format.NamedTextColor.GREEN;
import static net.kyori.adventure.text.format.NamedTextColor.YELLOW;

public final class TabTPS {
  private final TabTPSPlatform<?, ?> platform;
  private final CPUMonitor cpuMonitor;
  private final ConfigManager configManager;
  private final ScheduledExecutorService executor;
  private final Commands commands;
  private volatile long startedAt = System.currentTimeMillis();
  private volatile UpdateChecker.@Nullable Update availableUpdate;

  public TabTPS(final @NonNull TabTPSPlatform<?, ?> platform) {
    this.platform = platform;
    try {
      Messages.load();
      this.configManager = new ConfigManager(platform.dataDirectory());
      this.configManager.load();
      final ScheduledThreadPoolExecutor ex = new ScheduledThreadPoolExecutor(4, new DaemonThreadFactory("TabTPS-Worker"));
      ex.setRemoveOnCancelPolicy(true);
      this.executor = Executors.unconfigurableScheduledExecutorService(ex);
      this.commands = new Commands(this, platform.commandManager());
      this.registerCommands();
      this.cpuMonitor = new CPUMonitor();
      new LagAlerts(this).start();
    } catch (final IOException e) {
      this.platform.shutdown();
      this.shutdown();
      throw initializationFailed(e);
    }
  }

  public void shutdown() {
    if (this.cpuMonitor != null) {
      this.cpuMonitor.shutdown();
    }
    if (this.executor != null) {
      this.executor.shutdown();
    }
    this.platform.userService().flush();
  }

  public synchronized void reload() {
    try {
      this.configManager().load();
    } catch (final IOException e) {
      throw new IllegalStateException("Failed to reload configs", e);
    }
    this.platform.userService().reload();
    this.platform().onReload();
  }

  private void registerCommands() {
    Stream.of(
      new HelpCommand(this, this.commands),
      new ReloadCommand(this, this.commands),
      new ToggleDisplayCommands(this, this.commands),
      new AboutCommand(this, this.commands),
      new PingCommand(this, this.commands),
      new MemoryCommand(this, this.commands),
      new MobCapsCommand(this, this.commands),
      new GuiCommand(this, this.commands)
    ).forEach(TabTPSCommand::register);
    final RegionStatsService regionStats = this.platform.regionStatsService();
    if (regionStats != null) {
      new RegionsCommand(this, this.commands, regionStats).register();
    }
  }

  public @NonNull TabTPSPlatform<?, ?> platform() {
    return this.platform;
  }

  public @NonNull ConfigManager configManager() {
    return this.configManager;
  }

  public @NonNull Optional<DisplayConfig> findDisplayConfig(final @NonNull User<?> player) {
    for (final String permission : this.configManager.pluginSettings().permissionPriorities()) {
      if (player.hasPermission(permission) || permission.isEmpty()) {
        return Optional.of(this.configManager.displayConfigsByPermission().get(permission));
      }
    }
    return Optional.empty();
  }

  public @NonNull Duration uptime() {
    return Duration.ofMillis(System.currentTimeMillis() - this.startedAt);
  }

  public void checkForUpdates(final @NonNull String version, final @NonNull String loader, final @NonNull String minecraftVersion) {
    try {
      final UpdateChecker.Result result = UpdateChecker.check(version, loader, minecraftVersion);
      result.lines().forEach(this.platform.logger()::info);
      this.availableUpdate = result.update();
    } catch (final RuntimeException | LinkageError e) {
      this.platform.logger().warn("Failed to look for updates", e);
    }
  }

  public void notifyAboutUpdate(final @NonNull User<?> user) {
    final UpdateChecker.Update update = this.availableUpdate;
    final PluginSettings settings = this.configManager.pluginSettings();
    if (update == null || !settings.updateChecker() || !settings.updateNotifyOnJoin() || !user.hasPermission(Constants.PERMISSION_UPDATE_NOTIFY)) {
      return;
    }
    user.sendMessage(Components.ofChildren(
      Constants.PREFIX,
      space(),
      Messages.MISC_TEXT_UPDATE_AVAILABLE.styled(GRAY, text("v" + update.latest(), GREEN), text("v" + update.current(), YELLOW)),
      space(),
      text()
        .append(text("[", DARK_GRAY), text("Modrinth", AQUA), text("]", DARK_GRAY))
        .clickEvent(ClickEvent.openUrl(update.url()))
        .hoverEvent(Messages.MISC_TEXT_UPDATE_DOWNLOAD.styled(GREEN))
        .build()
    ));
  }

  public void resetUptime() {
    this.startedAt = System.currentTimeMillis();
  }

  public @NonNull ScheduledExecutorService executor() {
    return this.executor;
  }

  public @NonNull CPUMonitor cpuMonitor() {
    return this.cpuMonitor;
  }

  public @NonNull Commands commands() {
    return this.commands;
  }

  private static @NonNull IllegalStateException initializationFailed(final Throwable cause) {
    return new IllegalStateException("Failed to initialize TabTPS", cause);
  }
}
