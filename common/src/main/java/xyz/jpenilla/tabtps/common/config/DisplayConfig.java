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
package xyz.jpenilla.tabtps.common.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;

import static net.kyori.adventure.text.Component.space;

@ConfigSerializable
public final class DisplayConfig {
  private static final String SEPARATOR_COMMENT = "The text used to separate modules. Accepts MiniMessage (i.e. \" <gray>|</gray> \")";
  private static final String FORMAT_COMMENT = "Optional custom text in MiniMessage format. When set, it replaces the module list above\n"
    + "Use a module name as tag for its value and <name_label> for its label, i.e. \"<gray>The server runs at <tps> TPS, your ping is <ping>\"\n"
    + "Leave empty (\"\") to keep the module list";

  @Comment("The permission required to use this display config\nCan be an empty string (\"\") to require no permission")
  private String permission = "tabtps.defaultdisplay";

  private ActionBarSettings actionBarSettings = new ActionBarSettings();
  private BossBarSettings bossBarSettings = new BossBarSettings();
  private TabSettings tabSettings = new TabSettings();

  public @NonNull String permission() {
    return this.permission;
  }

  public @NonNull ActionBarSettings actionBarSettings() {
    return this.actionBarSettings;
  }

  public @NonNull BossBarSettings bossBarSettings() {
    return this.bossBarSettings;
  }

  public @NonNull TabSettings tabSettings() {
    return this.tabSettings;
  }

  @Override
  public boolean equals(final @Nullable Object o) {
    if (this == o) return true;
    if (!(o instanceof DisplayConfig)) return false;
    final DisplayConfig that = (DisplayConfig) o;
    return this.permission.equals(that.permission)
      && this.actionBarSettings.equals(that.actionBarSettings)
      && this.bossBarSettings.equals(that.bossBarSettings)
      && this.tabSettings.equals(that.tabSettings);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.permission, this.actionBarSettings, this.bossBarSettings, this.tabSettings);
  }

  public interface DisplaySettings {
    boolean allow();

    boolean enableOnLogin();

    @NonNull String theme();

    @NonNull Component separator();
  }

  @ConfigSerializable
  public static final class ActionBarSettings implements DisplaySettings {
    private boolean allow = true;
    private boolean enableOnLogin = false;
    private String modules = "tps,mspt,ping";
    @Comment(FORMAT_COMMENT)
    private String format = "";
    private String theme = "default";

    @Comment(SEPARATOR_COMMENT)
    private Component separator = space();

    @Override
    public @NonNull Component separator() {
      return this.separator;
    }

    @Override
    public boolean enableOnLogin() {
      return this.enableOnLogin;
    }

    @Override
    public boolean allow() {
      return this.allow;
    }

    public @NonNull String modules() {
      return this.modules;
    }

    public @NonNull String format() {
      return this.format;
    }

    @Override
    public @NonNull String theme() {
      return this.theme;
    }
  }

  @ConfigSerializable
  public static final class BossBarSettings implements DisplaySettings {
    private boolean allow = true;
    private boolean enableOnLogin = false;
    private String modules = "tps,mspt,ping";
    @Comment(FORMAT_COMMENT)
    private String format = "";
    private String theme = "default";

    @Comment("Available colors: [PINK, RED, GREEN, BLUE, YELLOW, PURPLE, WHITE]")
    private Colors colors = new Colors();

    @Comment("Set the mode for determining boss bar fill.\nPossible values: [MSPT, TPS, REVERSE_MSPT, REVERSE_TPS]")
    private FillMode fillMode = FillMode.MSPT;

    @Comment("What kind of overlay should be used for the boss bar?\nMust be one of: [PROGRESS, NOTCHED_6, NOTCHED_10, NOTCHED_12, NOTCHED_20]")
    private BossBar.Overlay overlay = BossBar.Overlay.NOTCHED_20;

    @Comment(SEPARATOR_COMMENT)
    private Component separator = space();

    @Comment("Seconds to wait after a player joins before the boss bar appears, e.g. so boss bars of other plugins are shown first\n0 shows it right away")
    private int loginDelaySeconds = 0;

    @Override
    public @NonNull Component separator() {
      return this.separator;
    }

    @Override
    public boolean allow() {
      return this.allow;
    }

    @Override
    public boolean enableOnLogin() {
      return this.enableOnLogin;
    }

    public @NonNull String modules() {
      return this.modules;
    }

    public @NonNull String format() {
      return this.format;
    }

    public int loginDelaySeconds() {
      return Math.max(0, this.loginDelaySeconds);
    }

    public @NonNull FillMode fillMode() {
      return this.fillMode;
    }

    public BossBar.@NonNull Overlay overlay() {
      return this.overlay;
    }

    public @NonNull Colors colors() {
      return this.colors;
    }

    @Override
    public @NonNull String theme() {
      return this.theme;
    }

    public enum FillMode {
      TPS, MSPT, REVERSE_TPS, REVERSE_MSPT
    }

    @ConfigSerializable
    public static final class Colors {
      private BossBar.Color lowPerformance = BossBar.Color.RED;
      private BossBar.Color mediumPerformance = BossBar.Color.YELLOW;
      private BossBar.Color goodPerformance = BossBar.Color.GREEN;

      @Comment("Optional custom color levels by how full the boss bar is, in percent. Leave empty ([]) to use the three colors above\n"
        + "A level is used once the fill reaches its percent, the highest reached level wins, e.g.\n"
        + "  levels=[{percent=0, color=BLUE}, {percent=30, color=GREEN}, {percent=60, color=YELLOW}, {percent=90, color=RED}]")
      private List<Level> levels = new ArrayList<>();

      public BossBar.@Nullable Color levelColor(final float progress) {
        final double percent = progress * 100.0D;
        Level best = null;
        Level lowest = null;
        for (final Level level : this.levels) {
          if (percent >= level.percent() && (best == null || level.percent() > best.percent())) {
            best = level;
          }
          if (lowest == null || level.percent() < lowest.percent()) {
            lowest = level;
          }
        }
        final Level chosen = best != null ? best : lowest;
        return chosen == null ? null : chosen.color();
      }

      @ConfigSerializable
      public static final class Level {
        private double percent = 0;
        private BossBar.Color color = BossBar.Color.WHITE;

        public double percent() {
          return this.percent;
        }

        public BossBar.@NonNull Color color() {
          return this.color;
        }
      }

      public BossBar.@NonNull Color lowPerformance() {
        return this.lowPerformance;
      }

      public BossBar.@NonNull Color mediumPerformance() {
        return this.mediumPerformance;
      }

      public BossBar.@NonNull Color goodPerformance() {
        return this.goodPerformance;
      }
    }
  }

  @ConfigSerializable
  public static final class TabSettings implements DisplaySettings {
    private boolean allow = true;
    private boolean enableOnLogin = false;
    private String headerModules = "";
    @Comment(FORMAT_COMMENT)
    private String headerFormat = "";
    private String footerModules = "tps,mspt";
    @Comment(FORMAT_COMMENT)
    private String footerFormat = "";
    private String theme = "default";

    @Comment(SEPARATOR_COMMENT)
    private Component separator = space();

    @Override
    public @NonNull Component separator() {
      return this.separator;
    }

    @Override
    public boolean enableOnLogin() {
      return this.enableOnLogin;
    }

    @Override
    public boolean allow() {
      return this.allow;
    }

    public @NonNull String headerModules() {
      return this.headerModules;
    }

    public @NonNull String footerModules() {
      return this.footerModules;
    }

    public @NonNull String headerFormat() {
      return this.headerFormat;
    }

    public @NonNull String footerFormat() {
      return this.footerFormat;
    }

    @Override
    public @NonNull String theme() {
      return this.theme;
    }
  }
}
