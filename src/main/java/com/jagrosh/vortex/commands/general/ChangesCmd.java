/*
 * Copyright 2018 John Grosh (john.a.grosh@gmail.com).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jagrosh.vortex.commands.general;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.Permission;

import java.awt.*;

/**
 *
 * @author Michaili K (mysteriouscursor+git@protonmail.com)
 */
public class ChangesCmd extends Command
{
    public ChangesCmd()
    {
        this.name = "changes";
        this.help = "shows difference between fork & original about the bot";
        this.guildOnly = false;
        this.botPermissions = new Permission[]{Permission.MESSAGE_EMBED_LINKS};
    }

    @Override
    protected void execute(CommandEvent event)
    {
        event.reply(new MessageBuilder()
                .setEmbed(new EmbedBuilder()
                        .setColor(event.getGuild()==null ? Color.GRAY : event.getSelfMember().getColor())
                        .setDescription("The difference between this bot and jagrosh's bot can be found [here](https://github.com/MichailiK/Vortex/blob/master/Changes.md)\n")
                        .build())
                .build());
    }
}
