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

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.flattener.ComponentFlattener;
import net.kyori.adventure.translation.GlobalTranslator;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import xyz.jpenilla.tabtps.common.Messages;
import xyz.jpenilla.tabtps.common.TabTPS;
import xyz.jpenilla.tabtps.common.User;
import xyz.jpenilla.tabtps.common.config.PluginSettings;
import xyz.jpenilla.tabtps.common.config.Theme;
import xyz.jpenilla.tabtps.common.service.RegionStatsService;
import xyz.jpenilla.tabtps.common.service.TickTimeService;

import static net.kyori.adventure.text.Component.space;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.GRAY;
import static net.kyori.adventure.text.format.NamedTextColor.RED;
import static net.kyori.adventure.text.format.NamedTextColor.WHITE;

public final class LagAlerts {
  private final TabTPS tabTPS;
  private long laggingSince = -1;
  private long lastAlert = -1;
  private boolean alerting;

  public LagAlerts(final @NonNull TabTPS tabTPS) {
    this.tabTPS = tabTPS;
  }

  public void start() {
    this.tabTPS.executor().scheduleAtFixedRate(this::check, 5, 1, TimeUnit.SECONDS);
  }

  private void check() {
    try {
      this.tick();
    } catch (final RuntimeException e) {
      this.tabTPS.platform().logger().warn("Failed to check for lag", e);
    }
  }

  private synchronized void tick() {
    final PluginSettings.LagAlerts settings = this.tabTPS.configManager().pluginSettings().lagAlerts();
    if (!settings.enabled()) {
      this.laggingSince = -1;
      this.alerting = false;
      return;
    }
    final Sample sample = this.sample();
    final boolean lowTps = settings.tpsBelow() > 0 && sample.tps < settings.tpsBelow();
    final boolean highMspt = settings.msptAbove() > 0 && sample.mspt > settings.msptAbove();
    final long now = System.currentTimeMillis();

    if (!lowTps && !highMspt) {
      this.laggingSince = -1;
      if (this.alerting) {
        this.alerting = false;
        if (settings.notifyRecovery()) {
          this.send(Messages.MISC_LAG_ALERT_RECOVERED.styled(GRAY, tps(sample.tps), mspt(sample.mspt)), settings, false);
        }
      }
      return;
    }

    if (this.laggingSince < 0) {
      this.laggingSince = now;
    }
    final boolean longEnough = now - this.laggingSince >= TimeUnit.SECONDS.toMillis(settings.seconds());
    final boolean cooledDown = this.lastAlert < 0 || now - this.lastAlert >= TimeUnit.SECONDS.toMillis(settings.cooldownSeconds());
    if (this.alerting || !longEnough || !cooledDown) {
      return;
    }
    this.alerting = true;
    this.lastAlert = now;

    Component message = lowTps
      ? Messages.MISC_LAG_ALERT_LOW_TPS.styled(RED, text(TPSUtil.formatDouble(settings.tpsBelow()), WHITE), text(settings.seconds(), WHITE), tps(sample.tps))
      : Messages.MISC_LAG_ALERT_HIGH_MSPT.styled(RED, text(TPSUtil.formatDouble(settings.msptAbove()), WHITE), text(settings.seconds(), WHITE), mspt(sample.mspt));
    final RegionStatsService.Region region = sample.busiestRegion;
    if (region != null && region.located()) {
      message = message.append(space()).append(Messages.MISC_LAG_ALERT_REGION.styled(GRAY,
        text(region.world() + " " + region.blockX() + ", " + region.blockZ(), WHITE)));
    }
    this.send(message, settings, true);
  }

  private @NonNull Sample sample() {
    final RegionStatsService regionStats = this.tabTPS.platform().regionStatsService();
    if (regionStats != null) {
      final List<RegionStatsService.Region> regions = regionStats.regions();
      if (!regions.isEmpty()) {
        RegionStatsService.Region slowest = regions.get(0);
        RegionStatsService.Region busiest = regions.get(0);
        for (final RegionStatsService.Region region : regions) {
          if (region.tps() < slowest.tps()) {
            slowest = region;
          }
          if (region.mspt() > busiest.mspt()) {
            busiest = region;
          }
        }
        return new Sample(slowest.tps(), busiest.mspt(), busiest);
      }
    }
    final TickTimeService ticks = this.tabTPS.platform().tickTimeService();
    return new Sample(ticks.displayTps(), ticks.averageMspt(), null);
  }

  private void send(final @NonNull Component message, final PluginSettings.@NonNull LagAlerts settings, final boolean alert) {
    final Component prefixed = Components.ofChildren(Constants.PREFIX, space(), message);
    for (final User<?> user : this.tabTPS.platform().userService().onlineUsers()) {
      if (user.online() && user.hasPermission(Constants.PERMISSION_LAG_ALERT)) {
        user.sendMessage(prefixed);
      }
    }
    final String plain = plain(message);
    this.tabTPS.platform().logger().warn(plain);
    final String webhook = settings.discordWebhook();
    if (!webhook.isEmpty()) {
      if (DiscordWebhooks.valid(webhook)) {
        this.tabTPS.executor().execute(() -> this.postToDiscord(webhook, plain, alert));
      } else {
        this.tabTPS.platform().logger().warn("Ignoring discord-webhook in main.conf, it is not a Discord webhook URL (https://discord.com/api/webhooks/...)");
      }
    }
  }

  private void postToDiscord(final @NonNull String webhook, final @NonNull String content, final boolean alert) {
    final JsonObject body = new JsonObject();
    body.addProperty("username", "TabTPS");
    final JsonObject embed = new JsonObject();
    embed.addProperty("title", plain(alert ? Messages.MISC_LAG_ALERT_TITLE.plain() : Messages.MISC_LAG_ALERT_TITLE_RECOVERED.plain()));
    embed.addProperty("description", content);
    embed.addProperty("color", alert ? 0xEF4444 : 0x22C55E);
    final JsonArray embeds = new JsonArray();
    embeds.add(embed);
    body.add("embeds", embeds);
    final JsonObject mentions = new JsonObject();
    mentions.add("parse", new JsonArray());
    body.add("allowed_mentions", mentions);
    final byte[] bytes = body.toString().getBytes(StandardCharsets.UTF_8);
    try {
      final HttpURLConnection connection = (HttpURLConnection) new URL(webhook).openConnection();
      connection.setRequestMethod("POST");
      connection.setDoOutput(true);
      connection.setConnectTimeout(10000);
      connection.setReadTimeout(10000);
      connection.setRequestProperty("Content-Type", "application/json");
      connection.setRequestProperty("User-Agent", "MCbabel/TabTPS-Folia/" + Constants.TABTPS_VERSION);
      try (OutputStream out = connection.getOutputStream()) {
        out.write(bytes);
      }
      final int status = connection.getResponseCode();
      if (status < 200 || status >= 300) {
        this.tabTPS.platform().logger().warn("Discord webhook answered with HTTP {}", status);
      }
      connection.disconnect();
    } catch (final IOException | RuntimeException e) {
      this.tabTPS.platform().logger().warn("Failed to send lag alert to Discord: {}", e.getMessage());
    }
  }

  private static @NonNull Component tps(final double tps) {
    return TPSUtil.coloredTps(tps, Theme.DEFAULT.colorScheme());
  }

  private static @NonNull Component mspt(final double mspt) {
    return TPSUtil.coloredMspt(mspt, Theme.DEFAULT.colorScheme());
  }

  private static @NonNull String plain(final @NonNull Component component) {
    final StringBuilder builder = new StringBuilder();
    ComponentFlattener.basic().flatten(GlobalTranslator.render(component, Locale.getDefault()), builder::append);
    return builder.toString();
  }

  private static final class Sample {
    private final double tps;
    private final double mspt;
    private final RegionStatsService.@Nullable Region busiestRegion;

    Sample(final double tps, final double mspt, final RegionStatsService.@Nullable Region busiestRegion) {
      this.tps = tps;
      this.mspt = mspt;
      this.busiestRegion = busiestRegion;
    }
  }
}
