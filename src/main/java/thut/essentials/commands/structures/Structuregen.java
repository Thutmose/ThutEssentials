package thut.essentials.commands.structures;

import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;

import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MutableBoundingBox;
import net.minecraft.util.palette.UpgradeData;
import net.minecraft.util.registry.DynamicRegistries;
import net.minecraft.util.registry.WorldGenRegistries;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkPrimer;
import net.minecraft.world.chunk.IChunk;
import net.minecraft.world.gen.ChunkGenerator;
import net.minecraft.world.gen.feature.StructureFeature;
import net.minecraft.world.gen.feature.structure.Structure;
import net.minecraft.world.gen.feature.structure.StructureManager;
import net.minecraft.world.gen.feature.structure.StructureStart;
import net.minecraft.world.gen.feature.template.TemplateManager;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.server.permission.DefaultPermissionLevel;
import net.minecraftforge.server.permission.PermissionAPI;
import thut.essentials.Essentials;
import thut.essentials.commands.CommandManager;
import thut.essentials.util.world.WorldGenRegionWrapper;

public class Structuregen
{
    public static void register(final CommandDispatcher<CommandSource> commandDispatcher)
    {
        final String name = "gen_structure";
        String perm;
        PermissionAPI.registerNode(perm = "command." + name, DefaultPermissionLevel.OP, "Can the player use /" + name);
        if (Essentials.config.commandBlacklist.contains(name)) return;

        LiteralArgumentBuilder<CommandSource> command;

        command = Commands.literal(name).requires(cs -> CommandManager.hasPerm(cs, perm));
        command.then(Commands.literal("reset").executes((ctx) -> Structuregen.execute_reset(ctx.getSource(), 1)));
        commandDispatcher.register(command);

        command = Commands.literal(name).requires(cs -> CommandManager.hasPerm(cs, perm));
        command.then(Commands.literal("reset").then(Commands.argument("radius", IntegerArgumentType.integer()).executes(
                (ctx) -> Structuregen.execute_reset(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "radius")))));
        commandDispatcher.register(command);

        command = Commands.literal(name).requires(cs -> CommandManager.hasPerm(cs, perm));
        command.then(Commands.argument("structure", StringArgumentType.greedyString()).suggests(
                Structuregen.SUGGEST_NAMES).executes((ctx) -> Structuregen.execute_generate(ctx.getSource(),
                        StringArgumentType.getString(ctx, "structure"))));
        commandDispatcher.register(command);
    }

    private static SuggestionProvider<CommandSource> SUGGEST_NAMES = (ctx, sb) ->
    {
        final List<String> opts = Lists.newArrayList();
        for (final ResourceLocation loc : WorldGenRegistries.CONFIGURED_STRUCTURE_FEATURE.keySet())
            opts.add(loc.toString());
        return net.minecraft.command.ISuggestionProvider.suggest(opts, sb);
    };

    public static void regenerateChunks(final CommandSource source, final List<IChunk> primers,
            final List<Chunk> originals, final WorldGenRegionWrapper worldRegion, final int minY, final int maxY,
            final boolean doStructures)
    {

    }

    private static int execute_reset(final CommandSource source, final int radius) throws CommandSyntaxException
    {
        final ServerPlayerEntity player = source.asPlayer();
        IChunk chunk = player.getEntityWorld().getChunkAt(player.getPosition());
        final ServerWorld worldIn = player.getServerWorld();
        final int ds = radius;
        final ChunkPos chunkpos = chunk.getPos();
        final List<IChunk> primers_for_starts = Lists.newArrayList();
        final Map<ChunkPos, IChunk> all_primers = Maps.newHashMap();
        final List<IChunk> primers = Lists.newArrayList();
        final List<Chunk> originals = Lists.newArrayList();
        final int mx = chunkpos.x;
        final int mz = chunkpos.z;
        final int pad = Math.max(10, ds);
        for (int x = mx - ds - pad; x <= mx + ds + pad; x++)
            for (int z = mz - ds - pad; z <= mz + ds + pad; z++)
            {
                final ChunkPos pos = new ChunkPos(x, z);
                final ChunkPrimer chunk2 = new ChunkPrimer(pos, UpgradeData.EMPTY);
                primers_for_starts.add(chunk2);
                all_primers.put(pos, chunk2);
                if (x < mx - ds || x > mx + ds) continue;
                if (z < mz - ds || z > mz + ds) continue;
                chunk = worldIn.getChunk(x, z);
                primers.add(chunk2);
                originals.add((Chunk) chunk);
            }
        final WorldGenRegionWrapper worldRegion = new WorldGenRegionWrapper(worldIn, primers_for_starts);
        Structuregen.regenerateChunks(source, primers, originals, worldRegion, 0, chunk.getHeight(), true);
        source.sendFeedback(new StringTextComponent("Done"), false);
        return 0;
    }

    @SuppressWarnings("unchecked")
    private static int execute_generate(final CommandSource source, final String structname)
            throws CommandSyntaxException
    {
        final ServerPlayerEntity player = source.asPlayer();

        final ResourceLocation key = new ResourceLocation(structname);
        if (!WorldGenRegistries.CONFIGURED_STRUCTURE_FEATURE.keySet().contains(key)) return 1;

        final IChunk chunk = player.getEntityWorld().getChunkAt(player.getPosition());
        final ServerWorld worldIn = player.getServerWorld();
        final ChunkGenerator generator = worldIn.getChunkProvider().generator;
        final DynamicRegistries reg = worldIn.getServer().func_244267_aX();
        final TemplateManager templateManager = worldIn.getStructureTemplateManager();
        final int refs = 0;
        final long seed = worldIn.getRandom().nextLong();
        final Biome biome = worldIn.getBiome(player.getPosition());
        final StructureManager structManager = worldIn.func_241112_a_();
        final ChunkPos pos = chunk.getPos();
        final StructureFeature<?, ?> feature = WorldGenRegistries.CONFIGURED_STRUCTURE_FEATURE.getOrDefault(key);

        final Structure<?> structure = feature.field_236268_b_;
        @SuppressWarnings("rawtypes")
        final StructureStart start = structure.createStructureStart(pos.x, pos.z, MutableBoundingBox
                .getNewBoundingBox(), refs, seed);
        start.func_230364_a_(reg, generator, templateManager, pos.x, pos.z, biome, feature.field_236269_c_);
        start.func_230366_a_(worldIn, structManager, generator, worldIn.getRandom(), MutableBoundingBox
                .func_236990_b_(), pos);
        return 0;
    }
}
