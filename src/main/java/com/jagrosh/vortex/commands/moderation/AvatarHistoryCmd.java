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
package com.jagrosh.vortex.commands.moderation;

import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.commons.utils.FinderUtil;
import com.jagrosh.jdautilities.menu.Paginator;
import com.jagrosh.vortex.Vortex;
import com.jagrosh.vortex.commands.ModCommand;
import com.jagrosh.vortex.database.managers.PremiumManager;
import com.jagrosh.vortex.utils.FormatUtil;
import com.jagrosh.vortex.utils.Pair;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.exceptions.PermissionException;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author Michaili K (mysteriouscursor+git@protonmail.com)
 */
public class AvatarHistoryCmd extends ModCommand
{
    public AvatarHistoryCmd(Vortex vortex)
    {
        super(vortex, Permission.BAN_MEMBERS);
        this.name = "avatarhistory";
        this.arguments = "<user>";
        this.help = "sees the avatar history from a user";
        this.aliases = new String[] {"avatars", "pfps", "profilepictures"};
        this.botPermissions = new Permission[]{Permission.MESSAGE_EMBED_LINKS};
    }
    
    @Override
    protected void execute(CommandEvent event)
    {
        if(!vortex.getDatabase().premium.getPremiumInfo(event.getGuild()).level.isAtLeast(PremiumManager.Level.PRO))
        {
            event.reply(PremiumManager.Level.PRO.getRequirementMessage());
            return;
        }
        if(event.getArgs().isEmpty() || event.getArgs().equalsIgnoreCase("help"))
        {
            event.replySuccess("This command is used to see a user's past avatars. Please include a user or user ID to check.");
            return;
        }
        event.getChannel().sendTyping().queue();
        List<Member> members = FinderUtil.findMembers(event.getArgs(), event.getGuild());
        if(!members.isEmpty())
        {
            check(event, members.get(0).getUser());
            return;
        }
        List<User> users = FinderUtil.findUsers(event.getArgs(), event.getJDA());
        if(!users.isEmpty())
        {
            check(event, users.get(0));
            return;
        }
        try
        {
            Long uid = Long.parseLong(event.getArgs());
            User u = vortex.getShardManager().getUserById(uid);
            if(u!=null)
                check(event, u);
            else
                event.getJDA().retrieveUserById(uid).queue(
                        user -> check(event, user), 
                        v -> event.replyError("`"+uid+"` is not a valid user ID"));
        }
        catch(Exception ex)
        {
            event.replyError(FormatUtil.filterEveryone("Could not find a user `"+event.getArgs()+"`"));
        }
    }
    
    private void check(CommandEvent event, User user)
    {
        Collection<Pair<Instant, String>> avatars = vortex.getDatabase().avatarHistory.getPastAvatars(user);
        if(avatars.isEmpty())
        {
            event.replyWarning("This user does not have any past avatars recorded.");
            return;
        }

        ZoneId serverTimeZone = vortex.getDatabase().settings.getSettings(event.getGuild()).getTimezone();
        Paginator.Builder builder = new Paginator.Builder()
                .setText("Avatar History for **"+user.getName()+"**#"+user.getDiscriminator()+" (ID:"+user.getId()+")")
                .setColumns(1)
                .setFinalAction(m -> {try{m.clearReactions().queue();}catch(PermissionException ignore){}})
                .setItemsPerPage(10)
                .waitOnSinglePage(false)
                .showPageNumbers(true)
                .wrapPageEnds(true)
                .setEventWaiter(vortex.getEventWaiter())
                .setTimeout(1, TimeUnit.MINUTES);


        builder.setItems(avatars.stream()
                .map((v) -> "["+v.getKey().atZone(serverTimeZone).format(DateTimeFormatter.RFC_1123_DATE_TIME)+"]("+v.getValue()+")")
                .toArray(String[]::new));

        builder.build().display(event.getChannel());
    }
}
