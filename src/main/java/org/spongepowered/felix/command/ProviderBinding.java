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

import com.sk89q.intake.parametric.ParameterException;
import com.sk89q.intake.parametric.argument.ArgumentStack;
import com.sk89q.intake.parametric.binding.BindingBehavior;
import com.sk89q.intake.parametric.binding.BindingHelper;
import com.sk89q.intake.parametric.binding.BindingMatch;
import org.kitteh.irc.client.library.element.Channel;
import org.kitteh.irc.client.library.element.User;

import javax.annotation.Nullable;

public final class ProviderBinding extends BindingHelper {
  @BindingMatch(
    type = Channel.class,
    behavior = BindingBehavior.PROVIDES,
    consumedCount = -1
  )
  public Channel provideChannel(final ArgumentStack stack) {
    @Nullable final Channel channel = stack.getContext().getLocals().get(Channel.class);
    if(channel == null) {
      return null; // Do not throw
    } else {
      return channel;
    }
  }

  @BindingMatch(
    type = User.class,
    behavior = BindingBehavior.PROVIDES,
    consumedCount = -1
  )
  public User provideUser(final ArgumentStack stack) throws ParameterException {
    @Nullable final User user = stack.getContext().getLocals().get(User.class);
    if(user == null) {
      throw new ParameterException("The User is unavailable.");
    } else {
      return user;
    }
  }
}
