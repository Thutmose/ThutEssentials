package thut.essentials.commands.misc;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSource;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.StringTextComponent;
import thut.essentials.util.BaseCommand;

public class CPU extends BaseCommand
{

    public CPU()
    {
        super("cpu", 4);
    }

    @Override
    public void execute(MinecraftServer server, ICommandSource sender, String[] args) throws CommandException
    {
        int val = 0;
        double meanTickTime = mean(server.tickTimeArray) * 1.0E-6D;
        val = (int) (100 * meanTickTime / 50);
        val = Math.min(val, 100);
        int procs = Runtime.getRuntime().availableProcessors();

        String mess = "Processor Count: " + procs;
        sender.sendMessage(new StringTextComponent(mess));

        mess = "World Threads Load: " + val + "%";
        sender.sendMessage(new StringTextComponent(mess));
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
