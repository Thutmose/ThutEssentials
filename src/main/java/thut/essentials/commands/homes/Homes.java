package thut.essentials.commands.homes;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.Util;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraftforge.server.permission.DefaultPermissionLevel;
import net.minecraftforge.server.permission.PermissionAPI;
import thut.essentials.Essentials;
import thut.essentials.commands.CommandManager;
import thut.essentials.land.LandManager.KGobalPos;
import thut.essentials.util.HomeManager;
import thut.essentials.util.PlayerDataHandler;
import thut.essentials.util.PlayerMover;

public class Homes
{
    public static void register(final CommandDispatcher<CommandSourceStack> commandDispatcher)
    {
        String name = "homes";
        if (!Essentials.config.commandBlacklist.contains(name))
        {
            String perm;
            PermissionAPI.registerNode(perm = "command." + name, DefaultPermissionLevel.ALL, "Can the player use /"
                    + name);
            // Setup with name and permission
            LiteralArgumentBuilder<CommandSourceStack> command = Commands.literal(name).requires(cs -> CommandManager
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
            LiteralArgumentBuilder<CommandSourceStack> command = Commands.literal(name).requires(cs -> CommandManager
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

    private static int execute(final CommandSourceStack source) throws CommandSyntaxException
    {
        final ServerPlayer player = source.getPlayerOrException();
        HomeManager.sendHomeList(player);
        return 0;
    }

    private static int execute(final CommandSourceStack source, String homeName) throws CommandSyntaxException
    {
        if (homeName == null) homeName = "Home";

        final ServerPlayer player = source.getPlayerOrException();
        final KGobalPos home = HomeManager.getHome(player, homeName);
        if (home == null)
        {
            final Component message = CommandManager.makeFormattedComponent("thutessentials.homes.noexists", null,
                    false, homeName);
            player.sendMessage(message, Util.NIL_UUID);
            return 1;
        }
        final CompoundTag tag = PlayerDataHandler.getCustomDataTag(player);
        final CompoundTag tptag = tag.getCompound("tp");
        final long last = tptag.getLong("homeDelay");
        final long time = player.getServer().getLevel(Level.OVERWORLD).getGameTime();
        if (last > time && Essentials.config.homeReUseDelay > 0)
        {
            final Component message = CommandManager.makeFormattedComponent("thutessentials.tp.tosoon");
            player.sendMessage(message, Util.NIL_UUID);
            return 2;
        }

        Component message = CommandManager.makeFormattedComponent("thutessentials.homes.warping", null, false,
                homeName);
        player.sendMessage(message, Util.NIL_UUID);
        message = CommandManager.makeFormattedComponent("thutessentials.homes.warped", null, false, homeName);
        tptag.putLong("homeDelay", time + Essentials.config.homeReUseDelay);
        tag.put("tp", tptag);
        PlayerDataHandler.saveCustomData(player);
        PlayerMover.setMove(player, Essentials.config.homeActivateDelay, home, message, PlayerMover.INTERUPTED);

        return 0;
    }
}
