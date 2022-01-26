package thut.essentials.commands.misc;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import thut.essentials.Essentials;
import thut.essentials.commands.CommandManager;
import thut.essentials.land.LandManager.KGobalPos;
import thut.essentials.util.PermNodes;
import thut.essentials.util.PermNodes.DefaultPermissionLevel;
import thut.essentials.util.PlayerDataHandler;
import thut.essentials.util.PlayerMover;

public class Spawn
{
    public static void register(final CommandDispatcher<CommandSourceStack> commandDispatcher)
    {
        final String name = "spawn";
        if (Essentials.config.commandBlacklist.contains(name)) return;
        String perm;
        PermNodes.registerNode(perm = "command." + name, DefaultPermissionLevel.ALL, "Can the player use /" + name);

        // Setup with name and permission
        LiteralArgumentBuilder<CommandSourceStack> command = Commands.literal(name).requires(cs -> CommandManager.hasPerm(cs,
                perm));
        // Register the execution.
        command = command.executes(ctx -> Spawn.execute(ctx.getSource()));

        // Actually register the command.
        commandDispatcher.register(command);
    }

    private static int execute(final CommandSourceStack source) throws CommandSyntaxException
    {
        final Player player = source.getPlayerOrException();
        final CompoundTag tag = PlayerDataHandler.getCustomDataTag(player);
        final CompoundTag tptag = tag.getCompound("tp");
        final long last = tptag.getLong("spawnDelay");
        final long time = player.getServer().getLevel(Level.OVERWORLD).getGameTime();
        if (last > time && Essentials.config.spawnReUseDelay > 0)
        {
            player.sendMessage(CommandManager.makeFormattedComponent("thutessentials.tp.tosoon", ChatFormatting.RED,
                    false), Util.NIL_UUID);
            return 1;
        }
        final MinecraftServer server = player.getServer();
        final KGobalPos spawn = KGobalPos.getPosition(Essentials.config.spawnDimension, server
                .getLevel(Essentials.config.spawnDimension).getSharedSpawnPos());
        final Component teleMess = CommandManager.makeFormattedComponent("thutessentials.spawn.succeed");
        PlayerMover.setMove(player, Essentials.config.spawnActivateDelay, spawn, teleMess, PlayerMover.INTERUPTED);
        tptag.putLong("spawnDelay", time + Essentials.config.spawnReUseDelay);
        tag.put("tp", tptag);
        PlayerDataHandler.saveCustomData(player);
        return 0;
    }
}
