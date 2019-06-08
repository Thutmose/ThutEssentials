package thut.essentials.commands.chatcontrol;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.StringTextComponent;
import thut.essentials.util.BaseCommand;
import thut.essentials.util.PlayerDataHandler;

public class UnMute extends BaseCommand
{

    public UnMute()
    {
        super("unmute", 2);
    }

    /** Return whether the specified command parameter index is a username
     * parameter. */
    @Override
    public boolean isUsernameIndex(String[] args, int index)
    {
        return index == 0;
    }

    @Override
    public void execute(MinecraftServer server, ICommandSource sender, String[] args) throws CommandException
    {
        PlayerEntity player = getPlayer(server, sender, args[0]);
        CompoundNBT tag = PlayerDataHandler.getCustomDataTag(player);

        if (tag.getLong("muted") > server.getEntityWorld().getGameTime())
        {
            tag.remove("muted");
            PlayerDataHandler.saveCustomData(player);
            player.sendMessage(new StringTextComponent("You have been unmuted"));
            sender.sendMessage(new StringTextComponent(args[0] + " has been unmuted"));
        }
        else
        {
            sender.sendMessage(new StringTextComponent(args[0] + " was not muted."));
        }
    }

}
