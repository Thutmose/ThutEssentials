package thut.essentials.commands.misc;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.PlayerNotFoundException;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import thut.essentials.util.BaseCommand;

public class Fly extends BaseCommand
{
    public Fly()
    {
        super("fly", 4);
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException
    {
        EntityPlayer player;
        try
        {
            player = getPlayerBySender(sender);
        }
        catch (PlayerNotFoundException e)
        {
            if (args.length != 1) throw new CommandException("Invalid Arguments, /god <target>");
            player = getPlayer(server, sender, args[0]);
        }
        player.capabilities.allowFlying = !player.capabilities.allowFlying;
        if (!player.capabilities.allowFlying)
        {
            player.capabilities.isFlying = false;
        }
        player.sendPlayerAbilities();
        player.sendMessage(new TextComponentString(
                TextFormatting.GREEN + "Fly set to: " + TextFormatting.GOLD + player.capabilities.allowFlying));
    }
}
