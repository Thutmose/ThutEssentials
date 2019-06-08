package thut.essentials.commands.tpa;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextFormatting;
import thut.essentials.commands.CommandManager;
import thut.essentials.util.BaseCommand;
import thut.essentials.util.PlayerDataHandler;

public class TpToggle extends BaseCommand
{
    public TpToggle()
    {
        super("tptoggle", 0);
    }

    @Override
    public void execute(MinecraftServer server, ICommandSource sender, String[] args) throws CommandException
    {
        PlayerEntity player = getPlayerBySender(sender);
        CompoundNBT tag = PlayerDataHandler.getCustomDataTag(player);
        CompoundNBT tpaTag = tag.getCompound("tpa");
        boolean ignore = !tpaTag.getBoolean("ignore");
        tpaTag.putBoolean("ignore", ignore);
        tag.setTag("tpa", tpaTag);
        PlayerDataHandler.saveCustomData(player);
        player.sendMessage(CommandManager.makeFormattedComponent("Set ignoring TPA to " + ignore,
                TextFormatting.DARK_GREEN, true));
    }

}
