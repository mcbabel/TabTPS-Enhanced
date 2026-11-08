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
package xyz.jpenilla.tabtps.forge;

import xyz.jpenilla.tabtps.forge.gui.ForgeGui;
import xyz.jpenilla.tabtps.common.gui.Menu;
import xyz.jpenilla.tabtps.common.User;
import java.util.function.Consumer;
import java.util.Locale;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import net.minecraft.SharedConstants;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.server.permission.events.PermissionGatherEvent;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.incendo.cloud.SenderMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.jpenilla.tabtps.common.TabTPS;
import xyz.jpenilla.tabtps.common.TabTPSPlatform;
import xyz.jpenilla.tabtps.common.command.Commander;
import xyz.jpenilla.tabtps.common.command.DelegateUser;
import xyz.jpenilla.tabtps.common.command.commands.TickInfoCommand;
import xyz.jpenilla.tabtps.common.service.MobCapService;
import xyz.jpenilla.tabtps.common.service.TickTimeService;
import xyz.jpenilla.tabtps.common.service.UserService;
import xyz.jpenilla.tabtps.common.util.ConsoleAudience;
import xyz.jpenilla.tabtps.common.util.Constants;
import xyz.jpenilla.tabtps.common.util.TranslatableProvider;
import xyz.jpenilla.tabtps.forge.adventure.ForgeAudiences;
import xyz.jpenilla.tabtps.forge.command.ForgeCommandManager;
import xyz.jpenilla.tabtps.forge.command.ForgeConsoleCommander;
import xyz.jpenilla.tabtps.forge.command.ForgePingCommand;
import xyz.jpenilla.tabtps.forge.command.ForgeTickInfoCommandFormatter;
import xyz.jpenilla.tabtps.forge.service.ForgeMobCapService;
import xyz.jpenilla.tabtps.forge.service.ForgePermissions;
import xyz.jpenilla.tabtps.forge.service.ForgeTickTimeService;
import xyz.jpenilla.tabtps.forge.service.ForgeUserService;

@Mod("tabtps")
public final class TabTPSForge implements TabTPSPlatform<ServerPlayer, ForgeUser> {
  private final Logger logger = LoggerFactory.getLogger("TabTPS");
  private final Path dataDirectory = FMLPaths.CONFIGDIR.get().resolve("tabtps");
  private final ForgeAudiences audiences = new ForgeAudiences(this::server);
  private final ForgePermissions permissions = new ForgePermissions(this);
  private final ForgeMobCapService mobCapService = new ForgeMobCapService();
  private final ForgeGui gui = new ForgeGui(this);
  private final ForgeUserService userService;
  private final ForgeCommandManager commandManager;
  private final TabTPS tabTPS;
  private volatile @Nullable MinecraftServer server;
  private volatile ForgeTickTimeService tickTimeService = new ForgeTickTimeService(null);
  private int ticks;

  public TabTPSForge() {
    TranslatableProvider.MOD_JAR_OVERRIDE = ModList.get().getModFileById("tabtps").getFile().getFilePath();

    this.userService = new ForgeUserService(this);
    this.commandManager = new ForgeCommandManager(SenderMapper.create(this::commander, this::source));
    this.tabTPS = new TabTPS(this);

    TickInfoCommand.withFormatter(this.tabTPS, this.tabTPS.commands(), new ForgeTickInfoCommandFormatter(this)).register();
    new ForgePingCommand(this, this.tabTPS.commands()).register();

    MinecraftForge.EVENT_BUS.addListener((RegisterCommandsEvent event) -> this.commandManager.registerCommands(event.getDispatcher()));
    MinecraftForge.EVENT_BUS.addListener((PermissionGatherEvent.Nodes event) -> this.permissions.gather(event));
    MinecraftForge.EVENT_BUS.addListener((ServerStartedEvent event) -> this.serverStarted(event.getServer()));
    MinecraftForge.EVENT_BUS.addListener((ServerStoppedEvent event) -> this.serverStopped(event.getServer()));
    MinecraftForge.EVENT_BUS.addListener((TickEvent.ServerTickEvent event) -> {
      if (event.phase == TickEvent.Phase.START) {
        this.tickTimeService.tickStart();
      } else {
        this.tickEnd(event.getServer());
      }
    });
    MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent event) -> {
      if (event.getEntity() instanceof ServerPlayer player) {
        this.userService.handleJoin(player);
      }
    });
    MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent event) -> {
      if (event.getEntity() instanceof ServerPlayer player) {
        this.userService.handleQuit(player);
      }
    });
    MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerRespawnEvent event) -> {
      if (event.getEntity() instanceof ServerPlayer player) {
        this.userService.replacePlayer(player);
      }
    });

    if (FMLEnvironment.dist == Dist.CLIENT) {
      final Thread shutdown = new Thread(this.tabTPS::shutdown, "TabTPS-Client-Shutdown");
      Runtime.getRuntime().addShutdownHook(shutdown);
    }
  }

  private void serverStarted(final MinecraftServer server) {
    this.server = server;
    this.tickTimeService = new ForgeTickTimeService(server);
    this.tabTPS.resetUptime();
    if (this.tabTPS.configManager().pluginSettings().updateChecker()) {
      CompletableFuture.runAsync(() -> this.tabTPS.checkForUpdates(Constants.TABTPS_VERSION, "forge", SharedConstants.getCurrentVersion().getName()));
    }
  }

  private void tickEnd(final MinecraftServer server) {
    this.tickTimeService.tickEnd();
    if (++this.ticks % 20 == 0) {
      this.mobCapService.update(server);
    }
  }

  private void serverStopped(final MinecraftServer server) {
    if (server.isDedicatedServer()) {
      this.tabTPS.shutdown();
    } else {
      this.userService.flush();
    }
    this.audiences.clearBossBars();
    this.mobCapService.clear();
    this.server = null;
    this.tickTimeService = new ForgeTickTimeService(null);
  }

  private Commander commander(final CommandSourceStack source) {
    if (source.getEntity() instanceof ServerPlayer player) {
      final boolean elevated = this.elevatedSource(source, player);
      return new DelegateUser<>(this.userService.user(player), source, permission -> elevated);
    }
    return new ForgeConsoleCommander(source, ConsoleAudience.wrap(this.tabTPS, this.audiences.source(source)));
  }

  private boolean elevatedSource(final CommandSourceStack source, final ServerPlayer player) {
    final MinecraftServer current = source.getServer();
    if (current == null) {
      return false;
    }
    final int operator = current.getOperatorUserPermissionLevel();
    return source.hasPermission(operator) && !player.hasPermissions(operator);
  }

  private CommandSourceStack source(final Commander commander) {
    if (commander instanceof ForgeConsoleCommander console) {
      return console.source();
    } else if (commander instanceof DelegateUser<?, ?> user) {
      return (CommandSourceStack) user.c();
    } else if (commander instanceof ForgeUser user) {
      return user.base().createCommandSourceStack();
    }
    throw new IllegalArgumentException("Unknown commander " + commander);
  }

  public @NonNull MinecraftServer server() {
    return Objects.requireNonNull(this.server, "server is not running");
  }

  public @Nullable MinecraftServer serverOrNull() {
    return this.server;
  }

  public @NonNull ForgeAudiences audiences() {
    return this.audiences;
  }

  public @NonNull ForgePermissions permissions() {
    return this.permissions;
  }

  public @NonNull ForgeTickTimeService forgeTickTimeService() {
    return this.tickTimeService;
  }

  @Override
  public @NonNull MobCapService mobCapService() {
    return this.mobCapService;
  }

  @Override
  public @NonNull UserService<ServerPlayer, ForgeUser> userService() {
    return this.userService;
  }

  @Override
  public @NonNull Path dataDirectory() {
    return this.dataDirectory;
  }

  @Override
  public @NonNull TabTPS tabTPS() {
    return this.tabTPS;
  }

  @Override
  public @NonNull TickTimeService tickTimeService() {
    return this.tickTimeService;
  }

  @Override
  public boolean menusSupported() {
    return true;
  }

  @Override
  public void openMenu(final @NonNull User<?> user, final @NonNull Menu menu) {
    this.gui.open(user, menu);
  }

  @Override
  public void closeMenu(final @NonNull User<?> user) {
    this.gui.close(user);
  }

  @Override
  public void promptChat(final @NonNull User<?> user, final @NonNull Consumer<String> input) {
    this.gui.prompt(user, input);
  }

  @Override
  public @NonNull Locale locale(final @NonNull User<?> user) {
    return user.base() instanceof ServerPlayer ? ForgeGui.locale((ServerPlayer) user.base()) : Locale.US;
  }

  @Override
  public int maxPlayers() {
    final MinecraftServer current = this.server;
    return current == null ? 0 : current.getPlayerList().getMaxPlayers();
  }

  @Override
  public void shutdown() {
  }

  @Override
  public void onReload() {
    final MinecraftServer current = this.server;
    if (current == null) {
      return;
    }
    current.execute(() -> current.getPlayerList().getPlayers().forEach(player -> current.getCommands().sendCommands(player)));
  }

  @Override
  public @NonNull Logger logger() {
    return this.logger;
  }

  @Override
  public @NonNull ForgeCommandManager commandManager() {
    return this.commandManager;
  }
}
