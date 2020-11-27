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

import com.typesafe.config.Config;
import net.dv8tion.jda.api.entities.Message;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;

/**
 *
 * @author Michail K (mysteriouscursor+git@protonmail.com)
 */
public class AttachmentCache
{
    private final static int MaxFileSize = 8_378_000;
    private final String userAgent;
    public final long maxCacheSize;
    private final long maxCacheAge;

    private final HashMap<Long, CachedAttachments> filesCache = new HashMap<>();
    private long currentCacheSize = 0;

    public AttachmentCache(Config config) throws IOException
    {
        userAgent = config.getString("attachment-cache.user-agent");
        maxCacheSize = config.getLong("attachment-cache.max-size");
        maxCacheAge = config.getLong("attachment-cache.max-age");

        File cachesDir = new File("attachmentcache");
        if(!cachesDir.exists())
            if(!cachesDir.mkdir())
                throw new IOException("Could not create attachmentcache directory");

        for(File message : cachesDir.listFiles())
        {
            CachedAttachments attachments = new CachedAttachments(message);
            filesCache.put(Long.parseLong(message.getName()), attachments);
            currentCacheSize += attachments.size;
        }
    }

    @Nullable
    public CachedAttachments getCachedAttachment(long id)
    {
        return filesCache.get(id);
    }

    public long getCurrentCacheSize()
    {
        return currentCacheSize;
    }

    public void downloadAttachmentsOfMessage(Message message) throws IOException
    {
        if(message.getAttachments().isEmpty())
            return;

        File directory = new File("attachmentcache"+File.separator+message.getId());
        directory.mkdir();

        for(Message.Attachment attachment : message.getAttachments())
        {
            URLConnection connection = new URL(attachment.getUrl()).openConnection();
            connection.setRequestProperty("user-agent", userAgent);
            connection.connect();

            if(connection.getContentLengthLong() > MaxFileSize)
                continue;

            File file = new File("attachmentcache"+File.separator+message.getId()+File.separator+attachment.getFileName());
            if(!file.createNewFile())
                throw new IOException("Could not create file for attachment for "+message.getId());

            InputStream inputStream = connection.getInputStream();
            ByteArrayOutputStream buffer = new ByteArrayOutputStream(connection.getContentLength() > 0 ? connection.getContentLength() : MaxFileSize);
            FileOutputStream stream = new FileOutputStream(file);

            while(true)
            {
                int result = inputStream.read();
                if (result == -1) break;
                buffer.write(result);
            }

            stream.write(buffer.toByteArray());
            stream.close();
        }

        synchronized (filesCache)
        {
            CachedAttachments attachments = new CachedAttachments(directory);
            filesCache.put(message.getIdLong(), attachments);
            currentCacheSize += attachments.size;
        }

        checkAttachments();
    }


    public void deleteAttachment(CachedAttachments attachments) throws IOException
    {
        for(File attachment : attachments.attachments)
            if(!attachment.delete())
                throw new IOException("Could not delete file from attachment cache ("+attachments.directory.getName()+")");

        if(!attachments.directory.delete())
            throw new IOException("Could not delete directory from attachment cache ("+attachments.directory.getName()+")");
        synchronized (filesCache)
        {
            filesCache.values().remove(attachments);
            currentCacheSize -= attachments.size;
        }
    }
    public void deleteAttachment(long id) throws IOException
    {
        deleteAttachment(filesCache.get(id));
    }

    public boolean tryDeleteAttachment(long id)
    {
        try
        {
            deleteAttachment(filesCache.get(id));
            return true;
        }
        catch(Exception e)
        {
            return false;
        }
    }

    public synchronized void checkAttachments() throws IOException
    {
        if(maxCacheAge > 0 || (maxCacheSize > 0 && maxCacheSize <= currentCacheSize))
        {
            ArrayList<CachedAttachments> ordered = new ArrayList<>(filesCache.values());
            ordered.sort((msg1, msg2) -> (int) (msg1.creationDate.getEpochSecond() - msg2.creationDate.getEpochSecond()));
            Instant cacheExpireDate = Instant.now().minusSeconds(maxCacheAge);

            while (!ordered.isEmpty() && (
                    (maxCacheSize > 0 && maxCacheSize < currentCacheSize) ||
                    (maxCacheAge > 0 && ordered.get(0).creationDate.isBefore(cacheExpireDate))
                    ))
            {
                deleteAttachment(ordered.get(0));
                ordered.remove(0);
            }
        }
    }

    static class CachedAttachments
    {
        public final File directory;
        public final Collection<File> attachments;
        public final long size;
        public final Instant creationDate;

        public CachedAttachments(File directory) throws IOException
        {
            this.directory = directory;
            this.attachments = Arrays.asList(directory.listFiles());
            this.creationDate = Files.readAttributes(directory.toPath(), BasicFileAttributes.class).creationTime().toInstant();

            long size = 0;
            for(File attachment : this.attachments)
                size += attachment.length();
            this.size = size;
        }

    }
}


