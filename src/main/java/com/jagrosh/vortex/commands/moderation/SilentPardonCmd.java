package com.jagrosh.vortex.commands.moderation;

import com.jagrosh.vortex.Vortex;

/**
 *
 * @author Michaili K (mysteriouscursor+git@protonmail.com)
 */
public class SilentPardonCmd extends PardonCmd
{
    public SilentPardonCmd(Vortex vortex)
    {
        super(vortex);
        this.name = "silentpardon";
        this.arguments = "[numstrikes] <@users> <reason>";
        this.help = "removes strikes from users without messaging them";
        this.silent = true;
    }
}
