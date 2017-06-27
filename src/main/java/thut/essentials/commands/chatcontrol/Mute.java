package thut.essentials.commands.chatcontrol;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import thut.essentials.ThutEssentials;
import thut.essentials.util.BaseCommand;
import thut.essentials.util.PlayerDataHandler;

public class Mute extends BaseCommand
{

    public Mute()
    {
        super("mute", 2);
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void mute(ServerChatEvent event)
    {
        EntityPlayer talker = event.getPlayer();
        NBTTagCompound tag = PlayerDataHandler.getCustomDataTag(talker);
        if (tag.getLong("muted") > talker.getServer().getEntityWorld().getTotalWorldTime())
        {
            talker.sendMessage(new TextComponentString("You are muted"));
            System.out.println(event.getUsername() + ": " + event.getMessage());
            event.setCanceled(true);
        }
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException
    {
        EntityPlayer player = getPlayer(server, sender, args[0]);
        if (ThutEssentials.perms.hasPermission(player, "unmutable"))
            throw new CommandException(args[0] + " cannot be muted.");
        int time = Integer.parseInt(args[1]);
        String reason = args[2];
        NBTTagCompound tag = PlayerDataHandler.getCustomDataTag(player);
        tag.setLong("muted", server.getEntityWorld().getTotalWorldTime() + (time * 20 * 60));
        System.out.println("Muted " + player.getDisplayNameString() + " for " + reason + " for " + time + " minutes");
        player.sendMessage(
                new TextComponentString("You have been muted for " + reason + " for " + time + " minutes"));
        PlayerDataHandler.saveCustomData(player);
    }

}
