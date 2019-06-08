package thut.essentials.commands.chatcontrol;

import java.util.logging.Level;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
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
        PlayerEntity talker = event.getPlayer();
        CompoundNBT tag = PlayerDataHandler.getCustomDataTag(talker);
        if (tag.getLong("muted") > talker.getServer().getEntityWorld().getGameTime())
        {
            talker.sendMessage(new StringTextComponent("You are muted"));
            ThutEssentials.logger.log(Level.INFO, event.getUsername() + ": " + event.getMessage());
            event.setCanceled(true);
        }
    }

    @Override
    public void execute(MinecraftServer server, ICommandSource sender, String[] args) throws CommandException
    {
        PlayerEntity player = getPlayer(server, sender, args[0]);
        if (PermissionAPI.hasPermission(player, UNMUTABLE)) throw new CommandException(args[0] + " cannot be muted.");
        int time = Integer.parseInt(args[1]);
        String reason = args[2];
        CompoundNBT tag = PlayerDataHandler.getCustomDataTag(player);
        tag.putLong("muted", server.getEntityWorld().getGameTime() + (time * 20 * 60));
        ThutEssentials.logger.log(Level.INFO,
                "Muted " + player.getDisplayNameString() + " for " + reason + " for " + time + " minutes");
        player.sendMessage(new StringTextComponent("You have been muted for " + reason + " for " + time + " minutes"));
        PlayerDataHandler.saveCustomData(player);
    }

}
