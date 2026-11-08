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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.NonNull;

public final class DiscordWebhooks {
  private static final Set<String> HOSTS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
    "discord.com", "discordapp.com", "ptb.discord.com", "canary.discord.com"
  )));

  private DiscordWebhooks() {
  }

  public static boolean valid(final @NonNull String url) {
    try {
      final URI uri = new URI(url.trim());
      return "https".equalsIgnoreCase(uri.getScheme())
        && uri.getHost() != null
        && HOSTS.contains(uri.getHost().toLowerCase(Locale.ROOT))
        && uri.getPort() == -1
        && uri.getUserInfo() == null
        && uri.getPath() != null
        && uri.getPath().startsWith("/api/webhooks/");
    } catch (final URISyntaxException e) {
      return false;
    }
  }
}
