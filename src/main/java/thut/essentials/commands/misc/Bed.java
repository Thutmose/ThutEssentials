package thut.essentials.commands.misc;

import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

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
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.LogicalSidedProvider;
import net.minecraftforge.server.permission.DefaultPermissionLevel;
import net.minecraftforge.server.permission.PermissionAPI;
import thut.essentials.Essentials;
import thut.essentials.commands.CommandManager;
import thut.essentials.land.LandManager.KGobalPos;
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
        final ServerPlayerEntity player = source.getPlayerOrException();
        final CompoundNBT tag = PlayerDataHandler.getCustomDataTag(player);
        final CompoundNBT tptag = tag.getCompound("tp");
        final long last = tptag.getLong("bedDelay");
        final long time = player.getServer().getLevel(World.OVERWORLD).getGameTime();
        if (last > time && Essentials.config.bedReUseDelay > 0)
        {
            player.sendMessage(Essentials.config.getMessage("thutessentials.tp.tosoon"), Util.NIL_UUID);
            return 1;
        }
        final KGobalPos spot = Bed.getBedSpot(player);
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
        player.sendMessage(Essentials.config.getMessage("thutessentials.bed.nobed"), Util.NIL_UUID);
        return 1;
    }

    private static KGobalPos getBedSpot(final ServerPlayerEntity player)
    {
        if (player.getRespawnPosition() == null) return null;
        final KGobalPos pos = KGobalPos.getPosition(player.getRespawnDimension(), player.getRespawnPosition());
        final KGobalPos spot = pos;
        final MinecraftServer server = LogicalSidedProvider.INSTANCE.get(LogicalSide.SERVER);
        final ServerWorld world = server.getLevel(pos.getDimension());
        if (world == null) return null;
        final BlockPos check = pos.getPos();
        if (Back.valid(check, world)) return spot;
        final int r = Essentials.config.backRangeCheck;
        final Stream<BlockPos> stream = BlockPos.betweenClosedStream(check.getX() - r, check.getY() - r, check.getZ() - r, check
                .getX() + r, check.getY() + r, check.getZ() + r);
        final Optional<BlockPos> opt = stream.filter(p -> Back.valid(p, world)).min((p1, p2) ->
        {
            final double d1 = p1.distSqr(check);
            final double d2 = p2.distSqr(check);
            return Double.compare(d1, d2);
        });
        if (!opt.isPresent()) return null;
        return KGobalPos.getPosition(pos.getDimension(), opt.get().immutable());
    }
}
