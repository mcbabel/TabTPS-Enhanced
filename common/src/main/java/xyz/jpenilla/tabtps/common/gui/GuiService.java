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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import xyz.jpenilla.tabtps.common.Messages;
import xyz.jpenilla.tabtps.common.TabTPS;
import xyz.jpenilla.tabtps.common.TabTPSPlatform;
import xyz.jpenilla.tabtps.common.User;
import xyz.jpenilla.tabtps.common.config.DisplayConfig;
import xyz.jpenilla.tabtps.common.display.DisplayHandler;
import xyz.jpenilla.tabtps.common.util.Components;
import xyz.jpenilla.tabtps.common.util.Constants;
import xyz.jpenilla.tabtps.common.util.DiscordWebhooks;
import xyz.jpenilla.tabtps.common.config.Theme;
import xyz.jpenilla.tabtps.common.util.TPSUtil;
import xyz.jpenilla.tabtps.common.util.TranslatableProvider;

import static net.kyori.adventure.text.Component.space;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.DARK_GRAY;
import static net.kyori.adventure.text.format.NamedTextColor.GRAY;
import static net.kyori.adventure.text.format.NamedTextColor.GREEN;
import static net.kyori.adventure.text.format.NamedTextColor.RED;
import static net.kyori.adventure.text.format.NamedTextColor.WHITE;

public final class GuiService {
  static final String SEPARATOR = "\u00a6";
  private static final String INVALID_WEBHOOK = "invalid webhook";
  private static final String HIDDEN = "(hidden)";
  private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();
  private static final TextColor ACCENT = TextColor.color(0x4ADE80);
  private static final TextColor ACCENT_DARK = TextColor.color(0x14B8A6);
  private static final String BORDER = "black_stained_glass_pane";
  private static final String FILL = "gray_stained_glass_pane";

  private final TabTPS tabTPS;

  public GuiService(final @NonNull TabTPS tabTPS) {
    this.tabTPS = tabTPS;
  }

  public void open(final @NonNull User<?> user, final @NonNull String page) {
    this.open(user, page, null);
  }

  public void open(final @NonNull User<?> user, final @NonNull String page, final @Nullable Component notice) {
    final TabTPSPlatform<?, ?> platform = this.tabTPS.platform();
    final String style = this.tabTPS.configManager().pluginSettings().guiStyle();
    final boolean dialogs = platform.dialogsSupported() && !style.equals("chest");
    final boolean menus = platform.menusSupported() && (!style.equals("dialog") || !platform.dialogsSupported());
    try {
      if (dialogs) {
        platform.showDialog(user, GSON.toJson(this.dialog(user, page, notice)));
      } else if (menus) {
        if (notice != null) {
          user.sendMessage(Components.ofChildren(Constants.PREFIX, space(), notice));
        }
        platform.openMenu(user, this.menu(user, page));
      } else {
        user.sendMessage(Components.ofChildren(Constants.PREFIX, space(), Messages.GUI_TEXT_UNSUPPORTED.styled(RED)));
      }
    } catch (final IOException | RuntimeException e) {
      this.tabTPS.platform().logger().warn("Failed to open the settings menu", e);
      user.sendMessage(Components.ofChildren(Constants.PREFIX, space(), Messages.GUI_TEXT_FAILED.styled(RED)));
    }
  }

  public void reload(final @NonNull User<?> user) {
    if (!user.hasPermission(Constants.PERMISSION_COMMAND_GUI) || !user.hasPermission(Constants.PERMISSION_COMMAND_RELOAD)) {
      return;
    }
    this.tabTPS.reload();
    this.open(user, Pages.MAIN, Messages.GUI_TEXT_RELOADED.styled(GREEN));
  }

  public void toggleOwn(final @NonNull User<?> user, final @NonNull String display) {
    final DisplayHandler<?> handler = handler(user, display);
    if (handler != null && this.allowed(user, display)) {
      if (handler.enabled()) {
        handler.enabled(false);
        handler.stopDisplay();
      } else {
        handler.enabled(true);
        handler.startDisplay();
      }
      user.markDirty();
    }
    this.open(user, Pages.SELF);
  }

  public void apply(final @NonNull User<?> user, final @NonNull String page, final @NonNull String data) {
    if (!user.hasPermission(Constants.PERMISSION_COMMAND_GUI)) {
      return;
    }
    final Map<String, String> values = new HashMap<>();
    for (final String part : data.split(SEPARATOR)) {
      final int split = part.indexOf('=');
      if (split > 0) {
        values.put(part.substring(0, split).trim(), part.substring(split + 1));
      }
    }
    final String[] parts = page.split("\\.");
    try {
      final ConfigEditor editor = this.editor(parts);
      if (editor == null) {
        this.open(user, Pages.MAIN);
        return;
      }
      if (parts[0].equals(Pages.MODULES) && parts.length == 4) {
        final Setting setting = Pages.find(Pages.displaySettings(parts[2]), parts[3]);
        if (setting != null) {
          final List<String> selected = new ArrayList<>();
          for (final String module : editor.modules(setting)) {
            if (!"false".equals(values.get(module)) && (values.containsKey(module) || !ConfigEditor.MODULES.contains(module))) {
              selected.add(module);
            }
          }
          for (final String module : ConfigEditor.MODULES) {
            if ("true".equals(values.get(module)) && !selected.contains(module)) {
              selected.add(module);
            }
          }
          editor.setModules(setting, selected);
        }
      } else {
        for (final Setting setting : this.settings(parts)) {
          final String value = values.get(setting.id());
          if (value != null) {
            this.write(editor, setting, value);
          }
        }
      }
      editor.save();
      this.tabTPS.reload();
      this.open(user, page, Messages.GUI_TEXT_SAVED.styled(GREEN));
    } catch (final IllegalArgumentException e) {
      if (!INVALID_WEBHOOK.equals(e.getMessage())) {
        throw e;
      }
      user.sendMessage(Components.ofChildren(Constants.PREFIX, space(), Messages.GUI_TEXT_INVALID_WEBHOOK.styled(RED)));
      this.open(user, page);
    } catch (final IOException | RuntimeException e) {
      this.tabTPS.platform().logger().warn("Failed to save settings from the menu", e);
      user.sendMessage(Components.ofChildren(Constants.PREFIX, space(), Messages.GUI_TEXT_FAILED.styled(RED)));
    }
  }

  private void write(final ConfigEditor editor, final Setting setting, final String value) throws IOException {
    switch (setting.type()) {
      case TOGGLE:
        editor.set(setting, Boolean.parseBoolean(value.trim()));
        break;
      case NUMBER:
        try {
          editor.setNumber(setting, Double.parseDouble(value.trim()));
        } catch (final NumberFormatException ignored) {
        }
        break;
      case CHOICE:
        if (setting.options(this.tabTPS).contains(value.trim())) {
          editor.set(setting, value.trim());
        }
        break;
      case TEXT:
        if (secret(setting) && value.trim().equals(HIDDEN)) {
          break;
        }
        if (setting.id().equals("webhook") && !value.trim().isEmpty() && !DiscordWebhooks.valid(value)) {
          throw new IllegalArgumentException(INVALID_WEBHOOK);
        }
        editor.set(setting, setting.id().equals("webhook") ? value.trim() : value);
        break;
      default:
        break;
    }
  }

  private @Nullable ConfigEditor editor(final String[] parts) throws IOException {
    switch (parts[0]) {
      case Pages.GENERAL:
      case Pages.LAG:
        return new ConfigEditor(this.tabTPS.configManager().mainConfigPath());
      case Pages.DISPLAY:
      case Pages.MODULES:
        if (parts.length < 3) {
          return null;
        }
        final Path path = this.displayFiles().get(parts[1]);
        return path == null || !Pages.DISPLAY_NAMES.contains(parts[2]) ? null : new ConfigEditor(path);
      default:
        return null;
    }
  }

  private List<Setting> settings(final String[] parts) {
    switch (parts[0]) {
      case Pages.GENERAL:
        return Pages.GENERAL_SETTINGS;
      case Pages.LAG:
        return Pages.LAG_SETTINGS;
      case Pages.DISPLAY:
        return parts.length >= 3 ? Pages.displaySettings(parts[2]) : Collections.emptyList();
      default:
        return Collections.emptyList();
    }
  }

  private Map<String, Path> displayFiles() {
    final Map<String, Path> files = new LinkedHashMap<>();
    final List<Path> paths = new ArrayList<>(this.tabTPS.configManager().displayConfigPaths().values());
    paths.sort(null);
    for (final Path path : paths) {
      files.put(path.getFileName().toString().split("\\.")[0], path);
    }
    return files;
  }

  private static @Nullable DisplayHandler<?> handler(final User<?> user, final String display) {
    switch (display) {
      case "tab":
        return user.tab();
      case "actionbar":
        return user.actionBar();
      case "bossbar":
        return user.bossBar();
      default:
        return null;
    }
  }

  private boolean allowed(final User<?> user, final String display) {
    return this.tabTPS.findDisplayConfig(user).map(config -> {
      final DisplayConfig.DisplaySettings settings = display.equals("tab") ? config.tabSettings()
        : display.equals("actionbar") ? config.actionBarSettings() : config.bossBarSettings();
      return settings.allow();
    }).orElse(false);
  }

  private Component title(final TranslatableProvider page) {
    return Components.ofChildren(
      Components.gradient("TabTPS", ACCENT, ACCENT_DARK),
      text(" • ", DARK_GRAY),
      page.styled(WHITE)
    );
  }

  private Component status() {
    final double tps = this.tabTPS.platform().tickTimeService().displayTps();
    final double mspt = this.tabTPS.platform().tickTimeService().averageMspt();
    return Components.ofChildren(
      text("TabTPS Folia " + Constants.TABTPS_VERSION, GRAY),
      text("  •  ", DARK_GRAY),
      text("TPS ", GRAY), TPSUtil.coloredTps(tps, Theme.DEFAULT.colorScheme()),
      text("  MSPT ", GRAY), TPSUtil.coloredMspt(mspt, Theme.DEFAULT.colorScheme()),
      text("  •  ", DARK_GRAY),
      Messages.GUI_TEXT_PLAYERS.styled(GRAY, text(this.tabTPS.platform().userService().onlinePlayers(), WHITE))
    );
  }

  private JsonObject dialog(final User<?> user, final String page, final @Nullable Component notice) throws IOException {
    final Locale locale = this.tabTPS.platform().locale(user);
    final String[] parts = page.split("\\.");
    final DialogBuilder builder;
    switch (parts[0]) {
      case Pages.GENERAL:
        builder = this.settingsDialog(locale, page, Messages.GUI_PAGE_GENERAL, Pages.GENERAL_SETTINGS, this.editor(parts), Pages.MAIN, extra -> { });
        break;
      case Pages.LAG:
        builder = this.settingsDialog(locale, page, Messages.GUI_PAGE_LAG, Pages.LAG_SETTINGS, this.editor(parts), Pages.MAIN, extra -> { });
        break;
      case Pages.DISPLAYS:
        builder = new DialogBuilder(locale, this.title(Messages.GUI_PAGE_DISPLAYS));
        builder.body(Messages.GUI_PAGE_DISPLAYS_BODY.styled(GRAY));
        for (final String config : this.displayFiles().keySet()) {
          for (final String display : Pages.DISPLAY_NAMES) {
            final Component label = this.displayFiles().size() > 1
              ? Components.ofChildren(text(config + ": ", GRAY), Pages.displayName(display).plain())
              : Pages.displayName(display).plain();
            builder.button(label, null, "tabtps gui open " + Pages.DISPLAY + "." + config + "." + display);
          }
        }
        builder.button(Messages.GUI_ACTION_BACK.plain(), null, "tabtps gui");
        break;
      case Pages.DISPLAY: {
        final ConfigEditor editor = this.editor(parts);
        if (editor == null) {
          return this.dialog(user, Pages.MAIN, null);
        }
        final List<Setting> settings = Pages.displaySettings(parts[2]);
        final List<Setting> inputs = new ArrayList<>();
        for (final Setting setting : settings) {
          if (setting.type() != Setting.Type.MODULES) {
            inputs.add(setting);
          }
        }
        builder = this.settingsDialog(locale, page, null, inputs, editor, Pages.DISPLAYS, extra -> {
          for (final Setting setting : settings) {
            if (setting.type() == Setting.Type.MODULES) {
              extra.button(setting.label().plain(), this.moduleSummary(editor.modules(setting)),
                "tabtps gui open " + Pages.MODULES + "." + parts[1] + "." + parts[2] + "." + setting.id());
            }
          }
        });
        builder.title(Components.ofChildren(this.title(Messages.GUI_PAGE_DISPLAYS), text(" › ", DARK_GRAY), Pages.displayName(parts[2]).styled(WHITE)));
        break;
      }
      case Pages.MODULES: {
        final ConfigEditor editor = this.editor(parts);
        final Setting setting = parts.length == 4 ? Pages.find(Pages.displaySettings(parts[2]), parts[3]) : null;
        if (editor == null || setting == null) {
          return this.dialog(user, Pages.MAIN, null);
        }
        builder = new DialogBuilder(locale, Components.ofChildren(this.title(Pages.displayName(parts[2])), text(" › ", DARK_GRAY), setting.label().styled(WHITE)));
        builder.body(Messages.GUI_PAGE_MODULES_BODY.styled(GRAY));
        final List<String> current = editor.modules(setting);
        final StringBuilder template = new StringBuilder("tabtps gui apply " + page + " ");
        for (final String module : ConfigEditor.MODULES) {
          builder.toggle(module, Pages.moduleName(module).plain(), current.contains(module), Messages.GUI_TEXT_SHOWN.styled(GREEN), Messages.GUI_TEXT_HIDDEN.styled(RED));
          template.append(SEPARATOR).append(module).append("=$(").append(module).append(')');
        }
        builder.dynamicButton(Messages.GUI_ACTION_SAVE.styled(GREEN), template.toString());
        builder.button(Messages.GUI_ACTION_BACK.plain(), null, "tabtps gui open " + Pages.DISPLAY + "." + parts[1] + "." + parts[2]);
        break;
      }
      case Pages.SELF:
        builder = new DialogBuilder(locale, this.title(Messages.GUI_PAGE_SELF));
        builder.body(Messages.GUI_PAGE_SELF_BODY.styled(GRAY));
        for (final String display : Pages.DISPLAY_NAMES) {
          final DisplayHandler<?> handler = handler(user, display);
          final boolean on = handler != null && handler.enabled();
          builder.button(Components.ofChildren(Pages.displayName(display).plain(), text(": ", GRAY),
              on ? Messages.GUI_TEXT_ON.styled(GREEN) : Messages.GUI_TEXT_OFF.styled(RED)),
            Messages.GUI_TEXT_CLICK_TOGGLE.plain(), "tabtps gui self " + display);
        }
        builder.button(Messages.GUI_ACTION_BACK.plain(), null, "tabtps gui");
        break;
      default:
        builder = new DialogBuilder(locale, this.title(Messages.GUI_PAGE_MAIN));
        builder.body(this.status());
        builder.body(Messages.GUI_PAGE_MAIN_BODY.styled(GRAY));
        builder.button(Messages.GUI_PAGE_DISPLAYS.plain(), Messages.GUI_PAGE_DISPLAYS_DESCRIPTION.plain(), "tabtps gui open " + Pages.DISPLAYS);
        builder.button(Messages.GUI_PAGE_LAG.plain(), Messages.GUI_PAGE_LAG_DESCRIPTION.plain(), "tabtps gui open " + Pages.LAG);
        builder.button(Messages.GUI_PAGE_GENERAL.plain(), Messages.GUI_PAGE_GENERAL_DESCRIPTION.plain(), "tabtps gui open " + Pages.GENERAL);
        builder.button(Messages.GUI_PAGE_SELF.plain(), Messages.GUI_PAGE_SELF_DESCRIPTION.plain(), "tabtps gui open " + Pages.SELF);
        builder.button(Messages.GUI_ACTION_RELOAD.plain(), Messages.GUI_ACTION_RELOAD_DESCRIPTION.plain(), "tabtps gui reload");
        builder.exit(Messages.GUI_ACTION_DONE.plain());
        break;
    }
    if (notice != null) {
      builder.notice(notice);
    }
    return builder.build();
  }

  private DialogBuilder settingsDialog(
    final Locale locale,
    final String page,
    final @Nullable TranslatableProvider title,
    final List<Setting> settings,
    final @Nullable ConfigEditor editor,
    final String back,
    final Consumer<DialogBuilder> extra
  ) {
    final DialogBuilder builder = new DialogBuilder(locale, title == null ? text("") : this.title(title));
    if (editor == null) {
      return builder;
    }
    final StringBuilder template = new StringBuilder("tabtps gui apply " + page + " ");
    for (final Setting setting : settings) {
      switch (setting.type()) {
        case TOGGLE:
          builder.toggle(setting.id(), setting.label().plain(), editor.bool(setting, false), Messages.GUI_TEXT_ON.styled(GREEN), Messages.GUI_TEXT_OFF.styled(RED));
          break;
        case NUMBER:
          builder.slider(setting.id(), setting.label().plain(), setting.min(), setting.max(), setting.step(), editor.number(setting));
          break;
        case CHOICE: {
          final List<String> options = new ArrayList<>(setting.options(this.tabTPS));
          final String current = editor.string(setting);
          if (!current.isEmpty() && !options.contains(current)) {
            options.add(0, current);
          }
          builder.choice(setting.id(), setting.label().plain(), options, current);
          break;
        }
        case TEXT:
          builder.text(setting.id(), setting.label().plain(), shown(setting, editor.string(setting)));
          break;
        default:
          continue;
      }
      template.append(SEPARATOR).append(setting.id()).append("=$(").append(setting.id()).append(')');
    }
    extra.accept(builder);
    builder.dynamicButton(Messages.GUI_ACTION_SAVE.styled(GREEN), template.toString());
    builder.button(Messages.GUI_ACTION_BACK.plain(), null, back.equals(Pages.MAIN) ? "tabtps gui" : "tabtps gui open " + back);
    return builder;
  }

  private Component moduleSummary(final List<String> modules) {
    return modules.isEmpty() ? Messages.GUI_TEXT_EMPTY.plain() : text(String.join(", ", modules));
  }

  private Menu menu(final User<?> user, final String page) throws IOException {
    final Locale locale = this.tabTPS.platform().locale(user);
    final String[] parts = page.split("\\.");
    switch (parts[0]) {
      case Pages.GENERAL:
        return this.settingsMenu(this.chestTitle(Messages.GUI_PAGE_GENERAL, locale), page, Pages.GENERAL_SETTINGS, this.editor(parts), Pages.MAIN);
      case Pages.LAG:
        return this.settingsMenu(this.chestTitle(Messages.GUI_PAGE_LAG, locale), page, Pages.LAG_SETTINGS, this.editor(parts), Pages.MAIN);
      case Pages.DISPLAYS: {
        final List<MenuItem> items = new ArrayList<>();
        for (final String config : this.displayFiles().keySet()) {
          for (final String display : Pages.DISPLAY_NAMES) {
            final Component name = this.displayFiles().size() > 1
              ? Components.ofChildren(text(config + ": ", GRAY), Pages.displayName(display).styled(WHITE))
              : Pages.displayName(display).styled(WHITE);
            items.add(new MenuItem(Pages.displayIcon(display), bold(name), Collections.singletonList(hint(Messages.GUI_TEXT_CLICK_OPEN)), false,
              (u, click) -> this.open(u, Pages.DISPLAY + "." + config + "." + display)));
          }
        }
        return this.layout(this.chestTitle(Messages.GUI_PAGE_DISPLAYS, locale), items, Pages.MAIN);
      }
      case Pages.DISPLAY: {
        final ConfigEditor editor = this.editor(parts);
        if (editor == null) {
          return this.menu(user, Pages.MAIN);
        }
        return this.settingsMenu(this.chestTitle(Pages.displayName(parts[2]), locale), page, Pages.displaySettings(parts[2]), editor, Pages.DISPLAYS);
      }
      case Pages.MODULES: {
        final ConfigEditor editor = this.editor(parts);
        final Setting setting = parts.length == 4 ? Pages.find(Pages.displaySettings(parts[2]), parts[3]) : null;
        if (editor == null || setting == null) {
          return this.menu(user, Pages.MAIN);
        }
        final List<String> current = editor.modules(setting);
        final List<MenuItem> items = new ArrayList<>();
        for (final String module : ConfigEditor.MODULES) {
          final boolean on = current.contains(module);
          items.add(new MenuItem(Pages.moduleIcon(module), bold(Pages.moduleName(module).styled(on ? GREEN : GRAY)),
            Arrays.asList(state(on), Component.empty(), hint(Messages.GUI_TEXT_CLICK_TOGGLE)), on,
            (u, click) -> this.apply(u, page, SEPARATOR + module + "=" + !on)));
        }
        return this.layout(this.chestTitle(setting.label(), locale), items, Pages.DISPLAY + "." + parts[1] + "." + parts[2]);
      }
      case Pages.SELF: {
        final List<MenuItem> items = new ArrayList<>();
        for (final String display : Pages.DISPLAY_NAMES) {
          final DisplayHandler<?> handler = handler(user, display);
          final boolean on = handler != null && handler.enabled();
          items.add(new MenuItem(Pages.displayIcon(display), bold(Pages.displayName(display).styled(WHITE)),
            Arrays.asList(state(on), Component.empty(), hint(Messages.GUI_TEXT_CLICK_TOGGLE)), on,
            (u, click) -> this.toggleOwn(u, display)));
        }
        return this.layout(this.chestTitle(Messages.GUI_PAGE_SELF, locale), items, Pages.MAIN);
      }
      default: {
        final Menu menu = new Menu(this.chestTitle(Messages.GUI_PAGE_MAIN, locale), 5);
        this.border(menu);
        menu.set(4, new MenuItem("nether_star", bold(Components.gradient("TabTPS Folia", ACCENT, ACCENT_DARK)),
          Arrays.asList(this.status(), Component.empty(), Messages.GUI_PAGE_MAIN_BODY.styled(GRAY)), true, null));
        menu.set(20, this.pageItem("painting", Messages.GUI_PAGE_DISPLAYS, Messages.GUI_PAGE_DISPLAYS_DESCRIPTION, Pages.DISPLAYS));
        menu.set(21, this.pageItem("bell", Messages.GUI_PAGE_LAG, Messages.GUI_PAGE_LAG_DESCRIPTION, Pages.LAG));
        menu.set(23, this.pageItem("comparator", Messages.GUI_PAGE_GENERAL, Messages.GUI_PAGE_GENERAL_DESCRIPTION, Pages.GENERAL));
        menu.set(24, this.pageItem("spyglass", Messages.GUI_PAGE_SELF, Messages.GUI_PAGE_SELF_DESCRIPTION, Pages.SELF));
        menu.set(40, new MenuItem("book", bold(Messages.GUI_ACTION_RELOAD.styled(WHITE)),
          Arrays.asList(Messages.GUI_ACTION_RELOAD_DESCRIPTION.styled(GRAY), Component.empty(), hint(Messages.GUI_TEXT_CLICK_RELOAD)), false,
          (u, click) -> this.reload(u)));
        menu.set(44, this.closeItem());
        return menu;
      }
    }
  }

  private MenuItem pageItem(final String icon, final TranslatableProvider name, final TranslatableProvider description, final String page) {
    return new MenuItem(icon, bold(name.styled(WHITE)), Arrays.asList(description.styled(GRAY), Component.empty(), hint(Messages.GUI_TEXT_CLICK_OPEN)), false,
      (user, click) -> this.open(user, page));
  }

  private Menu settingsMenu(
    final Component title,
    final String page,
    final List<Setting> settings,
    final @Nullable ConfigEditor editor,
    final String back
  ) {
    final List<MenuItem> items = new ArrayList<>();
    if (editor != null) {
      for (final Setting setting : settings) {
        items.add(this.settingItem(page, setting, editor));
      }
    }
    return this.layout(title, items, back);
  }

  private MenuItem settingItem(final String page, final Setting setting, final ConfigEditor editor) {
    final List<Component> lore = new ArrayList<>();
    final Component name = bold(setting.label().styled(WHITE));
    switch (setting.type()) {
      case TOGGLE: {
        final boolean on = editor.bool(setting, false);
        lore.add(state(on));
        lore.add(Component.empty());
        lore.add(hint(Messages.GUI_TEXT_CLICK_TOGGLE));
        return new MenuItem(on ? "lime_dye" : "gray_dye", name, lore, on,
          (user, click) -> this.apply(user, page, SEPARATOR + setting.id() + "=" + !on));
      }
      case NUMBER: {
        final double value = editor.number(setting);
        lore.add(Messages.GUI_TEXT_VALUE.styled(GRAY, text(format(setting, value), ACCENT)));
        lore.add(Component.empty());
        lore.add(hint(Messages.GUI_TEXT_CLICK_NUMBER, text(format(setting, setting.step()))));
        return new MenuItem(setting.icon(), name, lore, false, (user, click) -> {
          final double delta = setting.step() * (click.shift() ? 10 : 1) * (click.right() ? -1 : 1);
          this.apply(user, page, SEPARATOR + setting.id() + "=" + (value + delta));
        });
      }
      case CHOICE: {
        final List<String> options = setting.options(this.tabTPS);
        final String current = editor.string(setting);
        for (final String option : options) {
          lore.add(option.equals(current) ? text("▸ " + DialogBuilder.pretty(option), ACCENT) : text("  " + DialogBuilder.pretty(option), DARK_GRAY));
        }
        lore.add(Component.empty());
        lore.add(hint(Messages.GUI_TEXT_CLICK_NEXT));
        return new MenuItem(setting.icon(), name, lore, false, (user, click) -> {
          if (options.isEmpty()) {
            return;
          }
          final int index = options.indexOf(current);
          final int next = Math.floorMod(index + (click.right() ? -1 : 1), options.size());
          this.apply(user, page, SEPARATOR + setting.id() + "=" + options.get(next));
        });
      }
      case TEXT: {
        final String value = editor.string(setting);
        lore.add(Messages.GUI_TEXT_CURRENT.styled(GRAY, value.isEmpty() ? Messages.GUI_TEXT_EMPTY.styled(DARK_GRAY) : text(shorten(shown(setting, value)), WHITE)));
        lore.add(Component.empty());
        lore.add(hint(Messages.GUI_TEXT_CLICK_EDIT));
        return new MenuItem(setting.icon(), name, lore, false, (user, click) -> {
          if (click.right()) {
            this.apply(user, page, SEPARATOR + setting.id() + "=");
            return;
          }
          this.tabTPS.platform().closeMenu(user);
          user.sendMessage(Components.ofChildren(Constants.PREFIX, space(), Messages.GUI_TEXT_TYPE_IN_CHAT.styled(GRAY, setting.label().styled(WHITE))));
          this.tabTPS.platform().promptChat(user, input -> {
            if (input.trim().equalsIgnoreCase("cancel")) {
              user.sendMessage(Components.ofChildren(Constants.PREFIX, space(), Messages.GUI_TEXT_CANCELLED.styled(GRAY)));
              this.open(user, page);
              return;
            }
            this.apply(user, page, SEPARATOR + setting.id() + "=" + input);
          });
        });
      }
      default: {
        final List<String> modules = editor.modules(setting);
        lore.add(Messages.GUI_TEXT_CURRENT.styled(GRAY, text(modules.isEmpty() ? "-" : String.join(", ", modules), WHITE)));
        lore.add(Component.empty());
        lore.add(hint(Messages.GUI_TEXT_CLICK_OPEN));
        final String[] parts = page.split("\\.");
        return new MenuItem(setting.icon(), name, lore, false,
          (user, click) -> this.open(user, Pages.MODULES + "." + parts[1] + "." + parts[2] + "." + setting.id()));
      }
    }
  }

  private Menu layout(final Component title, final List<MenuItem> items, final String back) {
    final int rows = Math.min(6, 3 + (Math.max(0, items.size() - 1) / 7));
    final Menu menu = new Menu(title, rows);
    this.border(menu);
    int slot = 10;
    for (final MenuItem item : items) {
      if (slot % 9 == 8) {
        slot += 2;
      }
      if (slot >= (rows - 1) * 9) {
        break;
      }
      menu.set(slot, item);
      slot++;
    }
    final int bottom = (rows - 1) * 9;
    menu.set(bottom, new MenuItem("arrow", bold(Messages.GUI_ACTION_BACK.styled(WHITE)), Collections.emptyList(), false,
      (user, click) -> this.open(user, back)));
    menu.set(bottom + 8, this.closeItem());
    return menu;
  }

  private Component chestTitle(final TranslatableProvider page, final Locale locale) {
    final String name = ComponentWidth.plain(page.plain(), locale);
    if (ComponentWidth.width("TabTPS \u2022 " + name) <= ComponentWidth.CHEST_TITLE) {
      return this.title(page);
    }
    return text(ComponentWidth.fit(name, ComponentWidth.CHEST_TITLE), WHITE);
  }

  private void border(final Menu menu) {
    final int size = menu.rows() * 9;
    for (int slot = 0; slot < size; slot++) {
      final boolean edge = slot < 9 || slot >= size - 9 || slot % 9 == 0 || slot % 9 == 8;
      menu.set(slot, MenuItem.filler(edge ? BORDER : FILL));
    }
  }

  private MenuItem closeItem() {
    return new MenuItem("barrier", bold(Messages.GUI_ACTION_CLOSE.styled(RED)), Collections.emptyList(), false,
      (user, click) -> this.tabTPS.platform().closeMenu(user));
  }

  private static Component bold(final Component component) {
    return component.decoration(TextDecoration.BOLD, true).decoration(TextDecoration.ITALIC, false);
  }

  private static Component hint(final TranslatableProvider message, final Component... args) {
    return message.styled(DARK_GRAY, args);
  }

  private static Component state(final boolean on) {
    return Messages.GUI_TEXT_STATE.styled(GRAY, on ? Messages.GUI_TEXT_ON.styled(GREEN) : Messages.GUI_TEXT_OFF.styled(RED));
  }

  private static String format(final Setting setting, final double value) {
    return setting.integer() ? String.valueOf((long) Math.round(value)) : String.valueOf(Math.round(value * 100.0) / 100.0);
  }

  private static boolean secret(final Setting setting) {
    return setting.id().equals("webhook");
  }

  private static String shown(final Setting setting, final String value) {
    return secret(setting) && !value.isEmpty() ? HIDDEN : value;
  }

  private static String shorten(final String value) {
    return value.length() > 40 ? value.substring(0, 37) + "..." : value;
  }
}
