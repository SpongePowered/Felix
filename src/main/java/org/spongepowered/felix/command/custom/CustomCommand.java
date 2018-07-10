package org.spongepowered.felix.command.custom;

import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;

public interface CustomCommand {

    void process(String[] args,MessageReceivedEvent event);

}
