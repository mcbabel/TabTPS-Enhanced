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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import org.checkerframework.checker.nullness.qual.NonNull;
import xyz.jpenilla.tabtps.common.TabTPS;
import xyz.jpenilla.tabtps.common.util.TranslatableProvider;

public final class Setting {
  public enum Type {
    TOGGLE, NUMBER, CHOICE, TEXT, MODULES
  }

  private final String id;
  private final Type type;
  private final List<String> path;
  private final TranslatableProvider label;
  private final String icon;
  private double min;
  private double max;
  private double step;
  private boolean integer;
  private Function<TabTPS, List<String>> options = tabTPS -> Collections.emptyList();

  private Setting(final String id, final Type type, final TranslatableProvider label, final String icon, final String... path) {
    this.id = id;
    this.type = type;
    this.label = label;
    this.icon = icon;
    this.path = Arrays.asList(path);
  }

  public static @NonNull Setting toggle(final String id, final TranslatableProvider label, final String icon, final String... path) {
    return new Setting(id, Type.TOGGLE, label, icon, path);
  }

  public static @NonNull Setting number(final String id, final TranslatableProvider label, final String icon, final double min, final double max, final double step, final boolean integer, final String... path) {
    final Setting setting = new Setting(id, Type.NUMBER, label, icon, path);
    setting.min = min;
    setting.max = max;
    setting.step = step;
    setting.integer = integer;
    return setting;
  }

  public static @NonNull Setting choice(final String id, final TranslatableProvider label, final String icon, final Function<TabTPS, List<String>> options, final String... path) {
    final Setting setting = new Setting(id, Type.CHOICE, label, icon, path);
    setting.options = options;
    return setting;
  }

  public static @NonNull Setting text(final String id, final TranslatableProvider label, final String icon, final String... path) {
    return new Setting(id, Type.TEXT, label, icon, path);
  }

  public static @NonNull Setting modules(final String id, final TranslatableProvider label, final String icon, final String... path) {
    return new Setting(id, Type.MODULES, label, icon, path);
  }

  public @NonNull String id() {
    return this.id;
  }

  public @NonNull Type type() {
    return this.type;
  }

  public @NonNull List<String> path() {
    return this.path;
  }

  public @NonNull TranslatableProvider label() {
    return this.label;
  }

  public @NonNull String icon() {
    return this.icon;
  }

  public double min() {
    return this.min;
  }

  public double max() {
    return this.max;
  }

  public double step() {
    return this.step;
  }

  public boolean integer() {
    return this.integer;
  }

  public @NonNull List<String> options(final @NonNull TabTPS tabTPS) {
    return this.options.apply(tabTPS);
  }
}
