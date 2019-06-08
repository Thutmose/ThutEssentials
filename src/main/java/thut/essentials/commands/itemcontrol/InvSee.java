package thut.essentials.commands.itemcontrol;

import java.util.UUID;

import com.mojang.authlib.GameProfile;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.IInventory;
import net.minecraft.server.MinecraftServer;
import thut.essentials.util.BaseCommand;

public class InvSee extends BaseCommand
{
    public InvSee()
    {
        super("invsee", 2);
    }

    @Override
    public String getUsage(ICommandSource sender)
    {
        return "/" + getName() + " <name|uuid> <optional|ender>";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSource sender, String[] args) throws CommandException
    {
        if (args.length == 0) throw new CommandException(getUsage(sender));

        UUID id = null;
        GameProfile gameprofile = null;
        ServerPlayerEntity user = getCommandSenderAsPlayer(sender);

        try
        {
            id = UUID.fromString(args[0]);
            gameprofile = server.getPlayerProfileCache().getProfileByUUID(id);
        }
        catch (Exception e)
        {
            gameprofile = server.getPlayerProfileCache().getGameProfileForUsername(args[0]);
        }
        if (gameprofile == null) { throw new CommandException("No profile found for " + args[0]); }
        id = gameprofile.getId();

        PlayerEntity player = server.getPlayerList().getPlayerByUUID(id);
        boolean fake = player == null;

        boolean ender = args.length == 2;

        if (fake)
        {
            // Make a fake one.
            player = new PlayerEntity(server.getEntityWorld(), gameprofile)
            {

                @Override
                public boolean isSpectator()
                {
                    return false;
                }

                @Override
                public boolean isCreative()
                {
                    return false;
                }
            };
            server.worlds[0].getSaveHandler().getPlayerNBTManager().readPlayerData(player);
        }
        IInventory inventory = ender ? player.getInventoryEnderChest() : player.inventory;

        user.displayGUIChest(inventory);

    }

}
