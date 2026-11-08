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
package xyz.jpenilla.tabtps.forge.service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permission;
import net.minecraftforge.server.permission.PermissionAPI;
import net.minecraftforge.server.permission.events.PermissionGatherEvent;
import net.minecraftforge.server.permission.nodes.PermissionNode;
import net.minecraftforge.server.permission.nodes.PermissionTypes;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.incendo.cloud.Command;
import xyz.jpenilla.tabtps.common.command.Commander;
import xyz.jpenilla.tabtps.common.util.Constants;
import xyz.jpenilla.tabtps.forge.TabTPSForge;

public final class ForgePermissions {
  private final TabTPSForge platform;
  private final Map<String, PermissionNode<Boolean>> nodes = new ConcurrentHashMap<>();

  public ForgePermissions(final TabTPSForge platform) {
    this.platform = platform;
  }

  public void gather(final PermissionGatherEvent.Nodes event) {
    final Set<String> permissions = new LinkedHashSet<>();
    permissions.add(Constants.PERMISSION_COMMAND_ERROR_HOVER_STACKTRACE);
    permissions.add(Constants.PERMISSION_UPDATE_NOTIFY);
    permissions.add(Constants.PERMISSION_LAG_ALERT);
    for (final Command<Commander> command : this.platform.commandManager().commands()) {
      permissions.add(command.commandPermission().permissionString());
    }
    permissions.addAll(this.platform.tabTPS().configManager().displayConfigsByPermission().keySet());

    final List<PermissionNode<?>> created = new ArrayList<>();
    for (final String permission : permissions) {
      final PermissionNode<Boolean> node = this.node(permission);
      if (node != null) {
        this.nodes.put(permission, node);
        created.add(node);
      }
    }
    event.addNodes(created);
  }

  private @Nullable PermissionNode<Boolean> node(final String permission) {
    final int dot = permission.indexOf('.');
    if (dot <= 0 || dot == permission.length() - 1) {
      return null;
    }
    try {
      return new PermissionNode<>(
        permission.substring(0, dot),
        permission.substring(dot + 1),
        PermissionTypes.BOOLEAN,
        (player, uuid, contexts) -> player != null && this.byDefault(player)
      );
    } catch (final RuntimeException e) {
      this.platform.logger().warn("Cannot register permission node '{}': {}", permission, e.getMessage());
      return null;
    }
  }

  public boolean hasPermission(final ServerPlayer player, final String permission) {
    if (permission.isEmpty()) {
      return true;
    }
    final PermissionNode<Boolean> node = this.nodes.get(permission);
    if (node != null) {
      try {
        return PermissionAPI.getPermission(player, node);
      } catch (final RuntimeException ignored) {
      }
    }
    return this.byDefault(player);
  }

  private boolean byDefault(final ServerPlayer player) {
    final MinecraftServer server = this.platform.serverOrNull();
    if (server == null) {
      return false;
    }
    if (!server.isDedicatedServer() && server.isSingleplayerOwner(player.nameAndId())) {
      return true;
    }
    return player.permissions().hasPermission(new Permission.HasCommandLevel(server.operatorUserPermissions().level()));
  }
}
