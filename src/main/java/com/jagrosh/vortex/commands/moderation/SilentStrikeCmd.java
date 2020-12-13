package com.jagrosh.vortex.commands.moderation;

import com.jagrosh.vortex.Vortex;

/**
 *
 * @author Michaili K (mysteriouscursor+git@protonmail.com)
 */
public class SilentStrikeCmd extends StrikeCmd
{
    public SilentStrikeCmd(Vortex vortex)
    {
        super(vortex);
        this.name = "silentstrike";
        this.arguments = "[number] <@users> <reason>";
        this.help = "applies strikes to users without messaging them";
        this.silent = true;
    }
}
