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

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.hocon.HoconConfigurationLoader;

final class ConfigEditor {
  static final List<String> MODULES = Collections.unmodifiableList(Arrays.asList(
    "tps", "mspt", "ping", "players", "cpu", "memory", "uptime",
    "lowest_region_tps", "median_region_tps", "highest_region_tps", "mob_caps"
  ));

  private final Path file;
  private final HoconConfigurationLoader loader;
  private final CommentedConfigurationNode root;

  ConfigEditor(final @NonNull Path file) throws ConfigurateException {
    this.file = file;
    this.loader = HoconConfigurationLoader.builder().path(file).build();
    this.root = this.loader.load();
  }

  @NonNull Path file() {
    return this.file;
  }

  private CommentedConfigurationNode node(final Setting setting) {
    return this.root.node(setting.path().toArray());
  }

  boolean bool(final @NonNull Setting setting, final boolean fallback) {
    return this.node(setting).getBoolean(fallback);
  }

  double number(final @NonNull Setting setting) {
    return this.node(setting).getDouble(setting.min());
  }

  @NonNull String string(final @NonNull Setting setting) {
    final String value = this.node(setting).getString();
    return value == null ? "" : value;
  }

  @NonNull List<String> modules(final @NonNull Setting setting) {
    final List<String> modules = new ArrayList<>();
    for (final String module : this.string(setting).split(",")) {
      final String trimmed = module.trim().toLowerCase(Locale.ROOT);
      if (!trimmed.isEmpty()) {
        modules.add(trimmed);
      }
    }
    return modules;
  }

  void set(final @NonNull Setting setting, final @NonNull Object value) throws ConfigurateException {
    this.node(setting).set(value);
  }

  void setNumber(final @NonNull Setting setting, final double value) throws ConfigurateException {
    final double clamped = Math.max(setting.min(), Math.min(setting.max(), value));
    if (setting.integer()) {
      this.set(setting, (int) Math.round(clamped));
    } else {
      this.set(setting, Math.round(clamped * 100.0) / 100.0);
    }
  }

  void setModules(final @NonNull Setting setting, final @NonNull List<String> modules) throws ConfigurateException {
    this.set(setting, String.join(",", modules));
  }

  void save() throws IOException {
    this.loader.save(this.root);
  }
}
