/*
 * This file is part of TabTPS, licensed under the MIT License.
 *
 * Copyright (c) 2020-2024 Jason Penilla
 * Copyright (c) 2026 MCbabel (Folia support)
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
package xyz.jpenilla.tabtps.folia.placeholder;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.kyori.adventure.text.Component;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import xyz.jpenilla.tabtps.common.util.Serializers;
import xyz.jpenilla.tabtps.folia.TabTPSFolia;

public final class TabTPSPlaceholders extends PlaceholderExpansion {
  private final TabTPSFolia plugin;
  private final PlaceholderValues values;

  public TabTPSPlaceholders(final @NonNull TabTPSFolia plugin) {
    this.plugin = plugin;
    this.values = new PlaceholderValues(plugin);
  }

  @Override
  public @NonNull String getIdentifier() {
    return "tabtps";
  }

  @Override
  public @NonNull String getAuthor() {
    return String.join(", ", this.plugin.getDescription().getAuthors());
  }

  @Override
  public @NonNull String getVersion() {
    return this.plugin.getDescription().getVersion();
  }

  @Override
  public boolean persist() {
    return true;
  }

  @Override
  public @Nullable String onRequest(final @Nullable OfflinePlayer offlinePlayer, final @NonNull String params) {
    final Player player = offlinePlayer == null ? null : offlinePlayer.getPlayer();
    final Component value = this.values.resolve(player, params);
    return value == null ? null : Serializers.LEGACY_SECTION.serialize(value);
  }
}
