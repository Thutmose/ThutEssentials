package thut.essentials.commands.misc;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;
import thut.essentials.util.BaseCommand;

public class CPU extends BaseCommand
{

    public CPU()
    {
        super("cpu", 4);
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException
    {
        int val = 0;
        double meanTickTime = mean(server.tickTimeArray) * 1.0E-6D;
        val = (int) (100 * meanTickTime / 50);
        val = Math.min(val, 100);
        String mess = "World Threads Load: " + val + "%";
        sender.sendMessage(new TextComponentString(mess));
    }

    private static long mean(long[] values)
    {
        long sum = 0l;
        for (long v : values)
        {
            sum += v;
        }

        return sum / values.length;
    }
}
