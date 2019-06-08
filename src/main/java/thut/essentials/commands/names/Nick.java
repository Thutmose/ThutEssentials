package thut.essentials.commands.names;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSource;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.StringTextComponent;
import thut.essentials.util.BaseCommand;
import thut.essentials.util.PlayerDataHandler;
import thut.essentials.util.RuleManager;

public class Nick extends BaseCommand
{
    public Nick()
    {
        super("nick", 2);
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
        ServerPlayerEntity player;
        int start = 1;
        try
        {
            player = getPlayer(server, sender, args[0]);
        }
        catch (Exception e)
        {
            player = getPlayerBySender(sender);
            start = 0;
        }
        String arg = args.length == start ? "" : args[start];
        CompoundNBT tag = PlayerDataHandler.getCustomDataTag(player);
        CompoundNBT nametag = tag.getCompound("name");
        if (!nametag.hasKey("original")) nametag.putString("original", player.getDisplayNameString());
        for (int i = start + 1; i < args.length; i++)
        {
            arg = arg + " " + args[i];
        }
        arg = RuleManager.format(arg);
        if (arg.isEmpty())
        {
            nametag.remove("name");
            sender.sendMessage(new StringTextComponent("Reset name of " + player.getDisplayNameString()));
        }
        else
        {
            nametag.putString("name", arg);
            sender.sendMessage(new StringTextComponent("Set name of " + player.getDisplayNameString() + " to " + arg));
        }
        tag.setTag("name", nametag);
        PlayerDataHandler.saveCustomData(player);

        player.refreshDisplayName();
    }

}
