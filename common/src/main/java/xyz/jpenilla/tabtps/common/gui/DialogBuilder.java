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
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.List;
import java.util.Locale;
import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

final class DialogBuilder {
  private static final int WIDTH = 300;

  private final Locale locale;
  private Component title;
  private final JsonArray body = new JsonArray();
  private final JsonArray inputs = new JsonArray();
  private final JsonArray actions = new JsonArray();
  private @Nullable JsonObject exit;

  DialogBuilder(final @NonNull Locale locale, final @NonNull Component title) {
    this.locale = locale;
    this.title = title;
  }

  void title(final @NonNull Component title) {
    this.title = title;
  }

  void body(final @NonNull Component text) {
    final JsonObject message = new JsonObject();
    message.addProperty("type", "minecraft:plain_message");
    message.add("contents", this.json(text));
    message.addProperty("width", WIDTH);
    this.body.add(message);
  }

  void notice(final @NonNull Component text) {
    final JsonArray copy = new JsonArray();
    final JsonObject message = new JsonObject();
    message.addProperty("type", "minecraft:plain_message");
    message.add("contents", this.json(text));
    message.addProperty("width", WIDTH);
    copy.add(message);
    copy.addAll(this.body);
    while (this.body.size() > 0) {
      this.body.remove(0);
    }
    this.body.addAll(copy);
  }

  void toggle(final @NonNull String key, final @NonNull Component label, final boolean initial, final @NonNull Component on, final @NonNull Component off) {
    final JsonObject input = this.input("minecraft:single_option", key, label);
    input.addProperty("width", WIDTH);
    final JsonArray entries = new JsonArray();
    for (final boolean value : new boolean[]{true, false}) {
      final JsonObject entry = new JsonObject();
      entry.addProperty("id", String.valueOf(value));
      entry.add("display", this.json(value ? on : off));
      if (value == initial) {
        entry.addProperty("initial", true);
      }
      entries.add(entry);
    }
    input.add("options", entries);
    this.inputs.add(input);
  }

  void slider(final @NonNull String key, final @NonNull Component label, final double start, final double end, final double step, final double initial) {
    final JsonObject input = this.input("minecraft:number_range", key, label);
    input.addProperty("start", start);
    input.addProperty("end", end);
    input.addProperty("step", step);
    input.addProperty("initial", Math.max(start, Math.min(end, initial)));
    input.addProperty("width", WIDTH);
    this.inputs.add(input);
  }

  void choice(final @NonNull String key, final @NonNull Component label, final @NonNull List<String> options, final @NonNull String current) {
    final JsonObject input = this.input("minecraft:single_option", key, label);
    input.addProperty("width", WIDTH);
    final JsonArray entries = new JsonArray();
    boolean selected = false;
    for (final String option : options) {
      final JsonObject entry = new JsonObject();
      entry.addProperty("id", option);
      entry.addProperty("display", pretty(option));
      if (!selected && option.equals(current)) {
        entry.addProperty("initial", true);
        selected = true;
      }
      entries.add(entry);
    }
    input.add("options", entries);
    this.inputs.add(input);
  }

  void text(final @NonNull String key, final @NonNull Component label, final @NonNull String initial) {
    final JsonObject input = this.input("minecraft:text", key, label);
    input.addProperty("width", WIDTH);
    input.addProperty("max_length", 512);
    input.addProperty("initial", initial.length() > 512 ? initial.substring(0, 512) : initial);
    this.inputs.add(input);
  }

  void button(final @NonNull Component label, final @Nullable Component tooltip, final @NonNull String command) {
    final JsonObject action = new JsonObject();
    action.addProperty("type", "minecraft:run_command");
    action.addProperty("command", command);
    this.actions.add(this.button(label, tooltip, action));
  }

  void dynamicButton(final @NonNull Component label, final @NonNull String template) {
    final JsonObject action = new JsonObject();
    action.addProperty("type", "minecraft:dynamic/run_command");
    action.addProperty("template", template);
    this.actions.add(this.button(label, null, action));
  }

  void exit(final @NonNull Component label) {
    this.exit = new JsonObject();
    this.exit.add("label", this.json(label));
    this.exit.addProperty("width", 200);
  }

  @NonNull JsonObject build(final boolean customClicks) {
    if (customClicks) {
      for (final JsonElement element : this.actions) {
        final JsonObject button = element.getAsJsonObject();
        button.add("action", custom(button.getAsJsonObject("action")));
      }
      if (this.exit != null) {
        final JsonObject close = new JsonObject();
        close.addProperty("type", "minecraft:run_command");
        close.addProperty("command", "tabtps gui close");
        this.exit.add("action", custom(close));
      }
    }
    final JsonObject dialog = new JsonObject();
    dialog.addProperty("type", "minecraft:multi_action");
    dialog.add("title", this.json(this.title));
    if (this.body.size() > 0) {
      dialog.add("body", this.body);
    }
    if (this.inputs.size() > 0) {
      dialog.add("inputs", this.inputs);
    }
    dialog.addProperty("can_close_with_escape", true);
    dialog.addProperty("pause", false);
    if (customClicks) {
      dialog.addProperty("after_action", "none");
    }
    dialog.add("actions", this.actions);
    dialog.addProperty("columns", 2);
    if (this.exit != null) {
      dialog.add("exit_action", this.exit);
    }
    return dialog;
  }

  static @NonNull String pretty(final @NonNull String option) {
    if (!option.equals(option.toUpperCase(Locale.ROOT))) {
      return option;
    }
    final StringBuilder builder = new StringBuilder();
    for (final String word : option.split("_")) {
      if (builder.length() > 0) {
        builder.append(' ');
      }
      builder.append(word.equals("TPS") || word.equals("MSPT") ? word : word.charAt(0) + word.substring(1).toLowerCase(Locale.ROOT));
    }
    return builder.toString();
  }

  private static JsonObject custom(final JsonObject action) {
    final JsonObject custom = new JsonObject();
    final JsonObject payload = new JsonObject();
    custom.addProperty("id", GuiService.CLICK_ID);
    if (action.has("template")) {
      final String template = action.get("template").getAsString();
      final int separator = template.indexOf(GuiService.SEPARATOR);
      payload.addProperty("command", (separator < 0 ? template : template.substring(0, separator)).trim());
      custom.addProperty("type", "minecraft:dynamic/custom");
      custom.add("additions", payload);
    } else {
      payload.addProperty("command", action.get("command").getAsString());
      custom.addProperty("type", "minecraft:custom");
      custom.add("payload", payload);
    }
    return custom;
  }

  private JsonObject button(final Component label, final @Nullable Component tooltip, final JsonObject action) {
    final JsonObject button = new JsonObject();
    button.add("label", this.json(label));
    if (tooltip != null) {
      button.add("tooltip", this.json(tooltip));
    }
    button.addProperty("width", 148);
    button.add("action", action);
    return button;
  }

  private JsonObject input(final String type, final String key, final Component label) {
    final JsonObject input = new JsonObject();
    input.addProperty("type", type);
    input.addProperty("key", key);
    input.add("label", this.json(label));
    return input;
  }

  private JsonObject json(final Component component) {
    return ComponentJson.json(component, this.locale);
  }
}
