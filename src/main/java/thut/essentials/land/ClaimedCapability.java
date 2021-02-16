package thut.essentials.land;

import java.util.Set;
import java.util.UUID;

import com.google.common.collect.Sets;

import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap.Entry;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.nbt.IntArrayNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import thut.essentials.Essentials;
import thut.essentials.land.LandManager.KGobalPos;
import thut.essentials.land.LandManager.LandTeam;

public class ClaimedCapability
{
    public static class ClaimInfo implements INBTSerializable<CompoundNBT>
    {
        public Set<UUID> publicMobs    = Sets.newHashSet();
        public Set<UUID> protectedMobs = Sets.newHashSet();

        public Set<BlockPos> publicBlocks = Sets.newHashSet();

        @Override
        public CompoundNBT serializeNBT()
        {
            final CompoundNBT tag = new CompoundNBT();
            final ListNBT mobListPub = new ListNBT();
            this.publicMobs.forEach(uuid -> mobListPub.add(NBTUtil.func_240626_a_(uuid)));
            tag.put("public_mobs", mobListPub);
            final ListNBT mobListProt = new ListNBT();
            this.protectedMobs.forEach(uuid -> mobListProt.add(NBTUtil.func_240626_a_(uuid)));
            tag.put("protected_mobs", mobListProt);
            final ListNBT pubBlocks = new ListNBT();
            this.publicBlocks.forEach(b -> pubBlocks.add(NBTUtil.writeBlockPos(b)));
            tag.put("public_blocks", pubBlocks);
            return tag;
        }

        @Override
        public void deserializeNBT(final CompoundNBT nbt)
        {

        }
    }

    public static class ClaimSegment implements INBTSerializable<IntArrayNBT>
    {
        /**
         * This is the UUID of the TeamLand associated with this claim, not the
         * Team, or the Player, this allows resetting team land without loading
         * the chunks.
         * When this is first initialized, it will check to see if any teams
         * actually own here, via legacy means, and if so, will set owner to
         * that. Otherwise, owner will be set to TeamLand._WILDUUID_
         */
        public UUID owner = null;

        @Override
        public IntArrayNBT serializeNBT()
        {
            return NBTUtil.func_240626_a_(this.owner);
        }

        @Override
        public void deserializeNBT(final IntArrayNBT nbt)
        {
            try
            {
                this.owner = NBTUtil.readUniqueId(nbt);
            }
            catch (final Exception e)
            {
                Essentials.LOGGER.catching(e);
            }
        }
    }

    public static interface IClaimed
    {
        ClaimSegment getSegment(int yIndex);

        ClaimInfo getInfo();
    }

    public static class Storage implements Capability.IStorage<IClaimed>
    {
        @SuppressWarnings({ "rawtypes", "unchecked" })
        @Override
        public void readNBT(final Capability<IClaimed> capability, final IClaimed instance, final Direction side,
                final INBT nbt)
        {
            if (instance instanceof ICapabilitySerializable) ((ICapabilitySerializable) instance).deserializeNBT(nbt);
        }

        @Override
        public INBT writeNBT(final Capability<IClaimed> capability, final IClaimed instance, final Direction side)
        {
            if (instance instanceof ICapabilitySerializable<?>) return ((ICapabilitySerializable<?>) instance)
                    .serializeNBT();
            return null;
        }
    }

    @CapabilityInject(IClaimed.class)
    public static final Capability<IClaimed> CAPABILITY = null;

    private static final ResourceLocation CAPTAG = new ResourceLocation(Essentials.MODID, "claims");

    public static void setup()
    {
        CapabilityManager.INSTANCE.register(IClaimed.class, new Storage(), ClaimImpl::new);
        MinecraftForge.EVENT_BUS.register(ClaimedCapability.class);
    }

    @SubscribeEvent
    public static void attach(final AttachCapabilitiesEvent<Chunk> event)
    {
        if (event.getCapabilities().containsKey(ClaimedCapability.CAPTAG)) return;
        event.addCapability(ClaimedCapability.CAPTAG, new ClaimImpl(event.getObject()));
    }

    public static class ClaimImpl implements IClaimed, ICapabilitySerializable<CompoundNBT>
    {
        private final LazyOptional<IClaimed> holder = LazyOptional.of(() -> this);

        private final ClaimInfo info = new ClaimInfo();

        private final Int2ObjectArrayMap<ClaimSegment> claims = new Int2ObjectArrayMap<>(16);

        public ClaimImpl()
        {
        }

        public ClaimImpl(final Chunk chunk)
        {
            final World world = chunk.getWorld();
            if (world.isRemote) return;

            for (int y = 0; y < 16; y++)
            {
                final KGobalPos pos = KGobalPos.getPosition(world.getDimensionKey(), new BlockPos(chunk.getPos().x, y,
                        chunk.getPos().z));
                if (LandManager.getInstance()._landMap.containsKey(pos))
                {
                    final LandTeam team = LandManager.getInstance()._landMap.remove(pos);
                    team.land.claims.remove(pos);
                    team.land.claimed++;
                    this.getSegment(y).owner = team.land.uuid;
                    LandSaveHandler.saveTeam(team.teamName);
                }
            }
        }

        @Override
        public ClaimSegment getSegment(final int yIndex)
        {
            ClaimSegment claim = this.claims.get(yIndex);
            if (claim == null) this.claims.put(yIndex, claim = new ClaimSegment());
            return claim;
        }

        @Override
        public ClaimInfo getInfo()
        {
            return this.info;
        }

        @Override
        public <T> LazyOptional<T> getCapability(final Capability<T> cap, final Direction side)
        {
            return ClaimedCapability.CAPABILITY.orEmpty(cap, this.holder);
        }

        @Override
        public CompoundNBT serializeNBT()
        {
            final CompoundNBT tag = new CompoundNBT();
            tag.put("info", this.info.serializeNBT());
            for (final Entry<ClaimSegment> s : this.claims.int2ObjectEntrySet())
            {
                final ClaimSegment claim = s.getValue();
                if (!LandManager.isWild(LandManager.getInstance().getTeamForLand(claim.owner))) tag.put("seg_" + s
                        .getIntKey(), claim.serializeNBT());
            }
            return tag;
        }

        @Override
        public void deserializeNBT(final CompoundNBT nbt)
        {
            this.info.deserializeNBT(nbt.getCompound("info"));
            for (final String key : nbt.keySet())
                if (key.startsWith("seg_")) try
                {
                    final int i = Integer.parseInt(key.replace("seg_", ""));
                    final ClaimSegment claim = new ClaimSegment();
                    claim.deserializeNBT((IntArrayNBT) nbt.get(key));
                    if (!LandManager.isWild(LandManager.getInstance().getTeamForLand(claim.owner))) this.claims.put(i,
                            claim);
                }
                catch (final Exception e)
                {

                }
        }
    }
}
