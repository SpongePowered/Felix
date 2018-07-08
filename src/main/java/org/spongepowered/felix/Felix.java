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
package org.spongepowered.felix;

import org.spongepowered.felix.command.CommandConfiguration;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.gson.GsonConfigurationLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.felix.platform.DiscordPlatform;
import org.spongepowered.felix.platform.IrcPlatform;

import java.io.IOException;
import java.nio.file.Paths;

final class Felix {
  private static final Logger LOGGER = LogManager.getLogger();
  private final IrcPlatform irc;
  private final DiscordPlatform discord;

  Felix() throws IOException {
    LOGGER.info("Felix is starting up. Go get yourself a coffee.");
    final ConfigurationNode config = GsonConfigurationLoader.builder()
      .setPath(Paths.get("config.json"))
      .build()
      .load();
    final CommandConfiguration dcm = new CommandConfiguration(config);
    this.irc = new IrcPlatform(config.getNode("irc"), dcm);
    this.discord = new DiscordPlatform(config.getNode("discord"), config, dcm);
    LOGGER.info("We're ready to go.");
  }
}
