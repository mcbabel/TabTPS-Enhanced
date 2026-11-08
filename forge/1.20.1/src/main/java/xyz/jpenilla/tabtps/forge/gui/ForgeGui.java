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
package xyz.jpenilla.tabtps.forge.gui;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.translation.Translator;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.ServerChatEvent;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import xyz.jpenilla.tabtps.common.User;
import xyz.jpenilla.tabtps.common.gui.Menu;
import xyz.jpenilla.tabtps.common.gui.MenuItem;
import xyz.jpenilla.tabtps.forge.TabTPSForge;

public final class ForgeGui {
  private final TabTPSForge platform;
  private final Map<UUID, Consumer<String>> prompts = new ConcurrentHashMap<>();

  public ForgeGui(final @NonNull TabTPSForge platform) {
    this.platform = platform;
    MinecraftForge.EVENT_BUS.addListener(this::onChat);
  }

  private void onChat(final ServerChatEvent event) {
    final Consumer<String> input = this.prompts.remove(event.getPlayer().getUUID());
    if (input == null) {
      return;
    }
    event.setCanceled(true);
    final String text = event.getRawText();
    this.platform.server().execute(() -> input.accept(text));
  }

  public void open(final @NonNull User<?> user, final @NonNull Menu menu) {
    final ServerPlayer player = player(user);
    if (player == null) {
      return;
    }
    this.platform.server().execute(() -> {
      final Locale locale = locale(player);
      final int rows = Math.max(1, Math.min(6, menu.rows()));
      final SimpleContainer container = new SimpleContainer(rows * 9);
      for (final Map.Entry<Integer, MenuItem> entry : menu.items().entrySet()) {
        if (entry.getKey() < container.getContainerSize()) {
          container.setItem(entry.getKey(), this.stack(entry.getValue(), locale));
        }
      }
      player.openMenu(new SimpleMenuProvider(
        (id, inventory, opener) -> new GuiMenu(type(rows), id, inventory, container, rows, menu, user),
        this.platform.audiences().toNative(menu.title(), locale)
      ));
    });
  }

  public void close(final @NonNull User<?> user) {
    final ServerPlayer player = player(user);
    if (player != null) {
      this.platform.server().execute(player::closeContainer);
    }
  }

  public void prompt(final @NonNull User<?> user, final @NonNull Consumer<String> input) {
    final ServerPlayer player = player(user);
    if (player != null) {
      this.prompts.put(player.getUUID(), input);
    }
  }

  public static @NonNull Locale locale(final @NonNull ServerPlayer player) {
    final Locale locale = Translator.parseLocale(player.getLanguage());
    return locale == null ? Locale.US : locale;
  }

  private ItemStack stack(final MenuItem menuItem, final Locale locale) {
    final ItemStack stack = new ItemStack(item(menuItem.material()));
    stack.setHoverName(this.platform.audiences().toNative(plain(menuItem.name()), locale));
    final ListTag lore = new ListTag();
    for (final Component line : menuItem.lore()) {
      lore.add(StringTag.valueOf(net.minecraft.network.chat.Component.Serializer.toJson(this.platform.audiences().toNative(plain(line), locale))));
    }
    final CompoundTag display = stack.getOrCreateTagElement("display");
    display.put("Lore", lore);
    if (menuItem.glow()) {
      stack.enchant(Enchantments.UNBREAKING, 1);
    }
    for (final ItemStack.TooltipPart part : ItemStack.TooltipPart.values()) {
      stack.hideTooltipPart(part);
    }
    return stack;
  }

  private static Component plain(final Component component) {
    return Component.text().decoration(TextDecoration.ITALIC, false).append(component).build();
  }

  private static Item item(final String material) {
    for (final String name : material.split("\\|")) {
      final ResourceLocation id = ResourceLocation.tryParse(name);
      if (id != null && BuiltInRegistries.ITEM.containsKey(id)) {
        return BuiltInRegistries.ITEM.get(id);
      }
    }
    return Items.PAPER;
  }

  private static MenuType<?> type(final int rows) {
    switch (rows) {
      case 1:
        return MenuType.GENERIC_9x1;
      case 2:
        return MenuType.GENERIC_9x2;
      case 3:
        return MenuType.GENERIC_9x3;
      case 4:
        return MenuType.GENERIC_9x4;
      case 5:
        return MenuType.GENERIC_9x5;
      default:
        return MenuType.GENERIC_9x6;
    }
  }

  private static @Nullable ServerPlayer player(final User<?> user) {
    final Object base = user.base();
    return base instanceof ServerPlayer ? (ServerPlayer) base : null;
  }

  private static final class GuiMenu extends ChestMenu {
    private final int size;
    private final Menu menu;
    private final User<?> user;

    GuiMenu(final MenuType<?> type, final int id, final Inventory inventory, final SimpleContainer container, final int rows, final Menu menu, final User<?> user) {
      super(type, id, inventory, container, rows);
      this.size = rows * 9;
      this.menu = menu;
      this.user = user;
    }

    @Override
    public void clicked(final int slot, final int button, final ClickType type, final Player player) {
      if (slot >= 0 && slot < this.size) {
        final MenuItem item = this.menu.item(slot);
        if (item != null && item.action() != null) {
          item.action().click(this.user, click(type, button));
        }
      }
      this.sendAllDataToRemote();
    }

    @Override
    public ItemStack quickMoveStack(final Player player, final int slot) {
      return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(final Player player) {
      return true;
    }

    private static MenuItem.Click click(final ClickType type, final int button) {
      if (type == ClickType.QUICK_MOVE) {
        return button == 1 ? MenuItem.Click.SHIFT_RIGHT : MenuItem.Click.SHIFT_LEFT;
      }
      if (type == ClickType.PICKUP) {
        return button == 1 ? MenuItem.Click.RIGHT : MenuItem.Click.LEFT;
      }
      return MenuItem.Click.OTHER;
    }
  }
}
