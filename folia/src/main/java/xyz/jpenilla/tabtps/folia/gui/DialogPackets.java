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

import com.google.gson.JsonParser;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

final class DialogPackets {
  private final Object codec;
  private final Method parse;
  private final Method getOrThrow;
  private final Method holderDirect;
  private final Constructor<?> packet;
  private final Method registryOpsCreate;
  private final Object jsonOps;
  private final Object server;
  private final Method registryAccess;

  private DialogPackets() throws ReflectiveOperationException {
    final Class<?> dialog = Class.forName("net.minecraft.server.dialog.Dialog");
    this.codec = dialog.getField("DIRECT_CODEC").get(null);
    final Class<?> dynamicOps = Class.forName("com.mojang.serialization.DynamicOps");
    this.parse = Class.forName("com.mojang.serialization.Decoder").getMethod("parse", dynamicOps, Object.class);
    this.getOrThrow = Class.forName("com.mojang.serialization.DataResult").getMethod("getOrThrow");
    final Class<?> holder = Class.forName("net.minecraft.core.Holder");
    this.holderDirect = holder.getMethod("direct", Object.class);
    this.packet = Class.forName("net.minecraft.network.protocol.common.ClientboundShowDialogPacket").getConstructor(holder);
    final Class<?> provider = Class.forName("net.minecraft.core.HolderLookup$Provider");
    this.registryOpsCreate = Class.forName("net.minecraft.resources.RegistryOps").getMethod("create", dynamicOps, provider);
    this.jsonOps = Class.forName("com.mojang.serialization.JsonOps").getField("INSTANCE").get(null);
    final Class<?> minecraftServer = Class.forName("net.minecraft.server.MinecraftServer");
    this.server = minecraftServer.getMethod("getServer").invoke(null);
    this.registryAccess = minecraftServer.getMethod("registryAccess");
  }

  static @Nullable DialogPackets create() {
    try {
      return new DialogPackets();
    } catch (final ReflectiveOperationException | RuntimeException | LinkageError e) {
      return null;
    }
  }

  void send(final @NonNull Player player, final @NonNull String json) throws ReflectiveOperationException {
    final Object ops = this.registryOpsCreate.invoke(null, this.jsonOps, this.registryAccess.invoke(this.server));
    final Object result = this.parse.invoke(this.codec, ops, JsonParser.parseString(json));
    final Object dialog = this.getOrThrow.invoke(result);
    final Object packet = this.packet.newInstance(this.holderDirect.invoke(null, dialog));
    final Object handle = player.getClass().getMethod("getHandle").invoke(player);
    final Object connection = handle.getClass().getField("connection").get(handle);
    for (final Method method : connection.getClass().getMethods()) {
      if (method.getName().equals("send") && method.getParameterCount() == 1 && method.getParameterTypes()[0].isInstance(packet)) {
        method.invoke(connection, packet);
        return;
      }
    }
    throw new NoSuchMethodException("send(Packet)");
  }
}
