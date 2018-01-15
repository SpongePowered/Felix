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

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.sk89q.intake.Command;
import com.sk89q.intake.context.CommandContext;
import com.sk89q.intake.parametric.annotation.Optional;
import com.sk89q.intake.parametric.annotation.Switch;
import org.spongepowered.felix.command.CommandConfiguration;
import org.spongepowered.felix.command.CommandUtil;
import org.spongepowered.felix.command.PhysicalCommand;
import org.spongepowered.felix.util.StringUtil;
import org.kitteh.irc.client.library.element.Channel;
import org.kitteh.irc.client.library.element.User;
import org.kitteh.irc.client.library.util.Format;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

public final class IrcManagementCommands {
  private final CommandConfiguration cc;

  IrcManagementCommands(final CommandConfiguration cc) {
    this.cc = cc;
  }

  @Command(
    aliases = {"cmdinfo"},
    desc = "Obtain information about a command."
  )
  public void commandInfo(final CommandContext args, final User user, final Channel channel) throws IOException {
    this.preProcessCommand(args, user, channel, CommandType.COMMAND, QueryType.INFO, false, -1);
  }

  @Command(
    aliases = {"addcmd", "setcmd"},
    desc = "Set a command for a channel.",
    usage = "[-a] [-v] <name> [value]"
  )
  public void setCommand(
    final CommandContext args, final User user, final Channel channel,
    @Switch('v') @Optional final boolean addValue
  ) throws IOException {
    this.preProcessCommand(args, user, channel, CommandType.COMMAND, QueryType.MODIFY, addValue, -1);
  }

  @Command(
    aliases = {"delcmd", "unsetcmd"},
    desc = "Remove a command or command alias.",
    usage = "[-i <index>] <name>"
  )
  public void delCommand(
    final CommandContext args, final User user, final Channel channel,
    @Switch('i') @Optional("-1") final int index
  ) throws IOException {
    if(args.argsLength() > 1) {
      user.sendNotice("Invalid usage.");
      return;
    }

    this.preProcessCommand(args, user, channel, CommandType.DELETE, QueryType.DELETE, false, index);
  }

  @Command(
    aliases = {"addalias"},
    desc = "Add an alias for an existing command.",
    usage = "<command name> <alias names...>"
  )
  public void addAlias(final CommandContext args, final User user, final Channel channel) throws IOException {
    this.preProcessCommand(args, user, channel, CommandType.ALIAS, QueryType.MODIFY, false, -1);
  }

  private void preProcessCommand(
    final CommandContext args, final User actor, final Channel defChannel,
    final CommandType type,
    final QueryType queryType,
    final boolean addValue,
    final int delIndex
  ) throws IOException {
    if(defChannel == null) {
      actor.sendNotice("Invalid channel.");
      return;
    }

    if(args.argsLength() == 0) {
      actor.sendNotice("Usage: " + type.usage(this.cc.prefix));
      return;
    }

    // check permissions
    if(!defChannel.getUserModes(actor).map(modes -> modes.stream().anyMatch(mode -> mode.getNickPrefix() == '@')).orElse(false)) {
      actor.sendNotice("You don't have permission to modify commands.");
      return;
    }

    this.processCommand(args, actor, type, queryType, addValue, delIndex);
  }

  private void processCommand(
    final CommandContext args, final User actor,
    final CommandType type,
    final QueryType queryType,
    final boolean addValue,
    final int delIndex
  ) throws IOException {
    final String name = args.getString(0).toLowerCase(); // COMMAND = name, ALIAS = command name
    final String newValue = args.argsLength() > 1 ? args.getJoinedStrings(1) : null;

    if(!String.valueOf(name.charAt(0)).matches("^[a-zA-Z0-9]$")) {
      actor.sendNotice("Illegal command name. Command names must start with an alphanumeric character.");
      return;
    }

    if(CommandConfiguration.RESERVED_COMMAND_NAMES.contains(name)) {
      actor.sendNotice("Sorry, but '" + name + "' is a reserved command name.");
      return;
    }

    // We're getting information about an existing command, or command alias.
    if(queryType == QueryType.INFO) {
      @Nullable final PhysicalCommand command = this.cc.get(name);
      if(command == null) {
        actor.sendMultiLineNotice(type.notFound(this.cc.prefix, name));
        return;
      }

      // Always send values
      for(final String value : command.responses) {
        actor.sendMultiLineNotice(queryType.value(this.cc.prefix, name, command.responses.indexOf(value), value, false));
      }
    } else if(newValue == null || newValue.isEmpty()) {
      @Nullable final PhysicalCommand command = this.cc.get(name);
      if(command == null) {
        actor.sendMultiLineNotice(type.notFound(this.cc.prefix, name));
      } else {
        // korobi/Korobi#44 - values should be 1-indexed
        if(delIndex == 0) {
          actor.sendNotice("Value indexes are 1-indexed.");
          return;
        }

        final int deletionIndex = (delIndex - 1);

        final String result;

        if(deletionIndex > 0) {
          // wheeeee
          final List<String> values = command.responses;
          if(deletionIndex > values.size()) {
            actor.sendNotice("Invalid index.");
            return;
          }

          final String originalValue = values.get(deletionIndex);
          values.remove(deletionIndex);
          command.responses = values;

          // create remove message
          result = type.removeValue(this.cc.prefix, name, originalValue);
        } else {
          this.cc.remove(command);

          result = type.removeWithAliases(this.cc.prefix, name, command.responses, new HashSet<>(command.aliases));
        }

        actor.sendMultiLineNotice(result);
        this.cc.write();
      }
    } else {
      if(type == CommandType.ALIAS) {
        @Nullable final PhysicalCommand parent = this.cc.get(name);
        if(parent == null) {
          actor.sendMultiLineNotice(type.parent(this.cc.prefix, name));
          return;
        }

        final List<String> added = Lists.newArrayList();
        final Map<String, String> replaceMap = Maps.newConcurrentMap();

        final String[] aliases = newValue.split(" "); // multi-set alias
        for(final String alias : aliases) {
          if(CommandConfiguration.RESERVED_COMMAND_NAMES.contains(alias)) {
            actor.sendNotice("Sorry, but '" + alias + "' is a reserved command name.");
            return;
          }

          final PhysicalCommand command = this.cc.get(alias);
          if(command != null) {
            if(command.responses.isEmpty()) {
              added.add(alias);
            } else {
              for(final String value : command.responses) {
                replaceMap.put(value, alias);
              }
            }

            this.cc.remove(command);
          }
          parent.aliases.add(alias);
          this.cc.put(parent);
        }

        if(!added.isEmpty()) {
          actor.sendMultiLineNotice(type.add(this.cc.prefix, name, added, false));
        }

        for(final Map.Entry<String, String> entry : replaceMap.entrySet()) {
          actor.sendMultiLineNotice(type.replace(this.cc.prefix, name, Lists.newArrayList(entry.getValue()), Lists.newArrayList(entry.getKey())));
        }

        this.cc.write();
      } else {
        final PhysicalCommand command = this.cc.getOrCreate(name);
        command.aliases.add(name.toLowerCase(Locale.ENGLISH));
        final List<String> oldValue = command.responses;

        if(addValue && !command.responses.isEmpty()) {
          final List<String> values = command.responses;
          values.add(newValue);
          if(values.size() > 3) {
            actor.sendNotice("I'm sorry, but that command has reached the value limit (3).");
            return;
          }
        } else {
          command.responses = Lists.newArrayList(newValue);
        }

        this.cc.put(command);
        this.cc.write();

        if(oldValue.isEmpty()) {
          actor.sendMultiLineNotice(type.add(this.cc.prefix, name, Lists.newArrayList(newValue), false));
        } else {
          actor.sendMultiLineNotice(type.replace(this.cc.prefix, name, command.responses, oldValue));
        }
      }
    }
  }

  enum CommandType {
    /*
     * Conventions for this class:
     * Template strings have no quoting anywhere.
     * Quotes are applied at replace-time, if needed for the value.
     * Resets are always used around input - it's user input, make it work.
     */

    COMMAND(
      "Command {0} has been set to {1}.",
      null, // multiAdd
      "Command {0} has been set to {1}. Old value: {2}.",
      "Command {0} removed. Old value: {1}.",
      "Command {0} removed. Old value: {1}. (Alias {2} was also removed.)",
      "Command {0} removed. Old value: {1}. (Aliases {2} were also removed.)",
      "Value {0} added to command {1}.",
      "Value {0} removed from command {1}.",
      "Could not find a command by that name.",
      null, // parent
      "addcmd <name> [value]"
    ),
    DELETE(
      COMMAND,
      "delcmd <name>"
    ),
    ALIAS(
      "Alias {1} has been set to {0}.", // add
      "Aliases {1} have been set to {0}.", // multiAdd
      "Alias {0} has been set to {1}. Old value: {2}.", // replace
      null, // remove
      null, // removeWithAlias
      null, // removeWithAliases
      "Aliases can only have one value.", // addValue
      "Alias {0} removed. Old value: {1}.", // removeValue
      "Could not find an alias by that name.", // notFound
      "Could not find the parent command {0}.", // parent
      "addalias <command name> <alias names...>" // usage
    );

    private final String add;
    private final String multiAdd;
    private final String replace;
    private final String remove;
    private final String removeWithAlias;
    private final String removeWithAliases;
    private final String addValue;
    private final String removeValue;
    private final String notFound;
    private final String parent;
    private final String usage;

    CommandType(final CommandType parent, final String usage) {
      this.add = parent.add;
      this.multiAdd = parent.multiAdd;
      this.replace = parent.replace;
      this.remove = parent.remove;
      this.removeWithAlias = parent.removeWithAlias;
      this.removeWithAliases = parent.removeWithAliases;
      this.addValue = parent.addValue;
      this.removeValue = parent.removeValue;
      this.notFound = parent.notFound;
      this.parent = parent.parent;
      this.usage = usage;
    }

    CommandType(final String add, final String multiAdd, final String replace, final String remove, final String removeWithAlias, final String removeWithAliases, final String addValue, final String removeValue, final String notFound, final String parent, final String usage) {
      this.add = add;
      this.multiAdd = multiAdd;
      this.replace = replace;
      this.remove = remove;
      this.removeWithAlias = removeWithAlias;
      this.removeWithAliases = removeWithAliases;
      this.addValue = addValue;
      this.removeValue = removeValue;
      this.notFound = notFound;
      this.parent = parent;
      this.usage = usage;
    }

    public String add(final char prefix, final String name, final List<String> value, final boolean valueAdd) {
      if(valueAdd) {
        return this.addValue
          .replace("{0}", StringUtil.listToEnglishCompound(value, "'" + Format.RESET, Format.RESET + "'"))
          .replace("{1}", CommandUtil.quote(CommandUtil.prefix(prefix, name)));
      } else {
        if(value.size() > 1) {
          if(this == ALIAS) {
            return this.multiAdd
              .replace("{0}", CommandUtil.quote(CommandUtil.prefix(prefix, name)))
              .replace("{1}", StringUtil.listToEnglishCompound(value, "'" + Format.RESET, Format.RESET + "'", string -> CommandUtil.reset(CommandUtil.prefix(prefix, string))));
          } else {
            return this.multiAdd
              .replace("{0}", CommandUtil.quote(CommandUtil.prefix(prefix, name)))
              .replace("{1}", StringUtil.listToEnglishCompound(value, "'" + Format.RESET, Format.RESET + "'"));
          }
        } else {
          String value0 = value.get(0);
          if(this == ALIAS) {
            value0 = CommandUtil.prefix(prefix, value0);
          }

          return this.add
            .replace("{0}", CommandUtil.quote(CommandUtil.prefix(prefix, name)))
            .replace("{1}", CommandUtil.quote(value0));
        }
      }
    }

    public String replace(final char prefix, final String name, final List<String> newValue, final List<String> oldValue) {
      return this.replace
        .replace("{0}", CommandUtil.quote(CommandUtil.prefix(prefix, name)))
        .replace("{1}", StringUtil.listToEnglishCompound(newValue, "'" + Format.RESET, Format.RESET + "'"))
        .replace("{2}", StringUtil.listToEnglishCompound(oldValue, "'" + Format.RESET, Format.RESET + "'"));
    }

    public String remove(final char prefix, final String name, final List<String> oldValue) {
      return this.remove
        .replace("{0}", CommandUtil.quote(CommandUtil.prefix(prefix, name)))
        .replace("{1}", StringUtil.listToEnglishCompound(oldValue, "'" + Format.RESET, Format.RESET + "'"));
    }

    public String removeValue(final char prefix, final String name, final String value) {
      return this.removeValue
        .replace("{0}", CommandUtil.quote(value))
        .replace("{1}", CommandUtil.quote(CommandUtil.prefix(prefix, name)));
    }

    public String removeWithAliases(final char prefix, final String name, final List<String> oldValue, final Set<String> aliases) {
      aliases.remove(name);
      final String string = aliases.size() == 1 ? this.removeWithAlias : this.removeWithAliases;
      return string
        .replace("{0}", CommandUtil.quote(CommandUtil.prefix(prefix, name)))
        .replace("{1}", StringUtil.listToEnglishCompound(oldValue, "'" + Format.RESET, Format.RESET + "'"))
        .replace("{2}", StringUtil.listToEnglishCompound(aliases, "'" + Format.RESET, Format.RESET + "'"));
    }

    public String notFound(final char prefix, final String name) {
      return this.notFound.replace("{0}", CommandUtil.quote(CommandUtil.prefix(prefix, name)));
    }

    public String parent(final char prefix, final String parent) {
      return this.parent.replace("{0}", CommandUtil.quote(CommandUtil.prefix(prefix, parent)));
    }

    public String usage(final char prefix) {
      return CommandUtil.prefix(prefix, this.usage);
    }
  }

  enum QueryType {
    INFO(
      "Command '{0}' has value #{2} '{1}'.",
      "Command '{0}' is an alias of '{1}'.",
      "Command '{0}' has aliases {1}."
    ),
    MODIFY,
    DELETE;

    private final String value;
    private final String valueAlias;
    private final String aliases;

    QueryType() {
      this(null, null, null);
    }

    QueryType(final String value, final String valueAlias, final String aliases) {
      this.value = value;
      this.valueAlias = valueAlias;
      this.aliases = aliases;
    }

    public String value(final char prefix, final String name, final int index, final String value, final boolean alias) {
      final String type = alias ? this.valueAlias : this.value;
      return type
        .replace("{0}", CommandUtil.prefix(prefix, name))
        .replace("{1}", CommandUtil.reset(alias ? CommandUtil.prefix(prefix, value) : value))
        .replace("{2}", String.valueOf(index));
    }

    public String aliases(final char prefix, final String name, final List<String> aliases) {
      return this.aliases
        .replace("{0}", CommandUtil.prefix(prefix, name))
        .replace("{1}", StringUtil.listToEnglishCompound(aliases, "'", "'"));
    }
  }
}
