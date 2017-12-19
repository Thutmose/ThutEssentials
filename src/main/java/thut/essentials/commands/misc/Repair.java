package thut.essentials.commands.misc;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.PlayerNotFoundException;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import thut.essentials.util.BaseCommand;

public class Repair extends BaseCommand
{
    public Repair()
    {
        super("repair", 4);
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException
    {
        EntityPlayer player;
        try
        {
            player = getCommandSenderAsPlayer(sender);
        }
        catch (PlayerNotFoundException e)
        {
            if (args.length != 1) throw new CommandException("Invalid Arguments, /god <target>");
            player = getPlayer(server, sender, args[0]);
        }
        ItemStack stack = player.getHeldItemMainhand();
        if (stack != null && stack.isItemDamaged())
        {
            stack.setItemDamage(0);
        }
    }
}
