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

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.PrintStream;

/**
 * I'm a little teapot, short and stdout.
 *
 * Proxy calls to {@link System#out} and {@link System#err}, to record caller information in the log message.
 */
public final class TracingPrintStream extends PrintStream {
  private final Logger logger;
  private final Level level;

  public static void init() {
    System.setOut(new TracingPrintStream(LogManager.getLogger("STDOUT"), Level.INFO, System.out));
    System.setErr(new TracingPrintStream(LogManager.getLogger("STDERR"), Level.ERROR, System.err));
  }

  private TracingPrintStream(final Logger logger, final Level level, final PrintStream parent) {
    super(parent);
    this.logger = logger;
    this.level = level;
  }

  @Override
  public final void println(final String x) {
    if(this.level == Level.INFO) {
      this.logger.info(this.getPrefix() + x);
    } else if(this.level == Level.ERROR) {
      this.logger.error(this.getPrefix() + x);
    }
  }

  @Override
  public final void println(final Object x) {
    if(this.level == Level.INFO) {
      this.logger.info(this.getPrefix() + x);
    } else if(this.level == Level.ERROR) {
      this.logger.error(this.getPrefix() + x);
    }
  }

  private String getPrefix() {
    final StackTraceElement[] stack = Thread.currentThread().getStackTrace();
    final StackTraceElement element = stack[3];

    return "[" + element.getClassName() + ":" + element.getMethodName() + ":" + element.getLineNumber() + "]: ";
  }
}
