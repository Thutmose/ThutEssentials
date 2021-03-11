package thut.essentials.commands.structures;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.datafixers.util.Either;

import net.minecraft.block.BlockState;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MutableBoundingBox;
import net.minecraft.util.registry.DynamicRegistries;
import net.minecraft.util.registry.WorldGenRegistries;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkPrimer;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.IChunk;
import net.minecraft.world.gen.ChunkGenerator;
import net.minecraft.world.gen.feature.StructureFeature;
import net.minecraft.world.gen.feature.structure.Structure;
import net.minecraft.world.gen.feature.structure.StructureManager;
import net.minecraft.world.gen.feature.structure.StructureStart;
import net.minecraft.world.gen.feature.template.TemplateManager;
import net.minecraft.world.server.ChunkHolder;
import net.minecraft.world.server.ChunkManager;
import net.minecraft.world.server.ServerChunkProvider;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.server.ServerWorldLightManager;
import net.minecraftforge.server.permission.DefaultPermissionLevel;
import net.minecraftforge.server.permission.PermissionAPI;
import thut.essentials.Essentials;
import thut.essentials.commands.CommandManager;
import thut.essentials.land.LandManager;
import thut.essentials.util.world.TickScheduler;
import thut.essentials.util.world.WorldGenRegionWrapper;

public class Structuregen
{
    public static final Set<ChunkPos> toReset = Sets.newConcurrentHashSet();

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
            final boolean doStructures, final ChunkPos mid)
    {
        final ServerWorld world = worldRegion.world;
        final TemplateManager templates = world.getStructureManager();
        final ServerChunkProvider chunkProvider = world.getChunkSource();
        final ChunkGenerator generator = chunkProvider.generator;
        final ServerWorldLightManager lightManager = chunkProvider.getLightEngine();

        final CompletableFuture<Either<IChunk, ChunkHolder.IChunkLoadingError>> future = new CompletableFuture<>();

        final Function<IChunk, CompletableFuture<Either<IChunk, ChunkHolder.IChunkLoadingError>>> loadingFunction = (
                chunk) -> future;

        try
        {
            // The below follow vanilla ordering in general, however some need
            // to use the modified worldRegion instead

            if (doStructures)
            {
                ChunkStatus.STRUCTURE_STARTS.generate(world, generator, templates, lightManager,
                        loadingFunction, primers);
                // This emulates STRUCTURE_REFERENCES
                primers.forEach(c -> generator.createReferences(worldRegion, world.structureFeatureManager().forWorldGenRegion(
                        worldRegion), c));
            }

            ChunkStatus.BIOMES.generate(world, generator, templates, lightManager, loadingFunction, primers);
            // This emulates NOISE
            primers.forEach(c -> generator.fillFromNoise(worldRegion, world.structureFeatureManager().forWorldGenRegion(
                    worldRegion), c));

            ChunkStatus.SURFACE.generate(world, generator, templates, lightManager, loadingFunction, primers);
            ChunkStatus.CARVERS.generate(world, generator, templates, lightManager, loadingFunction, primers);
            ChunkStatus.LIQUID_CARVERS.generate(world, generator, templates, lightManager, loadingFunction,
                    primers);

            // Here we emulate FEATURES
            // primers.forEach(c ->
            // {
            // final ChunkPrimer chunkprimer = (ChunkPrimer) c;
            // Heightmap.updateChunkHeightmaps(c,
            // EnumSet.of(Heightmap.Type.MOTION_BLOCKING,
            // Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,
            // Heightmap.Type.OCEAN_FLOOR,
            // Heightmap.Type.WORLD_SURFACE));
            // generator.applyBiomeDecoration(worldRegion,
            // world.structureFeatureManager().getStructureManager(worldRegion));
            // chunkprimer.setStatus(ChunkStatus.FEATURES);
            // });

            ChunkStatus.LIGHT.generate(world, generator, templates, lightManager, loadingFunction, primers);
            ChunkStatus.SPAWN.generate(world, generator, templates, lightManager, loadingFunction, primers);
            ChunkStatus.HEIGHTMAPS.generate(world, generator, templates, lightManager, loadingFunction,
                    primers);
            ChunkStatus.FULL.generate(world, generator, templates, lightManager, loadingFunction, primers);

            final MutableBoundingBox box = MutableBoundingBox.getUnknownBox();
            box.y0 = minY;
            box.y1 = maxY;
            for (int i = 0; i < primers.size(); i++)
            {
                final ChunkPrimer c = (ChunkPrimer) primers.get(i);
                final Chunk o = originals.get(i);
                final ChunkPos p = o.getPos();
                if (!p.equals(mid)) continue;
                box.x0 = p.getMinBlockX();
                box.z0 = p.getMinBlockZ();
                box.x1 = p.getMaxBlockX();
                box.z1 = p.getMaxBlockZ();

                for (int y = minY; y < maxY; y++)
                    for (int x = box.x0; x <= box.x1; x++)
                        for (int z = box.z0; z <= box.z1; z++)
                        {
                            final BlockPos pos = new BlockPos(x, y, z);
                            final BlockState newstate = c.getBlockState(pos);
                            world.setBlock(pos, newstate, 3 + 32);
                        }
            }

        }
        catch (final Exception e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    private static int execute_reset(final CommandSource source, final int radius) throws CommandSyntaxException
    {

        final ServerPlayerEntity player = source.getPlayerOrException();
        final IChunk chunk = player.getCommandSenderWorld().getChunkAt(player.blockPosition());
        final ServerWorld worldIn = player.getLevel();
        final ChunkPos chunkpos = chunk.getPos();

        final ServerChunkProvider chunkProvider = worldIn.getChunkSource();
        final ChunkManager manager = chunkProvider.chunkMap;

        TickScheduler.Schedule(worldIn.dimension(), () ->
        {
            ChunkPos.rangeClosed(chunkpos, radius).forEach(p ->
            {
                boolean owned = false;
                for (int k = 0; k < 16; k++)
                {
                    owned = !LandManager.isWild(LandManager.getInstance().getLandOwner(worldIn, new BlockPos(p.x, k,
                            p.z), true));
                    if (owned) break;
                }
                if (owned) return;
                final long k = p.toLong();
                final ChunkHolder holder = manager.updatingChunkMap.get(k);
                if (holder != null)
                {
                    manager.updatingChunkMap.remove(k);
                    manager.entitiesInLevel.remove(k);
                    manager.modified = true;
                    holder.setTicketLevel(0);
                }
                Structuregen.toReset.add(p);
            });
            manager.promoteChunkMap();
        }, true);

        source.sendSuccess(new StringTextComponent("Reset Scheduled"), false);
        return 0;
    }

    @SuppressWarnings("unchecked")
    private static int execute_generate(final CommandSource source, final String structname)
            throws CommandSyntaxException
    {
        final ServerPlayerEntity player = source.getPlayerOrException();

        final ResourceLocation key = new ResourceLocation(structname);
        if (!WorldGenRegistries.CONFIGURED_STRUCTURE_FEATURE.keySet().contains(key)) return 1;

        final IChunk chunk = player.getCommandSenderWorld().getChunkAt(player.blockPosition());
        final ServerWorld worldIn = player.getLevel();
        final ChunkGenerator generator = worldIn.getChunkSource().generator;
        final DynamicRegistries reg = worldIn.getServer().registryAccess();
        final TemplateManager templateManager = worldIn.getStructureManager();
        final int refs = 0;
        final long seed = worldIn.getRandom().nextLong();
        final Biome biome = worldIn.getBiome(player.blockPosition());
        final StructureManager structManager = worldIn.structureFeatureManager();
        final ChunkPos pos = chunk.getPos();
        final StructureFeature<?, ?> feature = WorldGenRegistries.CONFIGURED_STRUCTURE_FEATURE.get(key);

        final Structure<?> structure = feature.feature;
        @SuppressWarnings("rawtypes")
        final StructureStart start = structure.createStart(pos.x, pos.z, MutableBoundingBox
                .getUnknownBox(), refs, seed);
        start.generatePieces(reg, generator, templateManager, pos.x, pos.z, biome, feature.config);
        start.placeInChunk(worldIn, structManager, generator, worldIn.getRandom(), MutableBoundingBox
                .infinite(), pos);
        return 0;
    }
}
