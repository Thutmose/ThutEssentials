package thut.essentials.commands.homes;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.dimension.DimensionType;
import net.minecraftforge.server.permission.DefaultPermissionLevel;
import net.minecraftforge.server.permission.PermissionAPI;
import thut.essentials.Essentials;
import thut.essentials.commands.CommandManager;
import thut.essentials.util.HomeManager;
import thut.essentials.util.PlayerDataHandler;
import thut.essentials.util.PlayerMover;

public class Homes
{
    public static void register(final CommandDispatcher<CommandSource> commandDispatcher)
    {
        String name = "homes";
        if (!Essentials.config.commandBlacklist.contains(name))
        {
            String perm;
            PermissionAPI.registerNode(perm = "command." + name, DefaultPermissionLevel.ALL, "Can the player use /"
                    + name);
            // Setup with name and permission
            LiteralArgumentBuilder<CommandSource> command = Commands.literal(name).requires(cs -> CommandManager
                    .hasPerm(cs, perm));
            // Register the execution.
            command = command.executes(ctx -> Homes.execute(ctx.getSource()));

            // Actually register the command.
            commandDispatcher.register(command);
        }
        name = "home";
        if (!Essentials.config.commandBlacklist.contains(name))
        {
            String perm;
            PermissionAPI.registerNode(perm = "command." + name, DefaultPermissionLevel.ALL, "Can the player use /"
                    + name);
            // Setup with name and permission
            LiteralArgumentBuilder<CommandSource> command = Commands.literal(name).requires(cs -> CommandManager
                    .hasPerm(cs, perm));

            // Home name argument version.
            command = command.then(Commands.argument("home_name", StringArgumentType.string()).executes(ctx -> Homes
                    .execute(ctx.getSource(), StringArgumentType.getString(ctx, "home_name"))));
            commandDispatcher.register(command);

            // No argument version.
            command = Commands.literal(name).requires(cs -> CommandManager.hasPerm(cs, perm));
            command = command.executes(ctx -> Homes.execute(ctx.getSource(), null));
            commandDispatcher.register(command);
        }
    }

    private static int execute(final CommandSource source) throws CommandSyntaxException
    {
        final ServerPlayerEntity player = source.asPlayer();
        HomeManager.sendHomeList(player);
        return 0;
    }

    private static int execute(final CommandSource source, String homeName) throws CommandSyntaxException
    {
        if (homeName == null) homeName = "Home";

        final ServerPlayerEntity player = source.asPlayer();
        final int[] home = HomeManager.getHome(player, homeName);
        if (home == null)
        {
            final ITextComponent message = CommandManager.makeFormattedComponent("thutessentials.homes.noexists", null,
                    false, homeName);
            player.sendMessage(message);
            return 1;
        }
        final CompoundNBT tag = PlayerDataHandler.getCustomDataTag(player);
        final CompoundNBT tptag = tag.getCompound("tp");
        final long last = tptag.getLong("homeDelay");
        final long time = player.getServer().getWorld(DimensionType.OVERWORLD).getGameTime();
        if (last > time && Essentials.config.homeReUseDelay > 0)
        {
            final ITextComponent message = CommandManager.makeFormattedComponent("thutessentials.tp.tosoon");
            player.sendMessage(message);
            return 2;
        }

        ITextComponent message = CommandManager.makeFormattedComponent("thutessentials.homes.warping", null, false,
                homeName);
        player.sendMessage(message);
        message = CommandManager.makeFormattedComponent("thutessentials.homes.warped", null, false, homeName);
        tptag.putLong("homeDelay", time + Essentials.config.homeReUseDelay);
        tag.put("tp", tptag);
        PlayerDataHandler.saveCustomData(player);
        PlayerMover.setMove(player, Essentials.config.homeActivateDelay, home[3], new BlockPos(home[0], home[1],
                home[2]), message, PlayerMover.INTERUPTED);

        return 0;
    }
}
