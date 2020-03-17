package thut.essentials.commands.misc;

import java.util.List;
import java.util.Random;

import com.google.common.collect.Lists;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;

import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.block.BlockState;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MutableBoundingBox;
import net.minecraft.util.palette.UpgradeData;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkPrimer;
import net.minecraft.world.chunk.IChunk;
import net.minecraft.world.gen.WorldGenRegion;
import net.minecraft.world.gen.feature.structure.StructureStart;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.server.permission.DefaultPermissionLevel;
import net.minecraftforge.server.permission.PermissionAPI;
import thut.essentials.Essentials;
import thut.essentials.commands.CommandManager;

public class Regen
{

    public static void register(final CommandDispatcher<CommandSource> commandDispatcher)
    {
        final String name = "regen_structure";
        String perm;
        PermissionAPI.registerNode(perm = "command." + name, DefaultPermissionLevel.OP, "Can the player use /" + name);
        if (Essentials.config.commandBlacklist.contains(name)) return;

        final LiteralArgumentBuilder<CommandSource> command = Commands.literal(name).requires(cs -> CommandManager
                .hasPerm(cs, perm));
        command.then(Commands.argument("structure", StringArgumentType.greedyString()).suggests(Regen.SUGGEST_NAMES)
                .executes((ctx) -> Regen.execute(ctx.getSource(), StringArgumentType.getString(ctx, "structure"))));
        commandDispatcher.register(command);
    }

    private static SuggestionProvider<CommandSource> SUGGEST_NAMES = (ctx, sb) ->
    {
        final BlockPos pos = new BlockPos(ctx.getSource().getPos());
        final Chunk chunk = ctx.getSource().getWorld().getChunkAt(pos);
        final List<String> opts = Lists.newArrayList();
        names:
        for (final String name : chunk.getStructureReferences().keySet())
            for (final Long olong : chunk.getStructureReferences().get(name))
            {
                final ChunkPos chunkpos = new ChunkPos(olong);
                if (chunk.getPos().equals(chunkpos))
                {
                    opts.add(name);
                    continue names;
                }
            }
        return net.minecraft.command.ISuggestionProvider.suggest(opts, sb);
    };

    private static int execute(final CommandSource source, final String structname) throws CommandSyntaxException
    {
        final ServerPlayerEntity player = source.asPlayer();

        final IChunk chunk = player.getEntityWorld().getChunkAt(player.getPosition());
        final LongSet list = chunk.getStructureReferences().get(structname);
        final ServerWorld worldIn = player.getServerWorld();
        if (list != null) for (final Long olong : list)
        {
            final ChunkPos chunkpos = new ChunkPos(olong);
            final StructureStart structurestart = worldIn.getChunk(chunkpos.x, chunkpos.z).getStructureStart(
                    structname);
            final Random rand = worldIn.getRandom();
            if (structurestart != null && structurestart != StructureStart.DUMMY)
            {
                final MutableBoundingBox box = structurestart.getBoundingBox();
                try
                {
                    final List<IChunk> primers = Lists.newArrayList();

                    final int dx = box.maxX - box.minX;
                    final int dz = box.maxZ - box.minZ;
                    final int mx = dx / 2 + box.minX >> 4;
                    final int mz = dz / 2 + box.minZ >> 4;
                    final int ds = Math.max(dx, dz) >> 4;

                    for (int i = mx - ds; i <= mx + ds; i++)
                        for (int j = mz - ds; j <= mz + ds; j++)
                        {
                            final IChunk chunk2 = new ChunkPrimer(new ChunkPos(i, j), UpgradeData.EMPTY);
                            primers.add(chunk2);
                        }
                    final WorldGenRegion world = new WorldGenRegion(worldIn, primers);
                    structurestart.func_225565_a_(world, worldIn.getChunkProvider().getChunkGenerator(), rand, box,
                            chunkpos);
                    for (int i = box.minX; i <= box.maxX; i++)
                        for (int j = box.minY; j <= box.maxY; j++)
                            for (int k = box.minZ; k <= box.maxZ; k++)
                            {
                                final BlockPos pos = new BlockPos(i, j, k);
                                final BlockState state = world.getBlockState(pos);
                                if (!state.isAir(worldIn, pos)) worldIn.setBlockState(pos, state, 3);
                            }
                }
                catch (final Exception e)
                {
                    e.printStackTrace();
                }
                return 0;

            }
        }
        return 0;
    }
}
