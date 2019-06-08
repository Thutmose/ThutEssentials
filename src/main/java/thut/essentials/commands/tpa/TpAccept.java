package thut.essentials.commands.tpa;

import java.util.UUID;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextFormatting;
import thut.essentials.ThutEssentials;
import thut.essentials.commands.CommandManager;
import thut.essentials.commands.misc.Spawn;
import thut.essentials.commands.misc.Spawn.PlayerMover;
import thut.essentials.util.BaseCommand;
import thut.essentials.util.PlayerDataHandler;

public class TpAccept extends BaseCommand
{
    public TpAccept()
    {
        super("tpaccept", 0);
    }

    @Override
    public void execute(MinecraftServer server, ICommandSource sender, String[] args) throws CommandException
    {
        if (args.length == 0)
            throw new CommandException("CLICK THE LINK TO ACCEPT, DO NOT SEND THIS COMMAND DIRECTLY!");
        String id = args[1];
        PlayerEntity player = getPlayerBySender(sender);
        CompoundNBT tag = PlayerDataHandler.getCustomDataTag(player);
        CompoundNBT tpaTag = tag.getCompound("tpa");
        String requestor = tpaTag.getString("R");
        if (!requestor.equals(id)) { return; }
        tpaTag.remove("R");
        tag.setTag("tpa", tpaTag);
        PlayerDataHandler.saveCustomData(player);
        PlayerEntity target = server.getPlayerList().getPlayerByUUID(UUID.fromString(id));
        if (args[0].equals("accept"))
        {
            target.sendMessage(CommandManager.makeFormattedComponent("Your TPA request was accepted.",
                    TextFormatting.GREEN, true));
            player.sendMessage(
                    CommandManager.makeFormattedComponent("Accepted the TPA request.", TextFormatting.GREEN, true));
            PlayerMover.setMove(target, ThutEssentials.instance.config.tpaActivateDelay, player.dimension,
                    player.getPosition(), null, Spawn.INTERUPTED);
        }
        else if (args[0].equals("deny"))
        {
            target.sendMessage(
                    CommandManager.makeFormattedComponent("Your TPA request was denied.", TextFormatting.RED, true));
            player.sendMessage(
                    CommandManager.makeFormattedComponent("Denied the TPA request.", TextFormatting.RED, true));
        }
    }
}
