/*
 * This file is part of felix, licensed under the MIT License.
 *
 * Copyright (c) Korobi <https://korobi.io>
 * Copyright (c) SpongePowered <https://spongepowered.org>
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
package org.spongepowered.felix.command;

public enum Target {
  DEFAULT(null, 0),
  USER_PUBLIC('>', 1),
  USER_PRIVATE('>', '>', 2),
  SELF('<', 1);

  private final Character a;
  private final Character b;
  public final int substring;

  Target(final Character a, final int substring) {
    this(a, null, substring);
  }

  Target(final Character a, final Character b, final int substring) {
    this.a = a;
    this.b = b;
    this.substring = substring;
  }

  public static Target of(final String arg) {
    if(arg.isEmpty()) {
      return DEFAULT;
    }

    final char c0 = arg.charAt(0);
    final boolean isUserPublic = c0 == USER_PUBLIC.a;

    if(!isUserPublic && c0 != SELF.a) {
      return DEFAULT;
    }

    if(c0 == USER_PRIVATE.a && (USER_PRIVATE.b != null && arg.length() >= 2 && arg.charAt(1) == USER_PRIVATE.b)) {
      return USER_PRIVATE;
    }

    if(isUserPublic) {
      return USER_PUBLIC;
    }

    return SELF;
  }
}
