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

import com.google.common.base.Charsets;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class UpdateChecker {
  private static final JsonParser parser = new JsonParser();
  private static final String PROJECT = "6n5IVh64";

  private UpdateChecker() {
  }

  public static @NonNull Result check(final @NonNull String version, final @NonNull String loader, final @NonNull String minecraftVersion) {
    final JsonArray result;
    try {
      final String query = "loaders=" + encode("[\"" + loader + "\"]") + "&game_versions=" + encode("[\"" + minecraftVersion + "\"]");
      final URLConnection connection = new URL("https://api.modrinth.com/v2/project/" + PROJECT + "/version?" + query).openConnection();
      connection.setRequestProperty("User-Agent", "MCbabel/TabTPS-Enhanced/" + version);
      connection.setConnectTimeout(10000);
      connection.setReadTimeout(10000);
      try (Reader reader = new InputStreamReader(connection.getInputStream(), Charsets.UTF_8)) {
        result = parser.parse(reader).getAsJsonArray();
      }
    } catch (final IOException | RuntimeException exception) {
      return new Result(Collections.singletonList("Failed to look for updates: " + exception.getMessage()), null);
    }

    final Set<String> newer = new LinkedHashSet<>();
    String latestId = null;
    for (final JsonElement element : result) {
      final JsonObject object = element.getAsJsonObject();
      if (!"release".equals(object.get("version_type").getAsString())) {
        continue;
      }
      final String candidate = baseVersion(object.get("version_number").getAsString());
      if (compare(candidate, baseVersion(version)) > 0) {
        if (latestId == null) {
          latestId = object.get("id").getAsString();
        }
        newer.add(candidate);
      }
    }
    if (newer.isEmpty()) {
      return new Result(Collections.emptyList(), null);
    }

    final Update update = new Update(version, newer.iterator().next(), "https://modrinth.com/project/" + PROJECT + "/version/" + latestId);
    final List<String> list = new ArrayList<>();
    list.add("There is an update available for TabTPS!");
    list.add("This server is running v" + version + ", the latest version for Minecraft " + minecraftVersion + " is v" + update.latest() + " (" + newer.size() + " " + (newer.size() == 1 ? "version" : "versions") + " behind).");
    list.add("Download it from Modrinth: " + update.url());
    return new Result(list, update);
  }

  public static final class Result {
    private final List<String> lines;
    private final @Nullable Update update;

    Result(final @NonNull List<String> lines, final @Nullable Update update) {
      this.lines = lines;
      this.update = update;
    }

    public @NonNull List<String> lines() {
      return this.lines;
    }

    public @Nullable Update update() {
      return this.update;
    }
  }

  public static final class Update {
    private final String current;
    private final String latest;
    private final String url;

    Update(final @NonNull String current, final @NonNull String latest, final @NonNull String url) {
      this.current = current;
      this.latest = latest;
      this.url = url;
    }

    public @NonNull String current() {
      return this.current;
    }

    public @NonNull String latest() {
      return this.latest;
    }

    public @NonNull String url() {
      return this.url;
    }
  }

  private static @NonNull String encode(final @NonNull String value) throws IOException {
    return URLEncoder.encode(value, "UTF-8");
  }

  private static @NonNull String baseVersion(final @NonNull String version) {
    final int end = indexOfAny(version, '+', '-');
    return end == -1 ? version : version.substring(0, end);
  }

  private static int indexOfAny(final @NonNull String value, final char first, final char second) {
    final int a = value.indexOf(first);
    final int b = value.indexOf(second);
    if (a == -1) {
      return b;
    }
    return b == -1 ? a : Math.min(a, b);
  }

  static int compare(final @NonNull String first, final @NonNull String second) {
    final String[] a = first.split("\\.");
    final String[] b = second.split("\\.");
    for (int i = 0; i < Math.max(a.length, b.length); i++) {
      final int x = i < a.length ? parse(a[i]) : 0;
      final int y = i < b.length ? parse(b[i]) : 0;
      if (x != y) {
        return Integer.compare(x, y);
      }
    }
    return 0;
  }

  private static int parse(final @NonNull String part) {
    try {
      return Integer.parseInt(part);
    } catch (final NumberFormatException e) {
      return 0;
    }
  }
}
