package thut.essentials.commands.kits;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import thut.essentials.economy.EconomyManager;
import thut.essentials.util.BaseCommand;
import thut.essentials.util.ConfigManager;
import thut.essentials.util.KitManager;
import thut.essentials.util.PlayerDataHandler;

public class Kit extends BaseCommand
{

    public Kit()
    {
        super("kit", 0);
        KitManager.init();
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException
    {
        EntityPlayer player = getPlayerBySender(sender);
        long kitTime = PlayerDataHandler.getCustomDataTag(player).getLong("kitTime");
        if ((ConfigManager.INSTANCE.kitReuseDelay <= 0 && kitTime != 0)
                || server.getEntityWorld().getTotalWorldTime() < kitTime)
            throw new CommandException("You cannot get another kit yet.");
        for (ItemStack stack : KitManager.kit)
        {
            EconomyManager.giveItem(player, stack.copy());
            PlayerDataHandler.getCustomDataTag(player).setLong("kitTime",
                    server.getEntityWorld().getTotalWorldTime() + ConfigManager.INSTANCE.kitReuseDelay);
        }
    }

}
