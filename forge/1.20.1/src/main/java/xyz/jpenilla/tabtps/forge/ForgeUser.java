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

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.tabtps.common.AbstractUser;

@DefaultQualifier(NonNull.class)
public final class ForgeUser extends AbstractUser<ServerPlayer> {
  private final TabTPSForge platform;
  private final Audience audience;

  private ForgeUser(final TabTPSForge platform, final ServerPlayer player) {
    super(platform.tabTPS(), player, player.getUUID());
    this.platform = platform;
    this.audience = platform.audiences().player(player);
  }

  public static ForgeUser from(final TabTPSForge platform, final ServerPlayer player) {
    return new ForgeUser(platform, player);
  }

  @Override
  public String name() {
    return this.base().getGameProfile().getName();
  }

  @Override
  public Component displayName() {
    return this.platform.audiences().toAdventure(this.base().getDisplayName());
  }

  @Override
  public boolean online() {
    final MinecraftServer server = this.platform.serverOrNull();
    return server != null && server.getPlayerList().getPlayer(this.uuid()) == this.base();
  }

  @Override
  public int ping() {
    return this.base().latency;
  }

  @Override
  public boolean hasPermission(final String permissionString) {
    return this.platform.permissions().hasPermission(this.base(), permissionString);
  }

  @Override
  public Audience audience() {
    return this.audience;
  }
}
