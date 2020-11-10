/*
 * Copyright 2019 John Grosh (john.a.grosh@gmail.com).
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
package com.jagrosh.vortex.commands.owner;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.vortex.Vortex;
import com.jagrosh.vortex.database.managers.PremiumManager;
import com.jagrosh.vortex.database.managers.PremiumManager.PremiumInfo;
import com.jagrosh.vortex.utils.OtherUtil;
import java.time.temporal.ChronoUnit;
import net.dv8tion.jda.api.entities.Guild;

import javax.swing.*;

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class PremiumCmd extends Command
{
    private final Vortex vortex;
    
    public PremiumCmd(Vortex vortex)
    {
        this.vortex = vortex;
        this.name = "premium";
        this.help = "gives premium";
        this.arguments = "<time> [type] [guild id]";
        this.ownerCommand = true;
        this.guildOnly = false;
        this.hidden = true;
    }

    @Override
    protected void execute(CommandEvent event)
    {

        if(event.getArgs().isEmpty())
        {
            PremiumInfo info = vortex.getDatabase().premium.getPremiumInfo(event.getGuild());
            event.reply("This server has " + info);
            return;
        }

        String[] parts = event.getArgs().split("\\s+", 3);

        int seconds;
        if(parts[0].equalsIgnoreCase("forever") || parts[0].equalsIgnoreCase("infinite"))
            seconds = Integer.MAX_VALUE;
        else
        {
            seconds = OtherUtil.parseTime(parts[0]);
            if(seconds <= 0)
            {
                event.replyError("Invalid time");
                return;
            }
        }

        PremiumManager.Level level = PremiumManager.Level.PRO;
        if(parts.length > 1)
        {
            try
            {
                level = PremiumManager.Level.valueOf(parts[1].toUpperCase());
            }
            catch(Exception e)
            {
                event.reply("Invalid level");
            }
        }

        long guildId = event.getGuild().getIdLong();
        if(parts.length > 2)
            guildId = Long.parseLong(parts[2]);


        Guild guild;
        try
        {
            guild = vortex.getShardManager().getGuildById(guildId);
        }
        catch(NumberFormatException ex)
        {
            event.replyError("Invalid guild");
            return;
        }
        if (guild == null)
        {
            event.replyError("Guild `" + guildId + "` not found");
            return;
        }
        PremiumInfo before = vortex.getDatabase().premium.getPremiumInfo(guild);

        if(seconds == Integer.MAX_VALUE)
            vortex.getDatabase().premium.addPremiumForever(guild, level);
        else
            vortex.getDatabase().premium.addPremium(guild, level, seconds, ChronoUnit.SECONDS);

        PremiumInfo after = vortex.getDatabase().premium.getPremiumInfo(guild);

        if(after.level == PremiumManager.Level.NONE)
        {
            vortex.getDatabase().automod.setResolveUrls(guild, false);
            vortex.getDatabase().settings.setAvatarLogChannel(guild, null);

            vortex.getDatabase().settings.setVoiceLogChannel(guild, null);
            vortex.getDatabase().filters.deleteAllFilters(guild.getIdLong());
        }
        else if(after.level == PremiumManager.Level.PLUS)
        {
            vortex.getDatabase().settings.setAvatarLogChannel(guild, null);
            vortex.getDatabase().automod.setResolveUrls(guild, false);
        }

        event.replySuccess("Before: " + before + "\n" + event.getClient().getSuccess() + " After: " + after);
    }
}
