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

import java.util.Locale;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.flattener.ComponentFlattener;
import net.kyori.adventure.translation.GlobalTranslator;
import org.checkerframework.checker.nullness.qual.NonNull;

final class ComponentWidth {
  static final int CHEST_TITLE = 160;
  private static final String NARROW_2 = "!',.:;i|";
  private static final String NARROW_3 = "`l";
  private static final String NARROW_4 = " ()*I[]t{}\"";
  private static final String NARROW_5 = "<>fk";
  private static final String WIDE_7 = "@~";

  private ComponentWidth() {
  }

  static @NonNull String plain(final @NonNull Component component, final @NonNull Locale locale) {
    final StringBuilder builder = new StringBuilder();
    ComponentFlattener.basic().flatten(GlobalTranslator.render(component, locale), builder::append);
    return builder.toString();
  }

  static int width(final @NonNull String text) {
    int width = 0;
    for (int i = 0; i < text.length(); i++) {
      width += width(text.charAt(i));
    }
    return width;
  }

  static @NonNull String fit(final @NonNull String text, final int max) {
    if (width(text) <= max) {
      return text;
    }
    final int ellipsis = width("...");
    final StringBuilder builder = new StringBuilder();
    int width = 0;
    for (int i = 0; i < text.length(); i++) {
      final int next = width(text.charAt(i));
      if (width + next + ellipsis > max) {
        break;
      }
      builder.append(text.charAt(i));
      width += next;
    }
    return builder.toString().trim() + "...";
  }

  private static int width(final char c) {
    if (NARROW_2.indexOf(c) >= 0) {
      return 2;
    }
    if (NARROW_3.indexOf(c) >= 0) {
      return 3;
    }
    if (NARROW_4.indexOf(c) >= 0) {
      return 4;
    }
    if (NARROW_5.indexOf(c) >= 0) {
      return 5;
    }
    if (WIDE_7.indexOf(c) >= 0) {
      return 7;
    }
    return 6;
  }
}
