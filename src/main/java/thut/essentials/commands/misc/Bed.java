package thut.essentials.commands.misc;

import java.util.function.Predicate;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.GlobalPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.LogicalSidedProvider;
import net.minecraftforge.server.permission.DefaultPermissionLevel;
import net.minecraftforge.server.permission.PermissionAPI;
import thut.essentials.Essentials;
import thut.essentials.commands.CommandManager;
import thut.essentials.util.PlayerDataHandler;
import thut.essentials.util.PlayerMover;

public class Bed
{
    public static void register(final CommandDispatcher<CommandSource> commandDispatcher)
    {
        final String name = "bed";
        if (Essentials.config.commandBlacklist.contains(name)) return;
        String perm;
        PermissionAPI.registerNode(perm = "command." + name, DefaultPermissionLevel.ALL, "Can the player use /" + name);

        // Setup with name and permission
        LiteralArgumentBuilder<CommandSource> command = Commands.literal(name).requires(cs -> CommandManager.hasPerm(cs,
                perm));
        // Register the execution.
        command = command.executes(ctx -> Bed.execute(ctx.getSource()));

        // Actually register the command.
        commandDispatcher.register(command);
    }

    private static int execute(final CommandSource source) throws CommandSyntaxException
    {
        final ServerPlayerEntity player = source.asPlayer();
        final CompoundNBT tag = PlayerDataHandler.getCustomDataTag(player);
        final CompoundNBT tptag = tag.getCompound("tp");
        final long last = tptag.getLong("bedDelay");
        final long time = player.getServer().getWorld(World.OVERWORLD).getGameTime();
        if (last > time && Essentials.config.bedReUseDelay > 0)
        {
            player.sendMessage(Essentials.config.getMessage("thutessentials.tp.tosoon"), Util.DUMMY_UUID);
            return 1;
        }
        final GlobalPos spot = Bed.getBedSpot(player);
        if (spot != null)
        {
            final Predicate<Entity> callback = t ->
            {
                if (!(t instanceof PlayerEntity)) return false;
                tptag.putLong("bedDelay", time + Essentials.config.bedReUseDelay);
                tag.put("tp", tptag);
                PlayerDataHandler.saveCustomData((PlayerEntity) t);
                return true;
            };
            final ITextComponent teleMess = Essentials.config.getMessage("thutessentials.bed.succeed");
            PlayerMover.setMove(player, Essentials.config.bedActivateDelay, spot, teleMess, PlayerMover.INTERUPTED,
                    callback, false);
            return 0;
        }
        player.sendMessage(Essentials.config.getMessage("thutessentials.bed.nobed"), Util.DUMMY_UUID);
        return 1;
    }

    private static GlobalPos getBedSpot(final ServerPlayerEntity player)
    {
        if (!player.getBedPosition().isPresent()) return null;
        final GlobalPos pos = GlobalPos.getPosition(player.func_241141_L_(), player.getBedPosition().get());
        GlobalPos spot = pos;
        final MinecraftServer server = LogicalSidedProvider.INSTANCE.get(LogicalSide.SERVER);
        final ServerWorld world = server.getWorld(pos.getDimension());
        if (world == null) return null;
        final BlockPos check = pos.getPos();
        if (Back.valid(check, world)) return spot;
        final int r = Essentials.config.backRangeCheck;
        BlockPos test;
        for (int j = 0; j < r; j++)
            for (int i = 0; i < r; i++)
                for (int k = 0; k < r; k++)
                {
                    test = new BlockPos(check.getX() + i, check.getY() + j, check.getX() + k);
                    spot = GlobalPos.getPosition(pos.getDimension(), test);
                    if (Back.valid(check, world)) return spot;
                    test = new BlockPos(check.getX() - i, check.getY() + j, check.getX() + k);
                    spot = GlobalPos.getPosition(pos.getDimension(), test);
                    if (Back.valid(check, world)) return spot;
                    test = new BlockPos(check.getX() - i, check.getY() + j, check.getX() - k);
                    spot = GlobalPos.getPosition(pos.getDimension(), test);
                    if (Back.valid(check, world)) return spot;
                    test = new BlockPos(check.getX() + i, check.getY() + j, check.getX() - k);
                    spot = GlobalPos.getPosition(pos.getDimension(), test);
                    if (Back.valid(check, world)) return spot;
                }
        return null;
    }
}
