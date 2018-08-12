package thut.essentials.commands.economy;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import thut.essentials.economy.EconomyManager;
import thut.essentials.util.BaseCommand;
import thut.essentials.util.CompatWrapper;

public class Pay extends BaseCommand
{

    public Pay()
    {
        super("pay", 0);
    }

    /** Return whether the specified command parameter index is a username
     * parameter. */
    @Override
    public boolean isUsernameIndex(String[] args, int index)
    {
        return index == 0;
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException
    {
        int senderBalance = Integer.MAX_VALUE;
        EntityPlayer payer = null;
        if (sender instanceof EntityPlayer)
        {
            payer = getPlayerBySender(sender);
            senderBalance = EconomyManager.getBalance(payer);
        }
        int toSend = Integer.parseInt(args[1]);
        if (toSend <= 0) throw new CommandException("You must pay more than 0");
        EntityPlayer payee = getPlayer(server, sender, args[0]);
        if (toSend <= senderBalance)
        {
            EconomyManager.addBalance(payee, toSend);
            CompatWrapper.sendChatMessage(payee,
                    new TextComponentString(TextFormatting.AQUA + "You recieved " + TextFormatting.GOLD + toSend
                            + TextFormatting.AQUA + " from ").appendSibling(sender.getDisplayName()));
            if (payer != null)
            {
                EconomyManager.addBalance(payer, -toSend);
                CompatWrapper.sendChatMessage(payer, new TextComponentString(
                        TextFormatting.AQUA + "You sent " + TextFormatting.GOLD + toSend + TextFormatting.AQUA + " to ")
                                .appendSibling(payee.getDisplayName()));
            }
        }
        else
        {
            throw new CommandException("Insufficient Funds");
        }
    }

}
