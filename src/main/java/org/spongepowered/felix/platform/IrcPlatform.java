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

import org.spongepowered.felix.command.CommandConfiguration;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.Types;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.kitteh.irc.client.library.Client;
import org.kitteh.irc.client.library.util.AcceptingTrustManagerFactory;

import java.lang.reflect.InvocationTargetException;

public final class IrcPlatform {
  private static final Logger LOGGER = LogManager.getLogger();
  private final Client client;

  public IrcPlatform(final ConfigurationNode config, final CommandConfiguration dcm) {
    this.client = Client.builder()
      .name("felix")
      .nick(config.getNode("nick").getString())
      .serverHost(config.getNode("host").getString())
      .serverPort(config.getNode("port").getInt())
      .secure(config.getNode("ssl").getBoolean())
      .secureTrustManagerFactory(new AcceptingTrustManagerFactory()) // TODO: Be less accepting
      .serverPassword(config.getNode("password").getString())
      .exceptionListener(e -> {
        if(e instanceof InvocationTargetException && e.getCause() != null) {
          LOGGER.error("Client Exception", e.getCause());
        } else {
          LOGGER.error("Client Exception", e);
        }
      })
      .outputListener(s -> LOGGER.debug("> " + s))
      .inputListener(s -> LOGGER.debug("< " + s))
      .build();
    this.client.connect();
    this.client.getEventManager().registerEventListener(new IrcCommandManager(dcm));
    for(final String channel : config.getNode("channels").getList(Types::asString)) {
      this.client.addChannel(channel);
    }
  }
}
