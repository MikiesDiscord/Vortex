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
package com.jagrosh.vortex.logging;

import com.jagrosh.vortex.Vortex;
import com.typesafe.config.Config;
import net.dv8tion.jda.api.entities.User;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.concurrent.ExecutionException;

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class AvatarSaver
{

    private final static long MaxFileSize = 8_378_000;
    
    private final String userAgent;
    private final Vortex bot;
    
    public AvatarSaver(Config config, Vortex bot)
    {
        userAgent = config.getString("avatar-saver.user-agent");
        this.bot = bot;
    }

    @Nullable
    public String saveAvatar(User user) throws Exception
    {
        if(user.getAvatarId() == null)
            return user.getDefaultAvatarUrl();

        int attempts = 0;
        while(true)
        {
            try
            {
                attempts++;
                for (int imageSize = 1024; imageSize > 128; imageSize /= 2)
                {
                    URLConnection connection = new URL(user.getEffectiveAvatarUrl() + "?size=" + imageSize).openConnection();
                    connection.setRequestProperty("user-agent", userAgent);
                    connection.connect();

                    if (connection.getContentLengthLong() > MaxFileSize && connection.getContentLengthLong() != -1)
                        continue;

                    boolean isAnimated = connection.getContentType().equals("image/gif");


                    InputStream inputStream = connection.getInputStream();
                    ByteArrayOutputStream byteArray = new ByteArrayOutputStream();
                    while (true)
                    {
                        int result = inputStream.read();
                        if (result == -1) break;
                        byteArray.write(result);
                    }
                    return bot.getTextUploader().uploadBytes(byteArray.toByteArray(), user.getAvatarId() + (isAnimated ? ".gif" : ".png"));
                }
            }
            catch(Exception e)
            {
                if(attempts >= 3)
                    throw e;
            }
        }
    }
}
