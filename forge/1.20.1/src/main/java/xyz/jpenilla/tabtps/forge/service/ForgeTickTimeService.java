/*
 * This file is part of TabTPS, licensed under the MIT License.
 *
 * Copyright (c) 2020-2024 Jason Penilla
 * Copyright (c) 2026 MCbabel (Forge support)
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
package xyz.jpenilla.tabtps.forge.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.concurrent.TimeUnit;
import net.minecraft.server.MinecraftServer;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import xyz.jpenilla.tabtps.common.service.TickTimeService;
import xyz.jpenilla.tabtps.common.util.RollingAverage;
import xyz.jpenilla.tabtps.common.util.TickTimes;
import xyz.jpenilla.tabtps.common.util.TickingState;

public final class ForgeTickTimeService implements TickTimeService {
  private static final long PAUSE_NANOS = TimeUnit.SECONDS.toNanos(5);

  private final @Nullable MinecraftServer server;
  private final TickTimes tickTimes5s = new TickTimes(100);
  private final TickTimes tickTimes10s = new TickTimes(200);
  private final TickTimes tickTimes60s = new TickTimes(1200);
  private final RollingAverage tps5s = new RollingAverage(5);
  private final RollingAverage tps1m = new RollingAverage(60);
  private final RollingAverage tps5m = new RollingAverage(60 * 5);
  private final RollingAverage tps15m = new RollingAverage(60 * 15);

  private int tickCount;
  private long tickStart;
  private long lastTickStart;
  private long previousSampleTime;
  private TickingState tickingState = TickingState.NOT_TICKING;

  public ForgeTickTimeService(final @Nullable MinecraftServer server) {
    this.server = server;
  }

  public void tickStart() {
    final long now = System.nanoTime();
    if (this.lastTickStart != 0 && now - this.lastTickStart > PAUSE_NANOS) {
      this.tickingState = TickingState.NOT_TICKING;
    }
    this.lastTickStart = now;
    this.tickStart = now;
  }

  public void tickEnd() {
    if (this.tickStart == 0) {
      return;
    }
    final long duration = System.nanoTime() - this.tickStart;
    this.tickCount++;
    this.tickTimes5s.add(this.tickCount, duration);
    this.tickTimes10s.add(this.tickCount, duration);
    this.tickTimes60s.add(this.tickCount, duration);

    if (this.tickCount % RollingAverage.SAMPLE_INTERVAL != 0) {
      return;
    }
    if (this.tickingState == TickingState.NOT_TICKING) {
      this.tickingState = TickingState.INITIALIZING;
    } else if (this.tickingState == TickingState.INITIALIZING) {
      this.tickingState = TickingState.TICKING;
    }
    final long diff = this.tickStart - this.previousSampleTime;
    this.previousSampleTime = this.tickStart;
    if (this.tickingState == TickingState.TICKING && diff > 0) {
      final BigDecimal currentTps = RollingAverage.TPS_BASE.divide(new BigDecimal(diff), 30, RoundingMode.HALF_UP);
      this.tps5s.add(currentTps, diff);
      this.tps1m.add(currentTps, diff);
      this.tps5m.add(currentTps, diff);
      this.tps15m.add(currentTps, diff);
    }
  }

  @Override
  public double averageMspt() {
    final MinecraftServer current = this.server;
    return current == null ? 0 : current.getAverageTickTime();
  }

  @Override
  public double @NonNull [] recentTps() {
    return new double[]{this.tps5s.average(), this.tps1m.average(), this.tps5m.average(), this.tps15m.average()};
  }

  public @NonNull TickTimes tickTimes5s() {
    return this.tickTimes5s;
  }

  public @NonNull TickTimes tickTimes10s() {
    return this.tickTimes10s;
  }

  public @NonNull TickTimes tickTimes60s() {
    return this.tickTimes60s;
  }
}
