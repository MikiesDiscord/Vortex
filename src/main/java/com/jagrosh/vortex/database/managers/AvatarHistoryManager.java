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
import com.jagrosh.vortex.utils.Pair;
import net.dv8tion.jda.api.entities.User;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 *
 * @author Michail K (mysteriouscursor+git@protonmail.com)
 */
public class AvatarHistoryManager extends DataManager
{
    public static final SQLColumn<Long> USER_ID = new LongColumn("USER_ID", false, 0);
    public static final SQLColumn<String> AVATAR_URL = new StringColumn("AVATAR_URL", false, "'https://cdn.discordapp.com/embed/avatars/0.png'", 128);
    public static final SQLColumn<Instant> DATE = new InstantColumn("DATE", false, Instant.EPOCH);

    public AvatarHistoryManager(DatabaseConnector connector)
    {
        super(connector, "AVATAR_HISTORY");
    }

    @Override
    protected String primaryKey()
    {
        return USER_ID+", "+DATE;
    }

    public void addAvatar(long userId, Instant change, String avatarUrl)
    {
        readWrite(selectAll(USER_ID.is(userId)+" AND "+DATE.is(change.getEpochSecond())), rs ->
        {
            if (!rs.next())
            {
                rs.moveToInsertRow();
                USER_ID.updateValue(rs, userId);
                DATE.updateValue(rs, change);
                AVATAR_URL.updateValue(rs, avatarUrl);
                rs.insertRow();
            }
        });
    }

    public List<Pair<Instant, String>> getPastAvatars(long userId)
    {
        return read(selectAll(USER_ID.is(userId)), rs ->
        {
            List<Pair<Instant, String>> collection = new ArrayList<>();
            while(rs.next())
                collection.add(new Pair<>(DATE.getValue(rs), AVATAR_URL.getValue(rs)));
            Collections.reverse(collection);
            return collection;
        });
    }
    public List<Pair<Instant, String>> getPastAvatars(User user)
    {
        return getPastAvatars(user.getIdLong());
    }
}
