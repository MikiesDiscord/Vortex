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
package com.jagrosh.vortex.commands.tools;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.commons.utils.FinderUtil;
import com.jagrosh.jdautilities.menu.Paginator;
import com.jagrosh.vortex.Action;
import com.jagrosh.vortex.Vortex;
import com.jagrosh.vortex.commands.ModCommand;
import com.jagrosh.vortex.database.managers.PremiumManager;
import com.jagrosh.vortex.database.managers.UsernameHistoryManager;
import com.jagrosh.vortex.utils.FormatUtil;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild.Ban;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.exceptions.PermissionException;

import java.time.ZoneId;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 *
 * @author Michaili K (mysteriouscursor+git@protonmail.com)
 */
public class UsernameHistoryCmd extends Command
{
    private final Vortex vortex;
    public UsernameHistoryCmd(Vortex vortex)
    {
        this.vortex = vortex;
        this.name = "usernamehistory";
        this.arguments = "<user>";
        this.help = "sees the name history from a user";
        this.aliases = new String[] {"usernames", "namehistory", "names"};
        this.botPermissions = new Permission[]{Permission.MESSAGE_EMBED_LINKS};
    }
    
    @Override
    protected void execute(CommandEvent event)
    {
        if(event.getArgs().isEmpty() || event.getArgs().equalsIgnoreCase("help"))
        {
            event.replySuccess("This command is used to see a user's past names. Please include a user or user ID to check.");
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
        Collection<UsernameHistoryManager.PastUsername> usernames = vortex.getDatabase().usernameHistory.getPastUsername(user);
        if(usernames.isEmpty())
        {
            event.replyWarning("This user does not have any past usernames recorded.");
            return;
        }

        ZoneId serverTimeZone = vortex.getDatabase().settings.getSettings(event.getGuild()).getTimezone();
        Paginator.Builder builder = new Paginator.Builder()
                .setText("Username History for **"+user.getName()+"**#"+user.getDiscriminator()+" (ID:"+user.getId()+")")
                .setColumns(1)
                .setFinalAction(m -> {try{m.clearReactions().queue();}catch(PermissionException ignore){}})
                .setItemsPerPage(10)
                .waitOnSinglePage(false)
                .showPageNumbers(true)
                .wrapPageEnds(true)
                .setEventWaiter(vortex.getEventWaiter())
                .setTimeout(1, TimeUnit.MINUTES);


        builder.setItems(usernames.stream().map(x -> x.toString(serverTimeZone)).toArray(String[]::new));

        builder.build().display(event.getChannel());
    }
}
