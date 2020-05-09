package thut.essentials.commands.structures;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;

import net.minecraft.block.BlockState;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.crash.CrashReport;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SharedSeedRandom;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MutableBoundingBox;
import net.minecraft.util.palette.UpgradeData;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeManager;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkPrimer;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.IChunk;
import net.minecraft.world.gen.ChunkGenerator;
import net.minecraft.world.gen.GenerationStage;
import net.minecraft.world.gen.GenerationStage.Carving;
import net.minecraft.world.gen.Heightmap;
import net.minecraft.world.gen.feature.ConfiguredFeature;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.structure.Structure;
import net.minecraft.world.gen.feature.structure.StructureStart;
import net.minecraft.world.gen.feature.template.TemplateManager;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.server.permission.DefaultPermissionLevel;
import net.minecraftforge.server.permission.PermissionAPI;
import thut.essentials.Essentials;
import thut.essentials.commands.CommandManager;

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
        for (final Entry<ResourceLocation, Feature<?>> loc : ForgeRegistries.FEATURES.getEntries())
        {
            if (!(loc.getValue() instanceof Structure<?>)) continue;
            opts.add(loc.getKey().toString());
        }
        return net.minecraft.command.ISuggestionProvider.suggest(opts, sb);
    };

    public static void regenerateChunks(final CommandSource source, final List<IChunk> primers,
            final List<Chunk> originals, final WorldGenRegionWrapper worldRegion, final boolean doStructures)
    {
        final ServerWorld worldIn = worldRegion.world;
        final TemplateManager templateManager = worldIn.getSaveHandler().getStructureTemplateManager();

        final ChunkGenerator<?> generator = worldIn.getChunkProvider().generator;
        final BiomeManager bman = worldIn.getBiomeManager();

        int s = 0;
        source.sendFeedback(new StringTextComponent("Initializing Chunks " + s++), false);
        for (final IChunk c : primers)
            generator.generateStructures(bman, c, generator, templateManager);
        source.sendFeedback(new StringTextComponent("Initializing Chunks " + s++), false);
        for (final IChunk c : primers)
            generator.generateStructureStarts(worldRegion, c);
        source.sendFeedback(new StringTextComponent("Initializing Chunks " + s++), false);
        for (final IChunk c : primers)
            generator.generateBiomes(c);
        source.sendFeedback(new StringTextComponent("Initializing Chunks " + s++), false);
        for (final IChunk c : primers)
            generator.makeBase(worldRegion, c);
        source.sendFeedback(new StringTextComponent("Initializing Chunks " + s++), false);
        for (final IChunk c : primers)
            generator.func_225551_a_(worldRegion, c);
        source.sendFeedback(new StringTextComponent("Initializing Chunks " + s++), false);
        for (final IChunk c : primers)
            generator.func_225550_a_(bman, c, Carving.AIR);
        source.sendFeedback(new StringTextComponent("Initializing Chunks " + s++), false);
        for (final IChunk c : primers)
            generator.func_225550_a_(bman, c, Carving.LIQUID);
        source.sendFeedback(new StringTextComponent("Initializing Chunks " + s++), false);

        for (int i = 0; i < primers.size(); i++)
        {
            final ChunkPrimer c = (ChunkPrimer) primers.get(i);
            final Chunk o = originals.get(i);
            final ChunkPos p = o.getPos();
            for (int y = 0; y < (o.getSections().length << 4) + 15; y++)
                for (int x = p.getXStart(); x <= p.getXEnd(); x++)
                    for (int z = p.getZStart(); z <= p.getZEnd(); z++)
                    {
                        final BlockPos pos = new BlockPos(x, y, z);
                        final BlockState newstate = c.getBlockState(pos);
                        worldIn.setBlockState(pos, newstate, 3 + 32);
                    }

            for (final CompoundNBT compoundnbt : c.getEntities())
                EntityType.func_220335_a(compoundnbt, worldIn, (p_217325_1_) ->
                {
                    o.addEntity(p_217325_1_);
                    return p_217325_1_;
                });

            for (final TileEntity tileentity : c.getTileEntities().values())
                o.addTileEntity(tileentity);

            o.setStructureStarts(c.getStructureStarts());
            o.setStructureReferences(c.getStructureReferences());

            for (final Entry<Heightmap.Type, Heightmap> entry : c.getHeightmaps())
                if (ChunkStatus.FULL.getHeightMaps().contains(entry.getKey())) o.getHeightmap(entry.getKey())
                        .setDataArray(entry.getValue().getDataArray());

            o.setLight(c.hasLight());
            o.markDirty();
        }
        source.sendFeedback(new StringTextComponent("Regenerating Features and Structures"), false);
        for (final IChunk c : primers)
        {
            final ChunkPos cpos = c.getPos();
            final int cx = cpos.x;
            final int cj = cpos.z;
            final int k = cx * 16;
            final int l = cj * 16;
            final BlockPos pos = new BlockPos(k, 0, l);
            final Biome biome = bman.getBiome(pos.add(8, 8, 8));
            final SharedSeedRandom random = new SharedSeedRandom();
            final long seed = random.setDecorationSeed(worldRegion.getSeed(), k, l);
            for (final GenerationStage.Decoration stage : GenerationStage.Decoration.values())
                try
                {
                    int i = 0;

                    for (final ConfiguredFeature<?, ?> feature : biome.getFeatures(stage))
                    {
                        if (!doStructures && feature.feature instanceof Structure<?>)
                        {
                            ++i;
                            continue;
                        }
                        random.setFeatureSeed(seed, i, stage.ordinal());
                        try
                        {
                            feature.place(worldRegion, generator, random, pos);
                        }
                        catch (final Exception exception)
                        {
                            final CrashReport crashreport = CrashReport.makeCrashReport(exception, "Feature placement");
                            crashreport.makeCategory("Feature").addDetail("Id", ForgeRegistries.FEATURES.getKey(
                                    feature.feature)).addDetail("Description", () ->
                                    {
                                        return feature.feature.toString();
                                    });
                            Essentials.LOGGER.error(crashreport.getCompleteReport());
                        }
                        ++i;
                    }
                }
                catch (final Exception exception)
                {
                    Essentials.LOGGER.error(exception);
                }
        }
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
        Structuregen.regenerateChunks(source, primers, originals, worldRegion, true);
        source.sendFeedback(new StringTextComponent("Done"), false);
        return 0;
    }

    private static int execute_generate(final CommandSource source, final String structname)
            throws CommandSyntaxException
    {
        final ServerPlayerEntity player = source.asPlayer();

        IChunk chunk = player.getEntityWorld().getChunkAt(player.getPosition());
        final ServerWorld worldIn = player.getServerWorld();
        final TemplateManager templateManager = worldIn.getSaveHandler().getStructureTemplateManager();
        final ResourceLocation key = new ResourceLocation(structname);

        final Feature<?> feat = ForgeRegistries.FEATURES.getValue(key);
        if (feat instanceof Structure<?>)
        {
            final Structure<?> structure = (Structure<?>) feat;
            final ChunkGenerator<?> generator = worldIn.getChunkProvider().generator;
            ChunkPos chunkpos = chunk.getPos();
            final StructureStart start = structure.getStartFactory().create(structure, chunkpos.x, chunkpos.z,
                    MutableBoundingBox.getNewBoundingBox(), 0, generator.getSeed());

            final Biome biome = worldIn.getBiome(player.getPosition());
            final List<IChunk> primers = Lists.newArrayList();
            final List<Chunk> originals = Lists.newArrayList();
            start.init(generator, templateManager, chunkpos.x, chunkpos.z, biome);
            final SharedSeedRandom rand = new SharedSeedRandom(generator.getSeed());
            rand.setLargeFeatureSeed(generator.getSeed(), chunkpos.x, chunkpos.z);
            final MutableBoundingBox box = start.getBoundingBox();

            final int dx = box.maxX - box.minX;
            final int dz = box.maxZ - box.minZ;
            final int mx = dx / 2 + box.minX >> 4;
            final int mz = dz / 2 + box.minZ >> 4;
            final int ds = Math.max(dx, dz) >> 4;
            final List<IChunk> primers_for_starts = Lists.newArrayList();
            final Map<ChunkPos, IChunk> all_primers = Maps.newHashMap();

            chunkpos = new ChunkPos(mx, mz);
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
            Structuregen.regenerateChunks(source, primers, originals, worldRegion, true);
            source.sendFeedback(new StringTextComponent("Generating Requested Structure"), false);
            start.func_225565_a_(worldRegion, generator, rand, box, chunkpos);
            source.sendFeedback(new StringTextComponent("Done"), false);
            return 0;
        }
        else
        {

        }
        return 0;
    }
}
