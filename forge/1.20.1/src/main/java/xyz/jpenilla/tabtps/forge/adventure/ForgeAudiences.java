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
package xyz.jpenilla.tabtps.forge.adventure;

import com.google.gson.JsonElement;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.json.JSONOptions;
import net.kyori.adventure.translation.GlobalTranslator;
import net.minecraft.SharedConstants;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;

@DefaultQualifier(NonNull.class)
public final class ForgeAudiences {
  static final Locale DEFAULT_LOCALE = Locale.US;

  private final Supplier<MinecraftServer> server;
  private final GsonComponentSerializer serializer = GsonComponentSerializer.builder()
    .options(JSONOptions.byDataVersion().at(SharedConstants.getCurrentVersion().getDataVersion().getVersion()))
    .build();
  private final Map<BossBar, BossBarBridge> bossBars = new ConcurrentHashMap<>();

  public ForgeAudiences(final Supplier<MinecraftServer> server) {
    this.server = server;
  }

  MinecraftServer server() {
    return this.server.get();
  }

  public net.minecraft.network.chat.Component toNative(final Component component, final Locale locale) {
    final JsonElement json = this.serializer.serializeToTree(GlobalTranslator.render(component, locale));
    return net.minecraft.network.chat.Component.Serializer.fromJson(json);
  }

  public Component toAdventure(final net.minecraft.network.chat.Component component) {
    final JsonElement json = net.minecraft.network.chat.Component.Serializer.toJsonTree(component);
    return this.serializer.deserializeFromTree(json);
  }

  public Audience player(final ServerPlayer player) {
    return new PlayerAudience(this, player);
  }

  public Audience source(final CommandSourceStack source) {
    return new SourceAudience(this, source);
  }

  void showBossBar(final ServerPlayer player, final BossBar bar) {
    this.server().execute(() -> this.bossBars.computeIfAbsent(bar, key -> new BossBarBridge(this, key)).event().addPlayer(player));
  }

  void hideBossBar(final ServerPlayer player, final BossBar bar) {
    this.server().execute(() -> {
      final BossBarBridge bridge = this.bossBars.get(bar);
      if (bridge == null) {
        return;
      }
      bridge.event().removePlayer(player);
      if (bridge.event().getPlayers().isEmpty()) {
        this.bossBars.remove(bar);
        bridge.dispose();
      }
    });
  }

  public void clearBossBars() {
    this.bossBars.values().forEach(BossBarBridge::dispose);
    this.bossBars.clear();
  }
}
