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
package xyz.jpenilla.tabtps.common.gui;

import java.util.Collections;
import java.util.List;
import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import xyz.jpenilla.tabtps.common.User;

public final class MenuItem {
  public enum Click {
    LEFT, RIGHT, SHIFT_LEFT, SHIFT_RIGHT, OTHER;

    public boolean right() {
      return this == RIGHT || this == SHIFT_RIGHT;
    }

    public boolean shift() {
      return this == SHIFT_LEFT || this == SHIFT_RIGHT;
    }
  }

  public interface Action {
    void click(@NonNull User<?> user, @NonNull Click click);
  }

  private final String material;
  private final Component name;
  private final List<Component> lore;
  private final boolean glow;
  private final @Nullable Action action;

  public MenuItem(final @NonNull String material, final @NonNull Component name, final @NonNull List<Component> lore, final boolean glow, final @Nullable Action action) {
    this.material = material;
    this.name = name;
    this.lore = lore;
    this.glow = glow;
    this.action = action;
  }

  public static @NonNull MenuItem filler(final @NonNull String material) {
    return new MenuItem(material, Component.text(" "), Collections.emptyList(), false, null);
  }

  public @NonNull String material() {
    return this.material;
  }

  public @NonNull Component name() {
    return this.name;
  }

  public @NonNull List<Component> lore() {
    return this.lore;
  }

  public boolean glow() {
    return this.glow;
  }

  public @Nullable Action action() {
    return this.action;
  }
}
