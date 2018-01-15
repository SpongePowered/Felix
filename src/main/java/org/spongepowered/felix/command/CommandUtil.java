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

import com.google.common.collect.Lists;
import org.kitteh.irc.client.library.util.Format;

import java.util.List;

public final class CommandUtil {
  public static String arrayToString(final String[] array, final int index) {
    final StringBuilder result = new StringBuilder();
    for(final String item : copyArray(array, index)) {
      result.append(' ').append(item);
    }
    return result.toString().trim();
  }

  private static String[] copyArray(final String[] array, final int index) {
    List<String> args = Lists.newArrayList(array);
    args = args.subList(index, args.size());
    return args.toArray(new String[args.size()]);
  }

  public static String prefix(final char prefix, final String string) {
    return prefix + string;
  }

  public static String wrapPrefix(final char prefix, final String name, final String value) {
    return prefix + name + ": " + value;
  }

  public static String reset(final String string) {
    return Format.RESET + string + Format.RESET;
  }

  public static String quote(final String string) {
    return '\'' + reset(string) + '\'';
  }
}
