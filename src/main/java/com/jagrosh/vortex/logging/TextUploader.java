/*
 * Copyright 2017 John Grosh (john.a.grosh@gmail.com).
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

import club.minnced.discord.webhook.WebhookClient;
import club.minnced.discord.webhook.WebhookClientBuilder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.BiConsumer;

import club.minnced.discord.webhook.receive.ReadonlyMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class TextUploader
{
    /**
     * Alright, I'm going to write a bit here because I have a feeling that some
     * people will wonder about this. The purpose of this class is to utilize
     * Discord's persistent and encrypted message channels as storage for
     * message logs. 
     */
    private final Logger LOG = LoggerFactory.getLogger("Upload");
    private final List<WebhookClient> webhooks = new ArrayList<>();
    private int index = 0;
    
    public TextUploader(List<String> urls)
    {
        urls.forEach(url -> webhooks.add(new WebhookClientBuilder(url).build()));
    }
    
    public synchronized void upload(String content, String filename, BiConsumer<String,String> done)
    {
        webhooks.get(index % webhooks.size()).send(content.getBytes(StandardCharsets.UTF_8), filename+".txt").whenCompleteAsync((msg, err) -> 
        {
            if(msg != null)
            {
                String url = msg.getAttachments().get(0).getUrl();
                done.accept("https://txt.discord.website?txt=" + url.substring(url.indexOf("s/")+2, url.length()-4), url);
            }
            else if(err != null)
            {
                LOG.error("Failed to upload: ", err);
            }
        });
        index++;
    }

    public String uploadBytes(byte[] content, String filename) throws ExecutionException, InterruptedException
    {

        CompletableFuture<ReadonlyMessage> future = webhooks.get(index % webhooks.size()).send(content, filename);
        index++;
        return future.get().getAttachments().get(0).getUrl();
    }
    
    
    public static interface Result
    {
        public abstract void consume(String view, String download);
    }
}
