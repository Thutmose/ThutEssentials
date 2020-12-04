package thut.essentials.commands.misc;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Util;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.server.permission.DefaultPermissionLevel;
import net.minecraftforge.server.permission.PermissionAPI;
import thut.essentials.Essentials;
import thut.essentials.commands.CommandManager;
import thut.essentials.land.LandManager.KGobalPos;
import thut.essentials.util.PlayerDataHandler;
import thut.essentials.util.PlayerMover;

public class Spawn
{
    public static void register(final CommandDispatcher<CommandSource> commandDispatcher)
    {
        final String name = "spawn";
        if (Essentials.config.commandBlacklist.contains(name)) return;
        String perm;
        PermissionAPI.registerNode(perm = "command." + name, DefaultPermissionLevel.ALL, "Can the player use /" + name);

        // Setup with name and permission
        LiteralArgumentBuilder<CommandSource> command = Commands.literal(name).requires(cs -> CommandManager.hasPerm(cs,
                perm));
        // Register the execution.
        command = command.executes(ctx -> Spawn.execute(ctx.getSource()));

        // Actually register the command.
        commandDispatcher.register(command);
    }

    private static int execute(final CommandSource source) throws CommandSyntaxException
    {
        final PlayerEntity player = source.asPlayer();
        final CompoundNBT tag = PlayerDataHandler.getCustomDataTag(player);
        final CompoundNBT tptag = tag.getCompound("tp");
        final long last = tptag.getLong("spawnDelay");
        final long time = player.getServer().getWorld(World.OVERWORLD).getGameTime();
        if (last > time && Essentials.config.spawnReUseDelay > 0)
        {
            player.sendMessage(CommandManager.makeFormattedComponent("thutessentials.tp.tosoon", TextFormatting.RED,
                    false), Util.DUMMY_UUID);
            return 1;
        }
        final MinecraftServer server = player.getServer();
        final KGobalPos spawn = KGobalPos.getPosition(Essentials.config.spawnDimension, server
                .getWorld(Essentials.config.spawnDimension).getSpawnPoint());
        final ITextComponent teleMess = CommandManager.makeFormattedComponent("thutessentials.spawn.succeed");
        PlayerMover.setMove(player, Essentials.config.spawnActivateDelay, spawn, teleMess, PlayerMover.INTERUPTED);
        tptag.putLong("spawnDelay", time + Essentials.config.spawnReUseDelay);
        tag.put("tp", tptag);
        PlayerDataHandler.saveCustomData(player);
        return 0;
    }
}
