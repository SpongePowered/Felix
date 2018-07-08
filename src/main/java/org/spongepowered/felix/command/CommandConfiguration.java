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

import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.Types;
import org.spongepowered.felix.command.custom.CustomCommand;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class CommandConfiguration {
  private static final Gson GSON = new GsonBuilder().create();
  private static final Path PATH = Paths.get("commands.json");
  public static final Set<String> RESERVED_COMMAND_NAMES = Sets.newHashSet("addalias", "addcmd", "cmdinfo", "delcmd", "setcmd", "unsetcmd");
  public static final int MAX_TARGETS = 3;
  public final Map<String, PhysicalCommand> commands = new HashMap<>();
  public final Map<String, CustomCommand> customCommands = new HashMap<>();
  public final char prefix;
  private final Set<String> ignore;

  public CommandConfiguration(final ConfigurationNode config) throws IOException {
    this.prefix = config.getNode("command", "prefix").getString().charAt(0);
    this.ignore = new HashSet<>(config.getNode("command", "ignore").getList(Types::asString));

    this.read();
  }

  public void addCustomCommand(final String alias, final CustomCommand customCommand) {
    this.customCommands.put(alias, customCommand);
    RESERVED_COMMAND_NAMES.add(alias);
  }

  public PhysicalCommand get(final String alias) {
    return this.commands.get(alias);
  }

  public PhysicalCommand getOrCreate(final String alias) {
    return this.commands.getOrDefault(alias, new PhysicalCommand());
  }

  public void put(final PhysicalCommand command) {
    for(final String alias : command.aliases) {
      this.commands.put(alias.toLowerCase(Locale.ENGLISH), command);
    }
  }

  public void remove(final PhysicalCommand command) {
    for(final String alias : command.aliases) {
      this.commands.remove(alias.toLowerCase(Locale.ENGLISH));
    }
  }

  private void read() throws IOException {
    if(Files.exists(PATH)) {
      final PhysicalCommand[] commands = GSON.fromJson(new String(Files.readAllBytes(PATH), StandardCharsets.UTF_8), PhysicalCommand[].class);
      for(final PhysicalCommand command : commands) {
        this.put(command);
      }
    }
  }

  public void write() throws IOException {
    Files.write(Paths.get("commands.json"), GSON.toJson(this.commands.values()).getBytes(StandardCharsets.UTF_8));
  }

  public boolean ignored(final String string) {
    return this.ignore.contains(string);
  }
}
