package thut.essentials.commands.economy;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import thut.essentials.economy.EconomyManager;
import thut.essentials.util.BaseCommand;

public class Balance extends BaseCommand
{

    public Balance()
    {
        super("bal", 0);
    }

    @Override
    public void execute(MinecraftServer server, ICommandSource sender, String[] args) throws CommandException
    {
        PlayerEntity player = getPlayerBySender(sender);
        int amount = EconomyManager.getBalance(player);
        player.sendMessage(
                new StringTextComponent(TextFormatting.AQUA + "Your Balance is " + TextFormatting.GOLD + amount));
    }

}
