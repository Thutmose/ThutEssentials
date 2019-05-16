package thut.essentials.commands.kits;

import java.util.List;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.server.permission.IPermissionHandler;
import net.minecraftforge.server.permission.PermissionAPI;
import net.minecraftforge.server.permission.context.PlayerContext;
import thut.essentials.economy.EconomyManager;
import thut.essentials.util.BaseCommand;
import thut.essentials.util.ConfigManager;
import thut.essentials.util.KitManager;
import thut.essentials.util.KitManager.KitSet;
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

        List<ItemStack> stacks;
        int delay = ConfigManager.INSTANCE.kitReuseDelay;
        String kitTag = "kitTime";
        String perm = "thutessentials.kit.default";
        // Specific kit.
        if (args.length == 1)
        {
            KitSet kit = KitManager.kits.get(args[0]);
            if (kit == null) throw new CommandException("No kit by that name found.");
            perm = "thutessentials.kit." + args[0];
            kitTag = "kitTime_" + args[0];
            delay = kit.cooldown;
            stacks = kit.stacks;
        }
        else
        {
            stacks = KitManager.kit;
        }

        IPermissionHandler manager = PermissionAPI.getPermissionHandler();
        PlayerContext context = new PlayerContext(player);
        if (!manager.hasPermission(player.getGameProfile(), perm, context))
            throw new CommandException("You are not allowed to get that kit!");

        long kitTime = PlayerDataHandler.getCustomDataTag(player).getLong(kitTag);
        if ((delay <= 0 && kitTime != 0) || server.getEntityWorld().getTotalWorldTime() < kitTime)
            throw new CommandException("You cannot get another kit yet.");
        for (ItemStack stack : stacks)
        {
            EconomyManager.giveItem(player, stack.copy());
            PlayerDataHandler.getCustomDataTag(player).setLong(kitTag,
                    server.getEntityWorld().getTotalWorldTime() + delay);
        }

    }

}
