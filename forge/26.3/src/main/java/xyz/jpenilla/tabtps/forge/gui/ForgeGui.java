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

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.translation.Translator;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.Tag;
import net.minecraft.network.Connection;
import net.minecraft.network.HandlerNames;
import net.minecraft.network.protocol.common.ClientboundClearDialogPacket;
import net.minecraft.network.protocol.common.ServerboundCustomClickActionPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.dialog.Dialog;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import net.minecraftforge.event.ServerChatEvent;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import xyz.jpenilla.tabtps.common.User;
import xyz.jpenilla.tabtps.common.gui.GuiService;
import xyz.jpenilla.tabtps.common.gui.Menu;
import xyz.jpenilla.tabtps.common.gui.MenuItem;
import xyz.jpenilla.tabtps.forge.TabTPSForge;

public final class ForgeGui {
  private static final String CLICK_HANDLER = "tabtps_click";
  private final TabTPSForge platform;
  private final Map<UUID, Consumer<String>> prompts = new ConcurrentHashMap<>();

  public ForgeGui(final @NonNull TabTPSForge platform) {
    this.platform = platform;
    ServerChatEvent.BUS.addListener(this::onChat);
  }

  private boolean onChat(final ServerChatEvent event) {
    final Consumer<String> input = this.prompts.remove(event.getPlayer().getUUID());
    if (input == null) {
      return false;
    }
    final String text = event.getRawText();
    this.platform.server().execute(() -> input.accept(text));
    return true;
  }

  public void watchClicks(final @NonNull ServerPlayer player) {
    try {
      final Field connectionField = ServerCommonPacketListenerImpl.class.getDeclaredField("connection");
      connectionField.setAccessible(true);
      final Field channelField = Connection.class.getDeclaredField("channel");
      channelField.setAccessible(true);
      final Channel channel = (Channel) channelField.get(connectionField.get(player.connection));
      if (channel == null || channel.pipeline().get(CLICK_HANDLER) != null) {
        return;
      }
      final UUID uuid = player.getUUID();
      channel.pipeline().addBefore(HandlerNames.PACKET_HANDLER, CLICK_HANDLER, new ChannelInboundHandlerAdapter() {
        @Override
        public void channelRead(final ChannelHandlerContext context, final Object message) {
          if (message instanceof ServerboundCustomClickActionPacket packet && packet.id().toString().equals(GuiService.CLICK_ID)) {
            final String payload = packet.payload().map(Tag::toString).orElse("");
            ForgeGui.this.platform.server().execute(() -> {
              final ServerPlayer target = ForgeGui.this.platform.server().getPlayerList().getPlayer(uuid);
              if (target != null) {
                ForgeGui.this.platform.tabTPS().gui().click(ForgeGui.this.platform.userService().user(target), payload);
              }
            });
            return;
          }
          context.fireChannelRead(message);
        }
      });
    } catch (final ReflectiveOperationException | RuntimeException e) {
      this.platform.logger().warn("Failed to watch settings menu clicks", e);
    }
  }

  public void closeDialog(final @NonNull User<?> user) {
    final ServerPlayer player = player(user);
    if (player != null) {
      this.platform.server().execute(() -> player.connection.send(ClientboundClearDialogPacket.INSTANCE));
    }
  }

  public void showDialog(final @NonNull User<?> user, final @NonNull String json) {
    final ServerPlayer player = player(user);
    if (player == null) {
      return;
    }
    this.platform.server().execute(() -> {
      try {
        final RegistryOps<JsonElement> ops = this.platform.server().registryAccess().createSerializationContext(JsonOps.INSTANCE);
        final Dialog dialog = Dialog.DIRECT_CODEC.parse(ops, JsonParser.parseString(json)).getOrThrow();
        if (player.containerMenu instanceof GuiMenu) {
          player.closeContainer();
        }
        player.openDialog(Holder.direct(dialog));
      } catch (final RuntimeException e) {
        this.platform.logger().warn("Failed to show the settings dialog", e);
      }
    });
  }

  public void playSound(final @NonNull User<?> user, final @NonNull String sound) {
    final ServerPlayer player = player(user);
    final Identifier id = Identifier.tryParse(sound);
    if (player == null || id == null) {
      return;
    }
    this.platform.server().execute(() -> player.connection.send(new ClientboundSoundPacket(
      Holder.direct(SoundEvent.createVariableRangeEvent(id)), SoundSource.MASTER, player.getX(), player.getY(), player.getZ(), 0.5F, 1.0F, player.getRandom().nextLong()
    )));
  }

  public void open(final @NonNull User<?> user, final @NonNull Menu menu) {
    final ServerPlayer player = player(user);
    if (player == null) {
      return;
    }
    this.platform.server().execute(() -> {
      final Locale locale = locale(player);
      if (player.containerMenu instanceof GuiMenu current && current.show(menu)) {
        return;
      }
      final int rows = Math.max(1, Math.min(6, menu.rows()));
      final SimpleContainer container = new SimpleContainer(rows * 9);
      final Function<MenuItem, ItemStack> stacks = item -> this.stack(item, locale);
      for (final Map.Entry<Integer, MenuItem> entry : menu.items().entrySet()) {
        if (entry.getKey() < container.getContainerSize()) {
          container.setItem(entry.getKey(), stacks.apply(entry.getValue()));
        }
      }
      if (player.containerMenu instanceof GuiMenu) {
        player.doCloseContainer();
      }
      player.openMenu(new SimpleMenuProvider(
        (id, inventory, opener) -> new GuiMenu(type(rows), id, inventory, container, rows, menu, user, stacks),
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
    final Locale locale = Translator.parseLocale(player.clientInformation().language());
    return locale == null ? Locale.US : locale;
  }

  private ItemStack stack(final MenuItem menuItem, final Locale locale) {
    final ItemStack stack = new ItemStack(item(menuItem.material()));
    stack.set(DataComponents.CUSTOM_NAME, this.platform.audiences().toNative(plain(menuItem.name()), locale));
    final List<net.minecraft.network.chat.Component> lore = new ArrayList<>();
    for (final Component line : menuItem.lore()) {
      lore.add(this.platform.audiences().toNative(plain(line), locale));
    }
    stack.set(DataComponents.LORE, new ItemLore(lore));
    if (menuItem.glow()) {
      stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
    }
    return stack;
  }

  private static Component plain(final Component component) {
    return Component.text().decoration(TextDecoration.ITALIC, false).append(component).build();
  }

  private static Item item(final String material) {
    for (final String name : material.split("\\|")) {
      final Identifier id = Identifier.tryParse(name);
      if (id != null && BuiltInRegistries.ITEM.containsKey(id)) {
        return BuiltInRegistries.ITEM.getValue(id);
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
    private final SimpleContainer container;
    private final User<?> user;
    private final Function<MenuItem, ItemStack> stacks;
    private Menu menu;
    private int ticks;

    GuiMenu(final MenuType<?> type, final int id, final Inventory inventory, final SimpleContainer container, final int rows, final Menu menu, final User<?> user, final Function<MenuItem, ItemStack> stacks) {
      super(type, id, inventory, container, rows);
      this.size = rows * 9;
      this.container = container;
      this.menu = menu;
      this.user = user;
      this.stacks = stacks;
    }

    boolean show(final Menu next) {
      if (Math.max(1, Math.min(6, next.rows())) * 9 != this.size || !next.title().equals(this.menu.title())) {
        return false;
      }
      this.menu = next;
      for (int slot = 0; slot < this.size; slot++) {
        final MenuItem item = next.item(slot);
        this.container.setItem(slot, item == null ? ItemStack.EMPTY : this.stacks.apply(item));
      }
      this.broadcastChanges();
      return true;
    }

    @Override
    public void broadcastChanges() {
      if (this.menu.live() && ++this.ticks % 20 == 0) {
        for (final Map.Entry<Integer, MenuItem> entry : this.menu.refresh().entrySet()) {
          if (entry.getKey() < this.size) {
            this.container.setItem(entry.getKey(), this.stacks.apply(entry.getValue()));
          }
        }
      }
      super.broadcastChanges();
    }

    @Override
    public void clicked(final int slot, final int button, final ContainerInput input, final Player player) {
      if (slot >= 0 && slot < this.size) {
        final MenuItem item = this.menu.item(slot);
        if (item != null && item.action() != null) {
          item.action().click(this.user, click(input, button));
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

    private static MenuItem.Click click(final ContainerInput input, final int button) {
      if (input == ContainerInput.QUICK_MOVE) {
        return button == 1 ? MenuItem.Click.SHIFT_RIGHT : MenuItem.Click.SHIFT_LEFT;
      }
      if (input == ContainerInput.PICKUP) {
        return button == 1 ? MenuItem.Click.RIGHT : MenuItem.Click.LEFT;
      }
      return MenuItem.Click.OTHER;
    }
  }
}
