package thut.essentials.land.claims;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.INBTSerializable;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import thut.api.level.structures.NamedVolumes;
import thut.api.level.structures.NamedVolumes.INamedVolume;
import thut.api.level.structures.StructureManager;
import thut.essentials.api.events.ClaimEvent;
import thut.essentials.land.LandManager;

public class CapabilityWorldVolumes implements INBTSerializable<CompoundTag>
{
    static
    {
        NamedVolumes.VOLUMES_FACTORY_REGISTRY.put("thutessentials:claim", ClaimedVolume::new);
    }

    public List<INamedVolume> getVolumes()
    {
        return volumes;
    }

    private final List<INamedVolume> volumes = new ArrayList<>();
    private final Level level;

    public CapabilityWorldVolumes(Level level)
    {
        this.level = level;
    }

    public void addVolume(INamedVolume volume)
    {
        if (!this.volumes.contains(volume))
        {
            this.volumes.add(volume);
            StructureManager.addVolume(volume, this.level);
            if (volume instanceof ClaimedVolume claim)
            {
                ClaimEvent.Claim event = new ClaimEvent.Claim(claim, this.level);
                NeoForge.EVENT_BUS.post(event);
            }
        }
    }

    public void removeVolume(INamedVolume volume)
    {
        this.volumes.remove(volume);
        StructureManager.removeVolume(volume, this.level);
        if (volume instanceof ClaimedVolume claim)
        {
            ClaimEvent.Unclaim event = new ClaimEvent.Unclaim(claim, this.level);
            NeoForge.EVENT_BUS.post(event);
        }
    }

    public void removeVolumes(Predicate<INamedVolume> matcher)
    {
        List<INamedVolume> matched = this.volumes.stream().filter(matcher).toList();
        matched.forEach(this::removeVolume);
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider registries)
    {
        CompoundTag tag = new CompoundTag();
        ListTag list = new ListTag();
        this.volumes.forEach(b -> {
            if (b instanceof ClaimedVolume v)
            {
                if (v.info == null) return;
                if (v.info.owner == null) return;
                if (LandManager.instance != null && !LandManager.getInstance()._team_land.containsKey(v.info.owner))
                    return;
            }
            var _tag = NamedVolumes.saveVolumeOrPart(registries, b);
            if (!_tag.isEmpty()) list.add(_tag);
        });
        tag.put("volumes", list);
        return tag;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider registries, CompoundTag nbt)
    {
        ListTag list = nbt.getList("volumes", Tag.TAG_COMPOUND);
        this.volumes.clear();
        list.forEach(tag -> {
            if (tag instanceof CompoundTag comp)
            {
                var volume = NamedVolumes.loadVolume(registries, comp);
                if (volume != null)
                {
                    if (volume instanceof ClaimedVolume v && (v.info.owner == null)) return;
                    this.volumes.add(volume);
                }
            }
        });
        this.volumes.forEach(v -> StructureManager.addVolume(v, this.level));
    }

    public static CapabilityWorldVolumes makeProvider(final IAttachmentHolder in)
    {
        if (!(in instanceof Level level)) return null;
        return new CapabilityWorldVolumes(level);
    }

    public static CapabilityWorldVolumes get(final IAttachmentHolder in)
    {
        return in.getData(TYPE_SAVE.get());
    }

    public static final ResourceLocation LOCSAVEABLE = ResourceLocation.parse("thutessentials:world_volumes");

    public static Supplier<AttachmentType<CapabilityWorldVolumes>> TYPE_SAVE;

    public static void registerAttachment(DeferredRegister<AttachmentType<?>> registry)
    {
        Function<IAttachmentHolder, CapabilityWorldVolumes> func_a = CapabilityWorldVolumes::makeProvider;
        var attach_a = AttachmentType.serializable(func_a).build();
        TYPE_SAVE = registry.register(LOCSAVEABLE.getPath(), () -> attach_a);
    }
}
