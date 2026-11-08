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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import xyz.jpenilla.tabtps.common.Messages;
import xyz.jpenilla.tabtps.common.util.TranslatableProvider;

final class Pages {
  static final String MAIN = "main";
  static final String GENERAL = "general";
  static final String LAG = "lag";
  static final String DISPLAYS = "displays";
  static final String SELF = "self";
  static final String DISPLAY = "display";
  static final String MODULES = "modules";

  static final List<Setting> GENERAL_SETTINGS = Collections.unmodifiableList(Arrays.asList(
    Setting.toggle("update_checker", Messages.GUI_SETTING_UPDATE_CHECKER, "compass", "update-checker"),
    Setting.toggle("notify_join", Messages.GUI_SETTING_NOTIFY_JOIN, "bell", "update-notify-on-join"),
    Setting.toggle("console_colors", Messages.GUI_SETTING_CONSOLE_COLORS, "glow_ink_sac", "console-colors"),
    Setting.toggle("proxy_stats", Messages.GUI_SETTING_PROXY_STATS, "ender_eye", "send-stats-to-proxy"),
    Setting.number("rate_tab", Messages.GUI_SETTING_RATE_TAB, "clock", 50, 2000, 50, true, "update-rates", "tab"),
    Setting.number("rate_actionbar", Messages.GUI_SETTING_RATE_ACTIONBAR, "clock", 50, 2000, 50, true, "update-rates", "action-bar"),
    Setting.number("rate_bossbar", Messages.GUI_SETTING_RATE_BOSSBAR, "clock", 50, 2000, 50, true, "update-rates", "boss-bar"),
    Setting.choice("gui_style", Messages.GUI_SETTING_GUI_STYLE, "chest", tabTPS -> Arrays.asList("auto", "dialog", "chest"), "gui-style"),
    Setting.toggle("menu_sounds", Messages.GUI_SETTING_MENU_SOUNDS, "note_block", "menu-sounds")
  ));

  static final List<Setting> LAG_SETTINGS = Collections.unmodifiableList(Arrays.asList(
    Setting.toggle("enabled", Messages.GUI_SETTING_LAG_ENABLED, "bell", "lag-alerts", "enabled"),
    Setting.number("tps_below", Messages.GUI_SETTING_TPS_BELOW, "redstone", 0, 20, 0.5, false, "lag-alerts", "tps-below"),
    Setting.number("mspt_above", Messages.GUI_SETTING_MSPT_ABOVE, "repeater", 0, 200, 5, false, "lag-alerts", "mspt-above"),
    Setting.number("seconds", Messages.GUI_SETTING_SECONDS, "clock", 1, 120, 1, true, "lag-alerts", "seconds"),
    Setting.number("cooldown", Messages.GUI_SETTING_COOLDOWN, "hopper", 0, 1800, 30, true, "lag-alerts", "cooldown-seconds"),
    Setting.toggle("recovery", Messages.GUI_SETTING_RECOVERY, "emerald", "lag-alerts", "notify-recovery"),
    Setting.text("webhook", Messages.GUI_SETTING_WEBHOOK, "writable_book", "lag-alerts", "discord-webhook")
  ));

  static final List<String> DISPLAY_NAMES = Collections.unmodifiableList(Arrays.asList("tab", "actionbar", "bossbar"));

  private Pages() {
  }

  static @NonNull String section(final @NonNull String display) {
    switch (display) {
      case "tab":
        return "tab-settings";
      case "actionbar":
        return "action-bar-settings";
      default:
        return "boss-bar-settings";
    }
  }

  static @NonNull TranslatableProvider displayName(final @NonNull String display) {
    switch (display) {
      case "tab":
        return Messages.GUI_DISPLAY_TAB;
      case "actionbar":
        return Messages.GUI_DISPLAY_ACTIONBAR;
      default:
        return Messages.GUI_DISPLAY_BOSSBAR;
    }
  }

  static @NonNull String displayIcon(final @NonNull String display) {
    switch (display) {
      case "tab":
        return "painting";
      case "actionbar":
        return "oak_sign";
      default:
        return "nether_star";
    }
  }

  static @NonNull List<Setting> displaySettings(final @NonNull String display) {
    final String section = section(display);
    final List<Setting> settings = new ArrayList<>();
    settings.add(Setting.toggle("allow", Messages.GUI_SETTING_ALLOW, "lime_dye", section, "allow"));
    settings.add(Setting.toggle("login", Messages.GUI_SETTING_LOGIN, "oak_door", section, "enable-on-login"));
    settings.add(Setting.choice("theme", Messages.GUI_SETTING_THEME, "painting", tabTPS -> tabTPS.configManager().themeNames(), section, "theme"));
    if (display.equals("tab")) {
      settings.add(Setting.modules("header", Messages.GUI_SETTING_HEADER, "item_frame", section, "header-modules"));
      settings.add(Setting.modules("footer", Messages.GUI_SETTING_FOOTER, "item_frame", section, "footer-modules"));
      settings.add(Setting.text("header_format", Messages.GUI_SETTING_HEADER_FORMAT, "writable_book", section, "header-format"));
      settings.add(Setting.text("footer_format", Messages.GUI_SETTING_FOOTER_FORMAT, "writable_book", section, "footer-format"));
    } else {
      settings.add(Setting.modules("modules", Messages.GUI_SETTING_MODULES, "item_frame", section, "modules"));
    }
    if (display.equals("bossbar")) {
      settings.add(Setting.choice("fill_mode", Messages.GUI_SETTING_FILL_MODE, "comparator", tabTPS -> Arrays.asList("MSPT", "TPS", "REVERSE_MSPT", "REVERSE_TPS"), section, "fill-mode"));
      settings.add(Setting.choice("overlay", Messages.GUI_SETTING_OVERLAY, "iron_chain|chain", tabTPS -> Arrays.asList("PROGRESS", "NOTCHED_6", "NOTCHED_10", "NOTCHED_12", "NOTCHED_20"), section, "overlay"));
      settings.add(Setting.number("delay", Messages.GUI_SETTING_DELAY, "clock", 0, 60, 1, true, section, "login-delay-seconds"));
    }
    if (!display.equals("tab")) {
      settings.add(Setting.text("format", Messages.GUI_SETTING_FORMAT, "writable_book", section, "format"));
    }
    return settings;
  }

  static @Nullable Setting find(final @NonNull List<Setting> settings, final @NonNull String id) {
    for (final Setting setting : settings) {
      if (setting.id().equals(id)) {
        return setting;
      }
    }
    return null;
  }

  static @NonNull TranslatableProvider moduleName(final @NonNull String module) {
    switch (module) {
      case "tps":
        return Messages.GUI_MODULE_TPS;
      case "mspt":
        return Messages.GUI_MODULE_MSPT;
      case "ping":
        return Messages.GUI_MODULE_PING;
      case "players":
        return Messages.GUI_MODULE_PLAYERS;
      case "cpu":
        return Messages.GUI_MODULE_CPU;
      case "memory":
        return Messages.GUI_MODULE_MEMORY;
      case "uptime":
        return Messages.GUI_MODULE_UPTIME;
      case "lowest_region_tps":
        return Messages.GUI_MODULE_LOWEST_REGION_TPS;
      case "median_region_tps":
        return Messages.GUI_MODULE_MEDIAN_REGION_TPS;
      case "highest_region_tps":
        return Messages.GUI_MODULE_HIGHEST_REGION_TPS;
      default:
        return Messages.GUI_MODULE_MOB_CAPS;
    }
  }

  static @NonNull String moduleIcon(final @NonNull String module) {
    switch (module) {
      case "tps":
        return "clock";
      case "mspt":
        return "repeater";
      case "ping":
        return "compass";
      case "players":
        return "totem_of_undying";
      case "cpu":
        return "redstone";
      case "memory":
        return "chest_minecart";
      case "uptime":
        return "recovery_compass";
      case "lowest_region_tps":
      case "median_region_tps":
      case "highest_region_tps":
        return "filled_map";
      default:
        return "pig_spawn_egg";
    }
  }
}
