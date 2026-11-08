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
package xyz.jpenilla.tabtps.folia.gui;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.translation.GlobalTranslator;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import xyz.jpenilla.tabtps.common.User;
import xyz.jpenilla.tabtps.common.gui.Menu;
import xyz.jpenilla.tabtps.common.gui.MenuItem;
import xyz.jpenilla.tabtps.folia.TabTPSFolia;

public final class BukkitGui implements Listener {
  private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.builder()
    .character(LegacyComponentSerializer.SECTION_CHAR)
    .hexColors()
    .useUnusualXRepeatedCharacterHexFormat()
    .build();

  private final TabTPSFolia plugin;
  private final boolean regionized;
  private final boolean dialogs;
  private final Map<UUID, Consumer<String>> prompts = new ConcurrentHashMap<>();
  private @Nullable DialogPackets packets;
  private boolean packetsResolved;

  public BukkitGui(final @NonNull TabTPSFolia plugin, final boolean regionized, final @NonNull String minecraftVersion) {
    this.plugin = plugin;
    this.regionized = regionized;
    this.dialogs = atLeast(minecraftVersion, 1, 21, 6);
  }

  public boolean dialogsSupported() {
    return this.dialogs;
  }

  public void showDialog(final @NonNull User<?> user, final @NonNull String dialog) {
    final Player player = player(user);
    if (player == null) {
      return;
    }
    final DialogPackets packets = this.packets();
    if (packets != null) {
      this.run(player, () -> {
        try {
          packets.send(player, dialog);
        } catch (final ReflectiveOperationException | RuntimeException e) {
          this.plugin.logger().warn("Failed to show the settings dialog", e);
        }
      });
      return;
    }
    final String command = "dialog show " + player.getUniqueId() + " " + dialog;
    this.global(() -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command));
  }

  private synchronized @Nullable DialogPackets packets() {
    if (!this.packetsResolved) {
      this.packetsResolved = true;
      this.packets = DialogPackets.create();
    }
    return this.packets;
  }

  public void open(final @NonNull User<?> user, final @NonNull Menu menu) {
    final Player player = player(user);
    if (player == null) {
      return;
    }
    final Locale locale = locale(player);
    this.run(player, () -> {
      final Holder holder = new Holder(menu);
      final Inventory inventory = Bukkit.createInventory(holder, menu.rows() * 9, LEGACY.serialize(GlobalTranslator.render(menu.title(), locale)));
      holder.inventory = inventory;
      for (final Map.Entry<Integer, MenuItem> entry : menu.items().entrySet()) {
        if (entry.getKey() < inventory.getSize()) {
          inventory.setItem(entry.getKey(), this.stack(entry.getValue(), locale));
        }
      }
      player.openInventory(inventory);
    });
  }

  public void close(final @NonNull User<?> user) {
    final Player player = player(user);
    if (player != null) {
      this.run(player, player::closeInventory);
    }
  }

  public void prompt(final @NonNull User<?> user, final @NonNull Consumer<String> input) {
    final Player player = player(user);
    if (player != null) {
      this.prompts.put(player.getUniqueId(), input);
    }
  }

  public static @NonNull Locale locale(final @NonNull Player player) {
    try {
      final String tag = player.getLocale();
      if (tag != null && !tag.isEmpty()) {
        return Locale.forLanguageTag(tag.replace('_', '-'));
      }
    } catch (final RuntimeException | LinkageError ignored) {
    }
    return Locale.US;
  }

  @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
  public void onChat(final AsyncPlayerChatEvent event) {
    final Consumer<String> input = this.prompts.remove(event.getPlayer().getUniqueId());
    if (input == null) {
      return;
    }
    event.setCancelled(true);
    final String message = event.getMessage();
    this.run(event.getPlayer(), () -> input.accept(message));
  }

  @EventHandler
  public void onQuit(final PlayerQuitEvent event) {
    this.prompts.remove(event.getPlayer().getUniqueId());
  }

  @EventHandler(priority = EventPriority.LOWEST)
  public void onClick(final InventoryClickEvent event) {
    if (!(event.getView().getTopInventory().getHolder() instanceof Holder)) {
      return;
    }
    event.setCancelled(true);
    if (!(event.getWhoClicked() instanceof Player) || event.getRawSlot() < 0 || event.getRawSlot() >= event.getView().getTopInventory().getSize()) {
      return;
    }
    final Holder holder = (Holder) event.getView().getTopInventory().getHolder();
    final MenuItem item = holder.menu.item(event.getRawSlot());
    if (item == null || item.action() == null) {
      return;
    }
    final User<?> user = this.plugin.userService().user((Player) event.getWhoClicked());
    item.action().click(user, click(event.getClick()));
  }

  @EventHandler(priority = EventPriority.LOWEST)
  public void onDrag(final InventoryDragEvent event) {
    if (event.getView().getTopInventory().getHolder() instanceof Holder) {
      event.setCancelled(true);
    }
  }

  private ItemStack stack(final MenuItem item, final Locale locale) {
    Material material = null;
    for (final String name : item.material().split("\\|")) {
      material = Material.matchMaterial(name);
      if (material != null) {
        break;
      }
    }
    if (material == null) {
      material = Material.PAPER;
    }
    final ItemStack stack = new ItemStack(material);
    final ItemMeta meta = stack.getItemMeta();
    if (meta == null) {
      return stack;
    }
    meta.setDisplayName(LEGACY.serialize(GlobalTranslator.render(item.name(), locale)));
    final List<String> lore = new ArrayList<>();
    for (final Component line : item.lore()) {
      lore.add(LEGACY.serialize(GlobalTranslator.render(line, locale)));
    }
    meta.setLore(lore);
    meta.addItemFlags(ItemFlag.values());
    if (item.glow()) {
      glow(meta);
    }
    stack.setItemMeta(meta);
    return stack;
  }

  private static void glow(final ItemMeta meta) {
    try {
      final Method override = ItemMeta.class.getMethod("setEnchantmentGlintOverride", Boolean.class);
      override.invoke(meta, Boolean.TRUE);
      return;
    } catch (final ReflectiveOperationException | RuntimeException ignored) {
    }
    final Enchantment enchantment = Enchantment.getByKey(NamespacedKey.minecraft("unbreaking"));
    if (enchantment != null) {
      meta.addEnchant(enchantment, 1, true);
    }
  }

  private static MenuItem.Click click(final ClickType type) {
    switch (type) {
      case LEFT:
        return MenuItem.Click.LEFT;
      case RIGHT:
        return MenuItem.Click.RIGHT;
      case SHIFT_LEFT:
        return MenuItem.Click.SHIFT_LEFT;
      case SHIFT_RIGHT:
        return MenuItem.Click.SHIFT_RIGHT;
      default:
        return MenuItem.Click.OTHER;
    }
  }

  private void run(final Player player, final Runnable task) {
    if (this.regionized) {
      player.getScheduler().run(this.plugin, scheduled -> task.run(), null);
    } else {
      Bukkit.getScheduler().runTask(this.plugin, task);
    }
  }

  private void global(final Runnable task) {
    if (this.regionized) {
      Bukkit.getGlobalRegionScheduler().execute(this.plugin, task);
    } else {
      Bukkit.getScheduler().runTask(this.plugin, task);
    }
  }

  private static @Nullable Player player(final User<?> user) {
    final Object base = user.base();
    return base instanceof Player ? (Player) base : null;
  }

  static boolean atLeast(final String version, final int... minimum) {
    final String[] parts = version.split("[^0-9]+");
    for (int i = 0; i < minimum.length; i++) {
      final int value = i < parts.length && !parts[i].isEmpty() ? Integer.parseInt(parts[i]) : 0;
      if (value != minimum[i]) {
        return value > minimum[i];
      }
    }
    return true;
  }

  private static final class Holder implements InventoryHolder {
    private final Menu menu;
    private Inventory inventory;

    Holder(final Menu menu) {
      this.menu = menu;
    }

    @Override
    public @NonNull Inventory getInventory() {
      return this.inventory;
    }
  }
}
