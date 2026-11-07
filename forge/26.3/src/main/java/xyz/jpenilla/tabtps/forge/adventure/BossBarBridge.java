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

import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.world.BossEvent;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;

@DefaultQualifier(NonNull.class)
final class BossBarBridge implements BossBar.Listener {
  private final ForgeAudiences audiences;
  private final BossBar bar;
  private final ServerBossEvent event;

  BossBarBridge(final ForgeAudiences audiences, final BossBar bar) {
    this.audiences = audiences;
    this.bar = bar;
    this.event = new ServerBossEvent(UUID.randomUUID(), this.name(bar.name()), color(bar.color()), overlay(bar.overlay()));
    this.event.setProgress(bar.progress());
    this.applyFlags(bar.flags());
    bar.addListener(this);
  }

  ServerBossEvent event() {
    return this.event;
  }

  void dispose() {
    this.bar.removeListener(this);
    this.event.removeAllPlayers();
  }

  private net.minecraft.network.chat.Component name(final Component name) {
    return this.audiences.toNative(name, ForgeAudiences.DEFAULT_LOCALE);
  }

  private static BossEvent.BossBarColor color(final BossBar.Color color) {
    return BossEvent.BossBarColor.valueOf(color.name());
  }

  private static BossEvent.BossBarOverlay overlay(final BossBar.Overlay overlay) {
    return BossEvent.BossBarOverlay.valueOf(overlay.name());
  }

  private void applyFlags(final Set<BossBar.Flag> flags) {
    this.event.setDarkenScreen(flags.contains(BossBar.Flag.DARKEN_SCREEN));
    this.event.setPlayBossMusic(flags.contains(BossBar.Flag.PLAY_BOSS_MUSIC));
    this.event.setCreateWorldFog(flags.contains(BossBar.Flag.CREATE_WORLD_FOG));
  }

  @Override
  public void bossBarNameChanged(final BossBar bar, final Component oldName, final Component newName) {
    final net.minecraft.network.chat.Component name = this.name(newName);
    this.audiences.server().execute(() -> this.event.setName(name));
  }

  @Override
  public void bossBarProgressChanged(final BossBar bar, final float oldProgress, final float newProgress) {
    this.audiences.server().execute(() -> this.event.setProgress(newProgress));
  }

  @Override
  public void bossBarColorChanged(final BossBar bar, final BossBar.Color oldColor, final BossBar.Color newColor) {
    this.audiences.server().execute(() -> this.event.setColor(color(newColor)));
  }

  @Override
  public void bossBarOverlayChanged(final BossBar bar, final BossBar.Overlay oldOverlay, final BossBar.Overlay newOverlay) {
    this.audiences.server().execute(() -> this.event.setOverlay(overlay(newOverlay)));
  }

  @Override
  public void bossBarFlagsChanged(final BossBar bar, final Set<BossBar.Flag> flagsAdded, final Set<BossBar.Flag> flagsRemoved) {
    final Set<BossBar.Flag> flags = Set.copyOf(bar.flags());
    this.audiences.server().execute(() -> this.applyFlags(flags));
  }
}
