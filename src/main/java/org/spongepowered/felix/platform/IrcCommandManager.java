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
package org.spongepowered.felix.platform;

import com.sk89q.intake.CommandException;
import com.sk89q.intake.InvalidUsageException;
import com.sk89q.intake.InvocationCommandException;
import com.sk89q.intake.context.CommandContext;
import com.sk89q.intake.context.CommandLocals;
import com.sk89q.intake.dispatcher.Dispatcher;
import com.sk89q.intake.fluent.CommandGraph;
import com.sk89q.intake.parametric.ParametricBuilder;
import com.sk89q.intake.util.auth.AuthorizationException;
import org.spongepowered.felix.command.CommandConfiguration;
import org.spongepowered.felix.command.CommandUtil;
import org.spongepowered.felix.command.PhysicalCommand;
import org.spongepowered.felix.command.ProviderBinding;
import org.spongepowered.felix.command.Target;
import net.engio.mbassy.listener.Handler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.kitteh.irc.client.library.Client;
import org.kitteh.irc.client.library.element.Channel;
import org.kitteh.irc.client.library.element.User;
import org.kitteh.irc.client.library.event.channel.ChannelMessageEvent;

import java.util.List;

import javax.annotation.Nullable;

public final class IrcCommandManager {
  private static final Logger LOGGER = LogManager.getLogger();
  private final CommandConfiguration cc;
  private final Dispatcher dispatcher;

  public IrcCommandManager(final CommandConfiguration cc) {
    this.cc = cc;

    final ParametricBuilder builder = new ParametricBuilder();
    builder.addBinding(new ProviderBinding());
    this.dispatcher = new CommandGraph().builder(builder).getDispatcher();
    builder.registerMethodsAsCommands(this.dispatcher, new IrcManagementCommands(this.cc));
  }

  @Handler
  public void channelMessage(final ChannelMessageEvent event) {
    final String message = event.getMessage();
    if(message.length() < 2) {
      return;
    }

    final Channel channel = event.getChannel();
    final User user = event.getActor();

    if(this.cc.ignored(user.getNick())) {
      return;
    }

    final String arguments = message.substring(1);
    final String[] split = CommandContext.split(arguments);
    if(split.length < 1) {
      return;
    }

    if(this.dispatcher.contains(split[0])) {
      final CommandLocals namespace = new CommandLocals();
      namespace.put(Client.class, user.getClient());
      namespace.put(Channel.class, channel);
      namespace.put(User.class, user);

      try {
        this.dispatcher.call(arguments, namespace, new String[0]);
        LOGGER.info("Processed command '{}' from user '{}'", arguments, user.getName());
      } catch(final AuthorizationException ignored) {
        LOGGER.info("User was not permitted to run " + arguments);
      } catch(final InvocationCommandException e) {
        LOGGER.warn("Failed to execute a command", e);
        user.sendNotice("An unexpected error occurred while executing the command.");
      } catch(final InvalidUsageException e) {
        if(e.isFullHelpSuggested()) {
          user.sendNotice(e.getSimpleUsageString(String.valueOf(this.cc.prefix)));
        }
      } catch(final CommandException e) {
        user.sendNotice("error: " + e.getMessage());
      }
      return;
    }

    final java.util.Optional<User> clientUser = user.getClient().getUser();
    if(clientUser.isPresent() && clientUser.get().equals(user)) {
      LOGGER.warn("Skipping dynamic command for '" + split[0] + "' - " + user.getName() + " is an instance of Felix");
      return;
    }

    // Process target type
    final String tempName = split[0].toLowerCase();
    final Target targetType = Target.of(tempName);
    final String name = tempName.substring(targetType.substring);
    final String target = split.length > 1 ? CommandUtil.arrayToString(split, 1) : user.getNick();

    @Nullable final PhysicalCommand command = this.cc.get(name);

    // Verify that we have a command that we can process.
    if(command == null) {
      return;
    }

    final List<String> values = command.responses;

    // Determine which method we should use to send the value to the user(s).
    switch(targetType) {
      // Send a message to the channel, without a specific target.
      case DEFAULT:
        for(final String value : values) {
          channel.sendMessage(CommandUtil.wrapPrefix(this.cc.prefix, name, value));
        }
        break;
      // Send a message to the channel, prefixed with the target's name.
      case USER_PUBLIC:
        final String[] upuTargets = target.split(",");
        // Do not permit mass command sending - limit to 2 users at a time.
        if(upuTargets.length >= CommandConfiguration.MAX_TARGETS) {
          return;
        }

        for(String victim : upuTargets) {
          // Strip any whitespace that may be in the targets list.
          victim = victim.trim();

          for(final String value : values) {
            channel.sendMessage(victim + ": " + CommandUtil.wrapPrefix(this.cc.prefix, name, value));
          }
        }
        break;
      // Send a notice to a specific target.
      case USER_PRIVATE:
        final String[] uprTargets = target.split(",");
        // Do not permit mass command sending - limit to 2 users at a time.
        if(uprTargets.length >= CommandConfiguration.MAX_TARGETS) {
          return;
        }

        for(String victim : uprTargets) {
          // Strip any whitespace that may be in the targets list.
          victim = victim.trim();

          // Ensure that we actually have a target with this name.
          channel.getUser(victim).ifPresent(victimObj -> {
            for(final String value : values) {
              victimObj.sendNotice(CommandUtil.wrapPrefix(this.cc.prefix, name, value));
            }
          });
        }
        break;
      // Send a notice to the requestor.
      case SELF:
        for(final String value : values) {
          user.sendMultiLineNotice(CommandUtil.wrapPrefix(this.cc.prefix, name, value));
        }
        break;
    }
  }
}
