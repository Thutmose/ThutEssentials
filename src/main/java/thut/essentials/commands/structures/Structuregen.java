package thut.essentials.commands.structures;

import java.util.List;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.data.BuiltinRegistries;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import thut.essentials.Essentials;
import thut.essentials.commands.CommandManager;
import thut.essentials.util.PermNodes;
import thut.essentials.util.PermNodes.DefaultPermissionLevel;
import thut.essentials.util.world.WorldGenRegionWrapper;

public class Structuregen
{
    public static final Set<ChunkPos> toReset = Sets.newConcurrentHashSet();

    public static void register(final CommandDispatcher<CommandSourceStack> commandDispatcher)
    {
        final String name = "gen_structure";
        String perm;
        PermNodes.registerNode(perm = "command." + name, DefaultPermissionLevel.OP, "Can the player use /" + name);
        if (Essentials.config.commandBlacklist.contains(name)) return;

        LiteralArgumentBuilder<CommandSourceStack> command;

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

    private static SuggestionProvider<CommandSourceStack> SUGGEST_NAMES = (ctx, sb) ->
    {
        final List<String> opts = Lists.newArrayList();
        for (final ResourceLocation loc : BuiltinRegistries.CONFIGURED_STRUCTURE_FEATURE.keySet())
            opts.add(loc.toString());
        return net.minecraft.commands.SharedSuggestionProvider.suggest(opts, sb);
    };

    public static void regenerateChunks(final CommandSourceStack source, final List<ChunkAccess> primers,
            final List<LevelChunk> originals, final WorldGenRegionWrapper worldRegion, final int minY, final int maxY,
            final boolean doStructures, final ChunkPos mid)
    {
//        final ServerLevel world = worldRegion.world;
//        final StructureManager templates = world.getStructureManager();
//        final ServerChunkCache chunkProvider = world.getChunkSource();
//        final ChunkGenerator generator = chunkProvider.generator;
//        final ThreadedLevelLightEngine lightManager = chunkProvider.getLightEngine();
//
//        final CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> future = new CompletableFuture<>();
//
//        final Function<ChunkAccess, CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>>> loadingFunction = (
//                chunk) -> future;

        try
        {
            // The below follow vanilla ordering in general, however some need
            // to use the modified worldRegion instead
//
//            if (doStructures)
//            {
//                ChunkStatus.STRUCTURE_STARTS.generate(world, generator, templates, lightManager,
//                        loadingFunction, primers);
//                // This emulates STRUCTURE_REFERENCES
//                primers.forEach(c -> generator.createReferences(worldRegion, world.structureFeatureManager().forWorldGenRegion(
//                        worldRegion), c));
//            }
//
//            ChunkStatus.BIOMES.generate(world, generator, templates, lightManager, loadingFunction, primers);
//            // This emulates NOISE
//            primers.forEach(c -> generator.fillFromNoise(worldRegion, world.structureFeatureManager().forWorldGenRegion(
//                    worldRegion), c));
//
//            ChunkStatus.SURFACE.generate(world, generator, templates, lightManager, loadingFunction, primers);
//            ChunkStatus.CARVERS.generate(world, generator, templates, lightManager, loadingFunction, primers);
//            ChunkStatus.LIQUID_CARVERS.generate(world, generator, templates, lightManager, loadingFunction,
//                    primers);
//
//            // Here we emulate FEATURES
//            // primers.forEach(c ->
//            // {
//            // final ChunkPrimer chunkprimer = (ChunkPrimer) c;
//            // Heightmap.updateChunkHeightmaps(c,
//            // EnumSet.of(Heightmap.Type.MOTION_BLOCKING,
//            // Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,
//            // Heightmap.Type.OCEAN_FLOOR,
//            // Heightmap.Type.WORLD_SURFACE));
//            // generator.applyBiomeDecoration(worldRegion,
//            // world.structureFeatureManager().getStructureManager(worldRegion));
//            // chunkprimer.setStatus(ChunkStatus.FEATURES);
//            // });
//
//            ChunkStatus.LIGHT.generate(world, generator, templates, lightManager, loadingFunction, primers);
//            ChunkStatus.SPAWN.generate(world, generator, templates, lightManager, loadingFunction, primers);
//            ChunkStatus.HEIGHTMAPS.generate(world, generator, templates, lightManager, loadingFunction,
//                    primers);
//            ChunkStatus.FULL.generate(world, generator, templates, lightManager, loadingFunction, primers);
//
//            final BoundingBox box = BoundingBox.getUnknownBox();
//            box.y0 = minY;
//            box.y1 = maxY;
//            for (int i = 0; i < primers.size(); i++)
//            {
//                final ProtoChunk c = (ProtoChunk) primers.get(i);
//                final LevelChunk o = originals.get(i);
//                final ChunkPos p = o.getPos();
//                if (!p.equals(mid)) continue;
//                box.x0 = p.getMinBlockX();
//                box.z0 = p.getMinBlockZ();
//                box.x1 = p.getMaxBlockX();
//                box.z1 = p.getMaxBlockZ();
//
//                for (int y = minY; y < maxY; y++)
//                    for (int x = box.x0; x <= box.x1; x++)
//                        for (int z = box.z0; z <= box.z1; z++)
//                        {
//                            final BlockPos pos = new BlockPos(x, y, z);
//                            final BlockState newstate = c.getBlockState(pos);
//                            world.setBlock(pos, newstate, 3 + 32);
//                        }
//            }

        }
        catch (final Exception e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    private static int execute_reset(final CommandSourceStack source, final int radius) throws CommandSyntaxException
    {
//
//        final ServerPlayer player = source.getPlayerOrException();
//        final ChunkAccess chunk = player.getCommandSenderWorld().getChunkAt(player.blockPosition());
//        final ServerLevel worldIn = player.getLevel();
//        final ChunkPos chunkpos = chunk.getPos();
//
//        final ServerChunkCache chunkProvider = worldIn.getChunkSource();
//        final ChunkMap manager = chunkProvider.chunkMap;
//
//        TickScheduler.Schedule(worldIn.dimension(), () ->
//        {
//            ChunkPos.rangeClosed(chunkpos, radius).forEach(p ->
//            {
//                boolean owned = false;
//                for (int k = 0; k < 16; k++)
//                {
//                    owned = !LandManager.isWild(LandManager.getInstance().getLandOwner(worldIn, new BlockPos(p.x, k,
//                            p.z), true));
//                    if (owned) break;
//                }
//                if (owned) return;
//                final long k = p.toLong();
//                final ChunkHolder holder = manager.updatingChunkMap.get(k);
//                if (holder != null)
//                {
//                    manager.updatingChunkMap.remove(k);
//                    manager.entitiesInLevel.remove(k);
//                    manager.modified = true;
//                    holder.setTicketLevel(0);
//                }
//                Structuregen.toReset.add(p);
//            });
//            manager.promoteChunkMap();
//        }, true);

        source.sendSuccess(new TextComponent("Reset Scheduled"), false);
        return 0;
    }

    private static int execute_generate(final CommandSourceStack source, final String structname)
            throws CommandSyntaxException
    {
//        final ServerPlayer player = source.getPlayerOrException();
//
//        final ResourceLocation key = new ResourceLocation(structname);
//        if (!BuiltinRegistries.CONFIGURED_STRUCTURE_FEATURE.keySet().contains(key)) return 1;
//
//        final ChunkAccess chunk = player.getCommandSenderWorld().getChunkAt(player.blockPosition());
//        final ServerLevel worldIn = player.getLevel();
//        final ChunkGenerator generator = worldIn.getChunkSource().generator;
//        final RegistryAccess reg = worldIn.getServer().registryAccess();
//        final StructureManager templateManager = worldIn.getStructureManager();
//        final int refs = 0;
//        final long seed = worldIn.getRandom().nextLong();
//        final Biome biome = worldIn.getBiome(player.blockPosition());
//        final StructureFeatureManager structManager = worldIn.structureFeatureManager();
//        final ChunkPos pos = chunk.getPos();
//        final ConfiguredStructureFeature<?, ?> feature = BuiltinRegistries.CONFIGURED_STRUCTURE_FEATURE.get(key);
//
//        final StructureFeature<?> structure = feature.feature;
//        @SuppressWarnings("rawtypes")
//        final StructureStart start = structure.createStart(pos.x, pos.z, BoundingBox.infinite(), refs, seed);
//        start.generatePieces(reg, generator, templateManager, pos.x, pos.z, biome, feature.config);
//        start.placeInChunk(worldIn, structManager, generator, worldIn.getRandom(), BoundingBox
//                .infinite(), pos);
        return 0;
    }
}
