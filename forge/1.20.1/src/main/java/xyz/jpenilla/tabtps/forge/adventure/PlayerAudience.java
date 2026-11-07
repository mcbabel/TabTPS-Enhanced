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

import java.util.Locale;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.audience.MessageType;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.translation.Translator;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.network.protocol.game.ClientboundTabListPacket;
import net.minecraft.server.level.ServerPlayer;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;

@DefaultQualifier(NonNull.class)
final class PlayerAudience implements Audience {
  private final ForgeAudiences audiences;
  private final ServerPlayer player;
  private Component header = Component.empty();
  private Component footer = Component.empty();

  PlayerAudience(final ForgeAudiences audiences, final ServerPlayer player) {
    this.audiences = audiences;
    this.player = player;
  }

  private Locale locale() {
    final Locale locale = Translator.parseLocale(this.player.getLanguage());
    return locale == null ? ForgeAudiences.DEFAULT_LOCALE : locale;
  }

  private net.minecraft.network.chat.Component toNative(final Component component) {
    return this.audiences.toNative(component, this.locale());
  }

  @Override
  public void sendMessage(final Component message) {
    this.player.sendSystemMessage(this.toNative(message));
  }

  @Override
  @SuppressWarnings("deprecation")
  public void sendMessage(final Identity source, final Component message, final MessageType type) {
    this.sendMessage(message);
  }

  @Override
  public void sendActionBar(final Component message) {
    this.player.connection.send(new ClientboundSetActionBarTextPacket(this.toNative(message)));
  }

  @Override
  public synchronized void sendPlayerListHeader(final Component header) {
    this.header = header;
    this.sendTabList();
  }

  @Override
  public synchronized void sendPlayerListFooter(final Component footer) {
    this.footer = footer;
    this.sendTabList();
  }

  @Override
  public synchronized void sendPlayerListHeaderAndFooter(final Component header, final Component footer) {
    this.header = header;
    this.footer = footer;
    this.sendTabList();
  }

  private void sendTabList() {
    this.player.connection.send(new ClientboundTabListPacket(this.toNative(this.header), this.toNative(this.footer)));
  }

  @Override
  public void showBossBar(final BossBar bar) {
    this.audiences.showBossBar(this.player, bar);
  }

  @Override
  public void hideBossBar(final BossBar bar) {
    this.audiences.hideBossBar(this.player, bar);
  }
}
