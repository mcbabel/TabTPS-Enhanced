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

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.Locale;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.translation.GlobalTranslator;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

final class ComponentJson {
  private ComponentJson() {
  }

  static @NonNull JsonObject json(final @NonNull Component component, final @NonNull Locale locale) {
    return convert(GlobalTranslator.render(component, locale), null);
  }

  private static JsonObject convert(final Component component, final @Nullable Style parent) {
    final JsonObject json = new JsonObject();
    json.addProperty("text", component instanceof TextComponent ? ((TextComponent) component).content() : "");
    final Style style = component.style();
    final TextColor color = style.color();
    if (color != null) {
      json.addProperty("color", color.asHexString());
    }
    for (final TextDecoration decoration : new TextDecoration[]{TextDecoration.BOLD, TextDecoration.ITALIC, TextDecoration.UNDERLINED, TextDecoration.STRIKETHROUGH}) {
      final TextDecoration.State state = style.decoration(decoration);
      if (state != TextDecoration.State.NOT_SET) {
        json.addProperty(decoration.name().toLowerCase(Locale.ROOT), state == TextDecoration.State.TRUE);
      }
    }
    if (!component.children().isEmpty()) {
      final JsonArray extra = new JsonArray();
      for (final Component child : component.children()) {
        extra.add(convert(child, style));
      }
      json.add("extra", extra);
    }
    return json;
  }
}
