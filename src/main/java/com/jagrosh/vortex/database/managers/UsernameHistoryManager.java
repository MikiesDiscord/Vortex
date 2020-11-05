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
package com.jagrosh.vortex.database.managers;

import com.jagrosh.easysql.DataManager;
import com.jagrosh.easysql.DatabaseConnector;
import com.jagrosh.easysql.SQLColumn;
import com.jagrosh.easysql.columns.InstantColumn;
import com.jagrosh.easysql.columns.LongColumn;
import com.jagrosh.easysql.columns.StringColumn;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.user.update.UserUpdateDiscriminatorEvent;
import net.dv8tion.jda.api.events.user.update.UserUpdateNameEvent;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 *
 * @author Michail K (mysteriouscursor+git@protonmail.com)
 */
public class UsernameHistoryManager extends DataManager
{
    public static final SQLColumn<Long> USER_ID = new LongColumn("USER_ID", false, 0);
    public static final SQLColumn<String> USERNAME = new StringColumn("USERNAME", false, "", 128);
    public static final SQLColumn<String> DISCRIMINATOR = new StringColumn("DISCRIMINATOR", false, "0000", 4);
    public static final SQLColumn<Instant> DATE = new InstantColumn("DATE", false, Instant.EPOCH);

    public UsernameHistoryManager(DatabaseConnector connector)
    {
        super(connector, "USERNAME_HISTORY");
    }

    @Override
    protected String primaryKey()
    {
        return USER_ID+", "+DATE;
    }

    public void addUsername(long userId, Instant change, String username, String discriminator)
    {
        readWrite(selectAll(USER_ID.is(userId)+" AND "+DATE.is(change.getEpochSecond())), rs ->
        {
            if (!rs.next())
            {
                rs.moveToInsertRow();
                USER_ID.updateValue(rs, userId);
                DATE.updateValue(rs, change);
                USERNAME.updateValue(rs, username);
                DISCRIMINATOR.updateValue(rs, discriminator);
                rs.insertRow();
            }
        });
    }
    public void addUsername(UserUpdateNameEvent event)
    {
        addUsername(event.getUser().getIdLong(), Instant.now(), event.getNewName(), event.getUser().getDiscriminator());
    }
    public void addUsername(UserUpdateDiscriminatorEvent event)
    {
        addUsername(event.getUser().getIdLong(), Instant.now(), event.getUser().getName(), event.getNewDiscriminator());
    }

    public List<PastUsername> getPastUsernames(long userId)
    {
        return read(selectAll(USER_ID.is(userId)), rs ->
        {
            List<PastUsername> collection = new ArrayList<>();
            while(rs.next())
                collection.add(new PastUsername(USER_ID.getValue(rs), USERNAME.getValue(rs), DISCRIMINATOR.getValue(rs), DATE.getValue(rs)));
            Collections.reverse(collection);
            return collection;
        });
    }
    public List<PastUsername> getPastUsername(User user)
    {
        return getPastUsernames(user.getIdLong());
    }


    public class PastUsername
    {
        public final long userId;
        public final String username;
        public final String discriminator;
        public final Instant changedAt;

        public PastUsername(long userId, String username, String discriminator, Instant changedAt)
        {
            this.userId = userId;
            this.username = username;
            this.discriminator = discriminator;
            this.changedAt = changedAt;
        }


        public String toString(ZoneId zoneId)
        {
            return "**"+username+"**#"+discriminator+" (" + changedAt.atZone(zoneId).format(DateTimeFormatter.RFC_1123_DATE_TIME) + ")";
        }
    }
}
