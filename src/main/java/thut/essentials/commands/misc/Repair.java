package thut.essentials.commands.misc;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSource;
import net.minecraft.command.PlayerNotFoundException;
import net.minecraft.entity.player.PlayerEntity;
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
    public void execute(MinecraftServer server, ICommandSource sender, String[] args) throws CommandException
    {
        PlayerEntity player;
        try
        {
            player = getPlayerBySender(sender);
        }
        catch (PlayerNotFoundException e)
        {
            if (args.length != 1) throw new CommandException("Invalid Arguments, /god <target>");
            player = getPlayer(server, sender, args[0]);
        }
        ItemStack stack = player.getHeldItemMainhand();
        if (stack != null && stack.isDamaged())
        {
            stack.setItemDamage(0);
        }
    }
}
