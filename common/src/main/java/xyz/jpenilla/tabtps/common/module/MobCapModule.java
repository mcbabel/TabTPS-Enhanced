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
package xyz.jpenilla.tabtps.common.module;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import xyz.jpenilla.tabtps.common.Messages;
import xyz.jpenilla.tabtps.common.TabTPS;
import xyz.jpenilla.tabtps.common.User;
import xyz.jpenilla.tabtps.common.config.Theme;
import xyz.jpenilla.tabtps.common.service.MobCapService;
import xyz.jpenilla.tabtps.common.util.Components;

public final class MobCapModule extends AbstractModule {
  public static final List<String> CATEGORIES = Collections.unmodifiableList(Arrays.asList(
    "monster", "creature", "ambient", "axolotls", "underground_water_creature", "water_creature", "water_ambient"
  ));

  private final @Nullable User<?> user;
  private final @Nullable String category;

  public MobCapModule(final @NonNull TabTPS tabTPS, final @NonNull Theme theme, final @Nullable User<?> user, final @Nullable String category) {
    super(tabTPS, theme);
    this.user = user;
    this.category = category;
  }

  @Override
  public @NonNull Component label() {
    final Component mobs = Messages.LABEL_MOB_CAPS.styled(this.theme.colorScheme().text());
    if (this.category == null) {
      return mobs;
    }
    return Component.text()
      .append(mobs)
      .append(Component.text(" (" + this.category + ")", this.theme.colorScheme().text()))
      .build();
  }

  @Override
  public boolean available() {
    return this.tabTPS.platform().mobCapService() != null;
  }

  @Override
  public @NonNull Component display() {
    final MobCapService service = this.tabTPS.platform().mobCapService();
    final MobCapService.WorldMobCaps caps = service == null || this.user == null ? null : service.world(this.user);
    if (caps == null) {
      return Component.text("N/A", this.theme.colorScheme().textSecondary());
    }
    if (this.category != null) {
      final MobCapService.Category category = caps.category(this.category);
      return category == null ? Component.text("N/A", this.theme.colorScheme().textSecondary()) : this.render(category);
    }
    final TextComponent.Builder builder = Component.text();
    boolean first = true;
    for (final MobCapService.Category category : caps.categories()) {
      if (!first) {
        builder.append(Component.text(", ", this.theme.colorScheme().textSecondary()));
      }
      first = false;
      builder.append(Component.text(category.name() + " ", this.theme.colorScheme().text())).append(this.render(category));
    }
    return builder.build();
  }

  private @NonNull Component render(final MobCapService.@NonNull Category category) {
    return Component.text()
      .append(Components.gradient(String.valueOf(category.current()), this.theme.colorScheme().goodPerformance(), this.theme.colorScheme().goodPerformanceSecondary()))
      .append(Component.text("/", this.theme.colorScheme().textSecondary()))
      .append(Components.gradient(String.valueOf(category.max()), this.theme.colorScheme().goodPerformance(), this.theme.colorScheme().goodPerformanceSecondary()))
      .build();
  }
}
