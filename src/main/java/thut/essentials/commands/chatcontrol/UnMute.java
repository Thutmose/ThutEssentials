package thut.essentials.commands.chatcontrol;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;
import thut.essentials.util.BaseCommand;
import thut.essentials.util.PlayerDataHandler;

public class UnMute extends BaseCommand
{

    public UnMute()
    {
        super("unmute", 2);
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException
    {
        EntityPlayer player = getPlayer(server, sender, args[0]);
        NBTTagCompound tag = PlayerDataHandler.getCustomDataTag(player);

        if (tag.getLong("muted") > server.getEntityWorld().getTotalWorldTime())
        {
            tag.removeTag("muted");
            PlayerDataHandler.saveCustomData(player);
            player.sendMessage(new TextComponentString("You have been unmuted"));
            sender.sendMessage(new TextComponentString(args[0] + " has been unmuted"));
        }
        else
        {
            sender.sendMessage(new TextComponentString(args[0] + " was not muted."));
        }
    }

}
