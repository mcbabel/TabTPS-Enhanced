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
package xyz.jpenilla.tabtps.folia.placeholder;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import xyz.jpenilla.tabtps.common.util.Serializers;
import xyz.jpenilla.tabtps.folia.TabTPSFolia;

public final class MiniPlaceholdersHook {
  private static final String NATIVE_TAG = String.join(".", "net", "kyori", "adventure", "text", "minimessage", "tag", "Tag");

  private final Object expansion;
  private final Method unregister;

  private MiniPlaceholdersHook(final Object expansion, final Method unregister) {
    this.expansion = expansion;
    this.unregister = unregister;
  }

  public static @Nullable MiniPlaceholdersHook register(final @NonNull TabTPSFolia plugin) {
    final Plugin miniPlaceholders = plugin.getServer().getPluginManager().getPlugin("MiniPlaceholders");
    if (miniPlaceholders == null || !miniPlaceholders.isEnabled()) {
      return null;
    }
    try {
      final ClassLoader loader = miniPlaceholders.getClass().getClassLoader();
      final Class<?> expansionClass = Class.forName("io.github.miniplaceholders.api.Expansion", true, loader);
      final Class<?> tagClass = Class.forName(NATIVE_TAG, true, loader);
      final Method preProcessParsed = tagClass.getMethod("preProcessParsed", String.class);
      final Method builderMethod = expansionClass.getMethod("builder", String.class);
      final Class<?> builderClass = builderMethod.getReturnType();
      final Method audiencePlaceholder = placeholderMethod(builderClass, "audiencePlaceholder", tagClass);
      final Method globalPlaceholder = placeholderMethod(builderClass, "globalPlaceholder", tagClass);

      final PlaceholderValues values = new PlaceholderValues(plugin);
      final Object builder = builderMethod.invoke(null, "tabtps");
      for (final String key : PlaceholderValues.PLAYER_KEYS) {
        add(builder, audiencePlaceholder, key, values, preProcessParsed);
      }
      for (final String key : PlaceholderValues.GLOBAL_KEYS) {
        add(builder, globalPlaceholder, key, values, preProcessParsed);
      }
      for (final String key : PlaceholderValues.COLORED_KEYS) {
        final Method method = PlaceholderValues.PLAYER_KEYS.contains(key) ? audiencePlaceholder : globalPlaceholder;
        add(builder, method, key + PlaceholderValues.COLORED, values, preProcessParsed);
      }
      authorAndVersion(builderClass, builder, plugin);

      final Object expansion = builderClass.getMethod("build").invoke(builder);
      expansionClass.getMethod("register").invoke(expansion);
      plugin.logger().info("Registered MiniPlaceholders placeholders (<tabtps_...>)");
      return new MiniPlaceholdersHook(expansion, expansionClass.getMethod("unregister"));
    } catch (final ReflectiveOperationException | RuntimeException | LinkageError e) {
      plugin.logger().warn("Could not register MiniPlaceholders placeholders: {}", e.toString());
      return null;
    }
  }

  public void unregister() {
    try {
      this.unregister.invoke(this.expansion);
    } catch (final ReflectiveOperationException | RuntimeException ignored) {
    }
  }

  private static void add(
    final Object builder,
    final Method method,
    final String key,
    final PlaceholderValues values,
    final Method preProcessParsed
  ) throws ReflectiveOperationException {
    final Class<?> resolverType = method.getParameterTypes()[1];
    final Object resolver = Proxy.newProxyInstance(resolverType.getClassLoader(), new Class<?>[]{resolverType}, (proxy, invoked, args) -> {
      if (invoked.getDeclaringClass() == Object.class) {
        switch (invoked.getName()) {
          case "equals":
            return proxy == args[0];
          case "hashCode":
            return System.identityHashCode(proxy);
          default:
            return "TabTPS placeholder " + key;
        }
      }
      if (!Modifier.isAbstract(invoked.getModifiers())) {
        return null;
      }
      final Object audience = args != null && args.length == 3 ? args[0] : null;
      final Player player = audience instanceof Player ? (Player) audience : null;
      if (audience != null && player == null) {
        return null;
      }
      final Component value = values.resolve(player, key);
      return value == null ? null : preProcessParsed.invoke(null, Serializers.MINIMESSAGE.serialize(value));
    });
    method.invoke(builder, key, resolver);
  }

  private static Method placeholderMethod(final Class<?> builderClass, final String name, final Class<?> tagClass) throws NoSuchMethodException {
    for (final Method method : builderClass.getMethods()) {
      if (!method.getName().equals(name) || method.getParameterCount() != 2) {
        continue;
      }
      final Class<?>[] parameters = method.getParameterTypes();
      if (parameters[0] == String.class && parameters[1].isInterface() && parameters[1] != tagClass) {
        return method;
      }
    }
    throw new NoSuchMethodException(builderClass.getName() + "#" + name + "(String, resolver)");
  }

  private static void authorAndVersion(final Class<?> builderClass, final Object builder, final TabTPSFolia plugin) {
    try {
      builderClass.getMethod("author", String.class).invoke(builder, "MCbabel");
      builderClass.getMethod("version", String.class).invoke(builder, plugin.getDescription().getVersion());
    } catch (final ReflectiveOperationException | RuntimeException ignored) {
    }
  }
}
