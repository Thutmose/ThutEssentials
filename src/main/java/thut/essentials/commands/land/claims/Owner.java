package thut.essentials.commands.land.claims;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.server.permission.DefaultPermissionLevel;
import net.minecraftforge.server.permission.PermissionAPI;
import thut.essentials.Essentials;
import thut.essentials.commands.CommandManager;
import thut.essentials.land.LandManager;
import thut.essentials.land.LandManager.KGobalPos;
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
        final PlayerEntity player = source.asPlayer();
        final int x = MathHelper.floor(player.getPosition().getX() >> 4);
        final int y = MathHelper.floor(player.getPosition().getY() >> 4);
        final int z = MathHelper.floor(player.getPosition().getZ() >> 4);
        if (y < 0 || y > 15) return 1;
        final RegistryKey<World> dim = player.getEntityWorld().getDimensionKey();
        final BlockPos b = new BlockPos(x, y, z);
        final KGobalPos chunk = KGobalPos.getPosition(dim, b);
        final LandTeam owner = LandManager.getInstance().getLandOwner(chunk);

        if (!LandManager.isWild(owner)) player.sendMessage(Essentials.config.getMessage(
                "thutessentials.claim.ownedby",
                owner.teamName), Util.DUMMY_UUID);
        else player.sendMessage(Essentials.config.getMessage("thutessentials.claim.unowned"), Util.DUMMY_UUID);
        return 0;
    }
}
