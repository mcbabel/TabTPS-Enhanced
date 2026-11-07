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
package xyz.jpenilla.tabtps.common.service;

import java.util.Collections;
import java.util.List;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import xyz.jpenilla.tabtps.common.User;

public interface MobCapService {
  @NonNull List<WorldMobCaps> worlds();

  @Nullable WorldMobCaps world(@NonNull User<?> user);

  final class WorldMobCaps {
    private final String name;
    private final List<Category> categories;

    public WorldMobCaps(final @NonNull String name, final @NonNull List<Category> categories) {
      this.name = name;
      this.categories = Collections.unmodifiableList(categories);
    }

    public @NonNull String name() {
      return this.name;
    }

    public @NonNull List<Category> categories() {
      return this.categories;
    }

    public @Nullable Category category(final @NonNull String name) {
      for (final Category category : this.categories) {
        if (category.name().equals(name)) {
          return category;
        }
      }
      return null;
    }
  }

  final class Category {
    private final String name;
    private final int current;
    private final int max;

    public Category(final @NonNull String name, final int current, final int max) {
      this.name = name;
      this.current = current;
      this.max = max;
    }

    public @NonNull String name() {
      return this.name;
    }

    public int current() {
      return this.current;
    }

    public int max() {
      return this.max;
    }
  }
}
