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
package org.spongepowered.felix.util;

import java.util.Collection;
import java.util.function.Function;

import javax.annotation.Nullable;

public final class StringUtil {
  /**
   * Converts a list of strings to a nice English list as a string.
   * For example:
   *    In: ["Purple", "Pink", "Red", "Orange"]
   *    Out: "Purple, Pink, Red and Orange"
   *
   * @param list list of strings to concatenate
   * @param prefix prefix to add before each element in the resulting string
   * @param suffix suffix to add after each element in the resulting string
   * @return string version of the list of strings
   */
  public static <T> String listToEnglishCompound(final Collection<? extends T> list, final String prefix, final String suffix) {
    return listToEnglishCompound(list, prefix, suffix, new Function<T, T>() {
      @Nullable
      @Override
      public T apply(@Nullable final T t) {
        return t;
      }
    });
  }

  /**
   * Converts a list of strings to a nice English list as a string.
   * For example:
   *    In: ["Purple", "Pink", "Red", "Orange"]
   *    Out: "Purple, Pink, Red and Orange"
   *
   * @param list list of strings to concatenate
   * @param prefix prefix to add before each element in the resulting string
   * @param suffix suffix to add after each element in the resulting string
   * @param transformer the transformer to apply to the value
   * @return string version of the list of strings
   */
  public static <T> String listToEnglishCompound(final Collection<? extends T> list, final String prefix, final String suffix, final Function<T, T> transformer) {
    return listToEnglishCompound(list, "and", prefix, suffix, transformer);
  }

  /**
   * Converts a list of strings to a nice English list as a string.
   * For example:
   *    In: ["Purple", "Pink", "Red", "Orange"]
   *    Conjunction: "and"
   *    Out: "Purple, Pink, Red and Orange"
   *
   * @param list list of strings to concatenate
   * @param prefix prefix to add before each element in the resulting string
   * @param suffix suffix to add after each element in the resulting string
   * @param transformer the transformer to apply to the value
   * @return string version of the list of strings
   */
  private static <T> String listToEnglishCompound(final Collection<? extends T> list, final String conjunction, final String prefix, final String suffix, final Function<T, T> transformer) {
    final StringBuilder builder = new StringBuilder();
    int i = 0;
    for(final T string : list) {
      if(i != 0) {
        if(i == list.size() - 1) {
          builder.append(' ').append(conjunction).append(' ');
        } else {
          builder.append(", ");
        }
      }

      builder.append(prefix).append(transformer.apply(string)).append(suffix);
      i++;
    }

    return builder.toString();
  }
}
