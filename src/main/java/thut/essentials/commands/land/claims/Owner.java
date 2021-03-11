package thut.essentials.commands.land.claims;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.server.permission.DefaultPermissionLevel;
import net.minecraftforge.server.permission.PermissionAPI;
import thut.essentials.Essentials;
import thut.essentials.commands.CommandManager;
import thut.essentials.land.LandManager;
import thut.essentials.land.LandManager.LandTeam;

public class Owner
{
    public static void register(final CommandDispatcher<CommandSource> commandDispatcher)
    {
        final String name = "land_owner";
        if (Essentials.config.commandBlacklist.contains(name)) return;
        String perm;
        PermissionAPI.registerNode(perm = "command." + name, DefaultPermissionLevel.ALL, "Can the player use /" + name);

        // Setup with name and permission
        LiteralArgumentBuilder<CommandSource> command = Commands.literal(name).requires(cs -> CommandManager.hasPerm(cs,
                perm));
        // Register the execution.
        command = command.executes(ctx -> Owner.execute(ctx.getSource()));

        // Actually register the command.
        commandDispatcher.register(command);
    }

    private static int execute(final CommandSource source) throws CommandSyntaxException
    {
        final PlayerEntity player = source.getPlayerOrException();
        final int x = MathHelper.floor(player.blockPosition().getX() >> 4);
        final int y = MathHelper.floor(player.blockPosition().getY() >> 4);
        final int z = MathHelper.floor(player.blockPosition().getZ() >> 4);
        if (y < 0 || y > 15) return 1;
        final World dim = player.getCommandSenderWorld();
        final BlockPos b = new BlockPos(x, y, z);
        final LandTeam owner = LandManager.getInstance().getLandOwner(dim, b, true);

        if (!LandManager.isWild(owner)) player.sendMessage(Essentials.config.getMessage(
                "thutessentials.claim.ownedby",
                owner.teamName), Util.NIL_UUID);
        else player.sendMessage(Essentials.config.getMessage("thutessentials.claim.unowned"), Util.NIL_UUID);
        return 0;
    }
}
