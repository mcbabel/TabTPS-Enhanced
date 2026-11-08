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
package xyz.jpenilla.tabtps.common.gui;

import java.util.LinkedHashMap;
import java.util.Map;
import org.checkerframework.checker.nullness.qual.NonNull;

final class ClickPayload {
  private final String input;
  private int index;

  private ClickPayload(final String input) {
    this.input = input;
  }

  static @NonNull Map<String, String> parse(final @NonNull String snbt) {
    final ClickPayload parser = new ClickPayload(snbt);
    final Map<String, String> values = new LinkedHashMap<>();
    parser.skipWhitespace();
    if (!parser.consume('{')) {
      return values;
    }
    while (true) {
      parser.skipWhitespace();
      if (parser.done() || parser.consume('}')) {
        return values;
      }
      final String key = parser.token();
      parser.skipWhitespace();
      if (!parser.consume(':')) {
        return values;
      }
      parser.skipWhitespace();
      final String value = parser.value();
      if (value != null) {
        values.put(key, value);
      }
      parser.skipWhitespace();
      parser.consume(',');
    }
  }

  private String value() {
    final char first = this.peek();
    if (first == '{' || first == '[') {
      this.skipNested();
      return null;
    }
    final boolean quoted = first == '"' || first == '\'';
    final String token = this.token();
    if (quoted || token.length() < 2) {
      return token;
    }
    final char suffix = Character.toLowerCase(token.charAt(token.length() - 1));
    if ("bsfdl".indexOf(suffix) >= 0 && Character.isDigit(token.charAt(token.length() - 2))) {
      return token.substring(0, token.length() - 1);
    }
    return token;
  }

  private String token() {
    final char first = this.peek();
    if (first == '"' || first == '\'') {
      this.index++;
      final StringBuilder builder = new StringBuilder();
      while (!this.done()) {
        final char c = this.input.charAt(this.index++);
        if (c == '\\' && !this.done()) {
          builder.append(this.input.charAt(this.index++));
        } else if (c == first) {
          break;
        } else {
          builder.append(c);
        }
      }
      return builder.toString();
    }
    final int start = this.index;
    while (!this.done() && ",:}] \t\n\r".indexOf(this.peek()) < 0) {
      this.index++;
    }
    return this.input.substring(start, this.index);
  }

  private void skipNested() {
    int depth = 0;
    char quote = 0;
    while (!this.done()) {
      final char c = this.input.charAt(this.index++);
      if (quote != 0) {
        if (c == '\\') {
          this.index++;
        } else if (c == quote) {
          quote = 0;
        }
      } else if (c == '"' || c == '\'') {
        quote = c;
      } else if (c == '{' || c == '[') {
        depth++;
      } else if ((c == '}' || c == ']') && --depth == 0) {
        return;
      }
    }
  }

  private void skipWhitespace() {
    while (!this.done() && Character.isWhitespace(this.peek())) {
      this.index++;
    }
  }

  private boolean consume(final char expected) {
    if (!this.done() && this.peek() == expected) {
      this.index++;
      return true;
    }
    return false;
  }

  private char peek() {
    return this.done() ? 0 : this.input.charAt(this.index);
  }

  private boolean done() {
    return this.index >= this.input.length();
  }
}
