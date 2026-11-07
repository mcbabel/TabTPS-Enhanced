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
package xyz.jpenilla.tabtps.common.util;

import java.util.Locale;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.audience.MessageType;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.kyori.adventure.translation.GlobalTranslator;
import org.checkerframework.checker.nullness.qual.NonNull;
import xyz.jpenilla.tabtps.common.TabTPS;

public final class ConsoleAudience implements Audience {
  private final TabTPS tabTPS;
  private final Audience delegate;

  private ConsoleAudience(final @NonNull TabTPS tabTPS, final @NonNull Audience delegate) {
    this.tabTPS = tabTPS;
    this.delegate = delegate;
  }

  public static @NonNull Audience wrap(final @NonNull TabTPS tabTPS, final @NonNull Audience delegate) {
    return new ConsoleAudience(tabTPS, delegate);
  }

  private @NonNull Component prepare(final @NonNull Component message) {
    if (this.tabTPS.configManager().pluginSettings().consoleColors()) {
      return message;
    }
    return Component.text(PlainTextComponentSerializer.plainText().serialize(GlobalTranslator.render(message, Locale.getDefault())));
  }

  @Override
  public void sendMessage(final @NonNull Component message) {
    this.delegate.sendMessage(this.prepare(message));
  }

  @Override
  @SuppressWarnings("deprecation")
  public void sendMessage(final @NonNull Identity source, final @NonNull Component message, final @NonNull MessageType type) {
    this.delegate.sendMessage(this.prepare(message));
  }
}
