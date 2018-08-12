package thut.essentials.commands.chatcontrol;

import java.util.logging.Level;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.server.permission.DefaultPermissionLevel;
import net.minecraftforge.server.permission.PermissionAPI;
import thut.essentials.ThutEssentials;
import thut.essentials.util.BaseCommand;
import thut.essentials.util.PlayerDataHandler;

public class Mute extends BaseCommand
{
    public static final String UNMUTABLE = "thutessentials.chat.unmutable";

    public Mute()
    {
        super("mute", 2);
        MinecraftForge.EVENT_BUS.register(this);
        PermissionAPI.registerNode(UNMUTABLE, DefaultPermissionLevel.OP, "Cannot be target of /mute");
    }

    /** Return whether the specified command parameter index is a username
     * parameter. */
    @Override
    public boolean isUsernameIndex(String[] args, int index)
    {
        return index == 0;
    }

    @SubscribeEvent
    public void mute(ServerChatEvent event)
    {
        EntityPlayer talker = event.getPlayer();
        NBTTagCompound tag = PlayerDataHandler.getCustomDataTag(talker);
        if (tag.getLong("muted") > talker.getServer().getEntityWorld().getTotalWorldTime())
        {
            talker.sendMessage(new TextComponentString("You are muted"));
            ThutEssentials.logger.log(Level.INFO, event.getUsername() + ": " + event.getMessage());
            event.setCanceled(true);
        }
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException
    {
        EntityPlayer player = getPlayer(server, sender, args[0]);
        if (PermissionAPI.hasPermission(player, UNMUTABLE)) throw new CommandException(args[0] + " cannot be muted.");
        int time = Integer.parseInt(args[1]);
        String reason = args[2];
        NBTTagCompound tag = PlayerDataHandler.getCustomDataTag(player);
        tag.setLong("muted", server.getEntityWorld().getTotalWorldTime() + (time * 20 * 60));
        ThutEssentials.logger.log(Level.INFO,
                "Muted " + player.getDisplayNameString() + " for " + reason + " for " + time + " minutes");
        player.sendMessage(new TextComponentString("You have been muted for " + reason + " for " + time + " minutes"));
        PlayerDataHandler.saveCustomData(player);
    }

}
