package thut.essentials.util;

import java.util.List;

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

import net.minecraft.server.MinecraftServer;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.TickEvent.ServerTickEvent;
import thut.essentials.Essentials;

public class CmdScheduler
{
    public static class Cmd
    {
        public int timer;

        public long _last_run = 0;

        public String cmd;
    }

    private static long tick = 0;

    private static List<Cmd> cmds = Lists.newArrayList();

    public static void loadConfigs()
    {
        CmdScheduler.cmds.clear();
        final Gson gson = new GsonBuilder().create();
        for (final String s : Essentials.config.scheduledCommands)
            try
            {
                final Cmd cmd = gson.fromJson(s, Cmd.class);
                if (cmd.cmd != null && cmd.timer > 0)
                {
                    if (CmdScheduler.cmds.contains(cmd)) Essentials.LOGGER.warn(
                            "Warning, adding duplicate copy of command: {}", cmd);
                    CmdScheduler.cmds.add(cmd);
                }
            }
            catch (final JsonSyntaxException e)
            {
                Essentials.LOGGER.error(e);
            }
    }

    public static void onTick(final ServerTickEvent event)
    {
        if (event.phase != Phase.END) return;
        CmdScheduler.tick++;
        final MinecraftServer server = Essentials.server;
        for (final Cmd cmd : CmdScheduler.cmds)
            if (cmd._last_run + cmd.timer < CmdScheduler.tick)
            {
                cmd._last_run = CmdScheduler.tick;
                server.getCommands().performCommand(server.createCommandSourceStack(), cmd.cmd);
            }
    }
}
