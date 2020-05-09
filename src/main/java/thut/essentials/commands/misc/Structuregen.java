package thut.essentials.commands.misc;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;

import net.minecraft.block.BlockState;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SharedSeedRandom;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MutableBoundingBox;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkPrimer;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.IChunk;
import net.minecraft.world.chunk.UpgradeData;
import net.minecraft.world.gen.ChunkGenerator;
import net.minecraft.world.gen.GenerationStage;
import net.minecraft.world.gen.GenerationStage.Carving;
import net.minecraft.world.gen.Heightmap;
import net.minecraft.world.gen.WorldGenRegion;
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

        final LiteralArgumentBuilder<CommandSource> command = Commands.literal(name).requires(cs -> CommandManager
                .hasPerm(cs, perm));
        command.then(Commands.argument("structure", StringArgumentType.greedyString()).suggests(
                Structuregen.SUGGEST_NAMES).executes((ctx) -> Structuregen.execute(ctx.getSource(), StringArgumentType
                        .getString(ctx, "structure"))));
        commandDispatcher.register(command);
    }

    private static SuggestionProvider<CommandSource> SUGGEST_NAMES = (ctx, sb) ->
    {
        final List<String> opts = Lists.newArrayList();
        // final ServerPlayerEntity player = ctx.getSource().asPlayer();
        // final ServerWorld worldIn = player.getServerWorld();
        // final Biome biome = worldIn.getBiome(player.getPosition());
        for (final Entry<ResourceLocation, Feature<?>> loc : ForgeRegistries.FEATURES.getEntries())
        {
            if (!(loc.getValue() instanceof Structure<?>)) continue;
            opts.add(loc.getKey().toString());
        }
        return net.minecraft.command.ISuggestionProvider.suggest(opts, sb);
    };

    private static int execute(final CommandSource source, final String structname) throws CommandSyntaxException
    {
        final ServerPlayerEntity player = source.asPlayer();

        try
        {

            IChunk chunk = player.getEntityWorld().getChunkAt(player.getPosition());
            final ServerWorld worldIn = player.getServerWorld();
            final TemplateManager templateManager = worldIn.getSaveHandler().getStructureTemplateManager();

            if (structname.startsWith("reset"))
            {
                final String[] args = structname.split(":");
                int ds = 1;
                if (args.length == 2) ds = Integer.parseInt(args[1]);
                final ChunkGenerator<?> generator = worldIn.getChunkProvider().generator;
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
                        for (int i = 0; i < chunk2.getSections().length; i++)
                            chunk2.getSections()[i] = Chunk.EMPTY_SECTION;
                        primers.add(chunk2);
                        originals.add((Chunk) chunk);
                    }
                final WorldGenRegion world = new WorldGenRegion(worldIn, primers);
                final WorldGenRegion world_for_starts = new WorldGenRegion(worldIn, primers_for_starts);

                for (final IChunk c : primers)
                    generator.initStructureStarts(c, generator, templateManager);

                for (final IChunk c : primers)
                    generator.generateStructureStarts(world_for_starts, c);

                for (final IChunk c : primers)
                    generator.generateBiomes(c);

                for (final IChunk c : primers)
                    generator.makeBase(world, c);

                for (final IChunk c : primers)
                    generator.generateSurface(c);

                for (final IChunk c : primers)
                    generator.carve(c, Carving.AIR);

                for (final IChunk c : primers)
                    generator.carve(c, Carving.LIQUID);

                for (final IChunk c : primers)
                {
                    final ChunkPos pos = c.getPos();
                    final int i = pos.x;
                    final int j = pos.z;
                    final int k = i * 16;
                    final int l = j * 16;
                    final BlockPos blockpos = new BlockPos(k, 0, l);
                    final Biome biome = generator.getBiomeProvider().getBiome(blockpos.add(8, 8, 8));
                    final SharedSeedRandom sharedseedrandom = new SharedSeedRandom();
                    final long i1 = sharedseedrandom.setDecorationSeed(world.getSeed(), k, l);
                    for (final GenerationStage.Decoration generationstage$decoration : GenerationStage.Decoration
                            .values())
                        try
                        {
                            biome.decorate(generationstage$decoration, generator, world_for_starts, i1,
                                    sharedseedrandom, blockpos);
                        }
                        catch (final Exception exception)
                        {
                            System.out.println(exception.getMessage());
                        }
                }

                for (int i = 0; i < primers.size(); i++)
                {
                    final ChunkPrimer c = (ChunkPrimer) primers.get(i);
                    final Chunk o = originals.get(i);

                    final ChunkPos p = o.getPos();
                    for (int j = 0; j < o.getSections().length; j++)
                    {
                        o.getSections()[j] = c.getSections()[j];
                        for (int x = p.getXStart(); x <= p.getXEnd(); x++)
                            for (int z = p.getZStart(); z <= p.getZEnd(); z++)
                                for (int y = j << 4; y <= (j << 4) + 15; y++)
                                {
                                    final BlockPos pos = new BlockPos(x, y, z);
                                    final BlockState state = worldIn.getBlockState(pos);
                                    worldIn.markAndNotifyBlock(pos, null, state, state, 3);
                                }
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

                    for (final Entry<Heightmap.Type, Heightmap> entry : c.func_217311_f())
                        if (ChunkStatus.FULL.getHeightMaps().contains(entry.getKey())) o.getHeightmap(entry.getKey())
                                .setDataArray(entry.getValue().getDataArray());

                    o.setLight(c.hasLight());
                    o.markDirty();
                }

                return 1;
            }
            final ResourceLocation key = new ResourceLocation(structname);

            final Feature<?> feat = ForgeRegistries.FEATURES.getValue(key);
            if (feat instanceof Structure<?>)
            {
                final Structure<?> structure = (Structure<?>) feat;
                final ChunkGenerator<?> generator = worldIn.getChunkProvider().generator;
                ChunkPos chunkpos = chunk.getPos();
                final Biome biome = worldIn.getBiome(player.getPosition());

                final List<IChunk> primers = Lists.newArrayList();
                final List<Chunk> originals = Lists.newArrayList();

                final StructureStart start = structure.getStartFactory().create(structure, chunkpos.x, chunkpos.z,
                        biome, MutableBoundingBox.getNewBoundingBox(), 0, generator.getSeed());
                start.init(generator, templateManager, chunkpos.x, chunkpos.z, biome);
                final SharedSeedRandom rand = new SharedSeedRandom(generator.getSeed());
                rand.setLargeFeatureSeed(generator.getSeed(), chunkpos.x, chunkpos.z);
                final MutableBoundingBox box = start.getBoundingBox();

                final int dx = box.maxX - box.minX;
                final int dz = box.maxZ - box.minZ;
                final int mx = dx / 2 + box.minX >> 4;
                final int mz = dz / 2 + box.minZ >> 4;
                final int ds = Math.max(dx, dz) >> 4;

                chunkpos = new ChunkPos(mx, mz);
                for (int x = mx - ds; x <= mx + ds; x++)
                    for (int z = mz - ds; z <= mz + ds; z++)
                    {
                        final ChunkPos pos = new ChunkPos(x, z);
                        chunk = worldIn.getChunk(x, z);
                        final ChunkPrimer chunk2 = new ChunkPrimer(pos, UpgradeData.EMPTY);
                        for (int i = 0; i < chunk2.getSections().length; i++)
                            chunk2.getSections()[i] = Chunk.EMPTY_SECTION;
                        primers.add(chunk2);
                        originals.add((Chunk) chunk);
                    }
                final WorldGenRegion world = new WorldGenRegion(worldIn, primers);
                for (final IChunk c : primers)
                {
                    generator.generateBiomes(c);
                    generator.makeBase(world, c);
                    generator.generateSurface(c);
                }
                start.generateStructure(world, rand, box, chunkpos);
                final int min_y = box.minY >> 4;
                final int max_y = box.maxY >> 4;
                for (int i = 0; i < primers.size(); i++)
                {
                    final ChunkPrimer c = (ChunkPrimer) primers.get(i);
                    final Chunk o = originals.get(i);

                    for (int j = min_y; j < max_y; j++)
                        o.getSections()[j] = c.getSections()[j];

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

                    for (final Entry<Heightmap.Type, Heightmap> entry : c.func_217311_f())
                        if (ChunkStatus.FULL.getHeightMaps().contains(entry.getKey())) o.getHeightmap(entry.getKey())
                                .setDataArray(entry.getValue().getDataArray());

                    o.setLight(c.hasLight());
                    o.markDirty();
                }

                for (int x = box.minX; x <= box.maxX; x++)
                    for (int y = box.minY; y <= box.maxY; y++)
                        for (int z = box.minZ; z <= box.maxZ; z++)
                        {
                            final BlockPos pos = new BlockPos(x, y, z);
                            final BlockState state = worldIn.getBlockState(pos);
                            worldIn.markAndNotifyBlock(pos, null, state, state, 3);
                        }

            }
            else
            {

            }

        }
        catch (final Exception e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return 0;
    }
}
