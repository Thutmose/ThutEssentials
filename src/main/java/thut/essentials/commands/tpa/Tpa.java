package thut.essentials.commands.tpa;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import thut.essentials.commands.CommandManager;
import thut.essentials.util.BaseCommand;
import thut.essentials.util.PlayerDataHandler;

public class Tpa extends BaseCommand
{
    public Tpa()
    {
        super("tpa", 0);
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
        PlayerEntity player = getPlayerBySender(sender);
        PlayerEntity target = getPlayer(server, sender, args[0]);
        CompoundNBT tag = PlayerDataHandler.getCustomDataTag(target);
        CompoundNBT tpaTag = tag.getCompound("tpa");

        if (tpaTag.getBoolean("ignore")) { return; }

        ITextComponent header = player.getDisplayName().appendSibling(
                CommandManager.makeFormattedComponent(" has Requested to tp to you", TextFormatting.YELLOW, true));

        target.sendMessage(header);
        ITextComponent tpMessage;
        ITextComponent accept = CommandManager.makeFormattedCommandLink("Accept",
                "/tpaccept accept " + player.getCachedUniqueIdString(), TextFormatting.GREEN, true);
        ITextComponent deny = CommandManager.makeFormattedCommandLink("Deny",
                "/tpaccept deny " + player.getCachedUniqueIdString(), TextFormatting.RED, true);
        tpMessage = accept.appendSibling(new StringTextComponent("      /      ")).appendSibling(deny);
        target.sendMessage(tpMessage);
        tpaTag.putString("R", player.getCachedUniqueIdString());
        tag.setTag("tpa", tpaTag);
        PlayerDataHandler.saveCustomData(target);
        player.sendMessage(CommandManager.makeFormattedComponent(
                target.getDisplayName().getFormattedText() + " has been sent a TPA request", TextFormatting.DARK_GREEN,
                true));
    }
}
