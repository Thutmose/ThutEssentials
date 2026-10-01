package thut.essentials.land.claims;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import thut.essentials.Essentials;
import thut.essentials.api.events.ClaimEvent;
import thut.api.level.structures.NamedVolumes;
import thut.api.level.structures.StructureManager;
import thut.essentials.commands.land.claims.Claim;
import thut.essentials.commands.land.claims.Unclaim;
import thut.essentials.land.LandManager;
import thut.essentials.network.Packet;
import thut.essentials.network.nbtpacket.NBTPacket;
import thut.essentials.network.nbtpacket.PacketAssembly;
import thut.essentials.util.PermNodes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@EventBusSubscriber
public class ClaimSyncPacket extends NBTPacket
{
    public static final PacketAssembly<ClaimSyncPacket> ASSEMBLER = PacketAssembly.registerAssembler(
            ClaimSyncPacket.class, ClaimSyncPacket::new, Essentials.packets);

    private static final Set<UUID> KNOWN_RECIEVERS = new HashSet<>();

    public static void syncClaims(ServerPlayer player, CompoundTag tag)
    {
        UUID id = player.getUUID();
        synchronized (KNOWN_RECIEVERS)
        {
            if (!KNOWN_RECIEVERS.contains(id)) return;
        }
        ASSEMBLER.sendTo(tag, player);
    }

    public static void syncClaims(ServerPlayer player)
    {
        UUID id = player.getUUID();
        synchronized (KNOWN_RECIEVERS)
        {
            if (!KNOWN_RECIEVERS.contains(id)) return;
        }
        var volumes = CapabilityWorldVolumes.get(player.level());
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("partial", false);
        tag.put("data", volumes.serializeNBT(player.level().registryAccess()));
        ASSEMBLER.sendTo(tag, player);
    }

    public static void tryClaim(BoundingBox box)
    {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("claim", true);
        tag.put("bounds", BoundingBox.CODEC.encodeStart(NbtOps.INSTANCE, box).getOrThrow());
        ASSEMBLER.sendToServer(tag);
    }

    public static void tryUnclaim(BoundingBox box)
    {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("claim", false);
        tag.put("bounds", BoundingBox.CODEC.encodeStart(NbtOps.INSTANCE, box).getOrThrow());
        ASSEMBLER.sendToServer(tag);
    }

    public static void tryMerge(BoundingBox box) {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("merge", true);
        tag.put("bounds", BoundingBox.CODEC.encodeStart(NbtOps.INSTANCE, box).getOrThrow());
        ASSEMBLER.sendToServer(tag);
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public static void onJoinServer(ClientPlayerNetworkEvent.LoggingIn event)
    {
        CompoundTag tag = new CompoundTag();
        ASSEMBLER.sendToServer(tag);
    }

    @SubscribeEvent
    public static void onJoinLevel(EntityJoinLevelEvent event)
    {
        if (event.getEntity() instanceof ServerPlayer player)
        {
            syncClaims(player);
        }
    }

    @SubscribeEvent
    public static void onLeaveServer(PlayerEvent.PlayerLoggedOutEvent event)
    {
        synchronized (KNOWN_RECIEVERS)
        {
            KNOWN_RECIEVERS.remove(event.getEntity().getUUID());
        }
    }

    public static void resendAll(MinecraftServer server)
    {
        server.getPlayerList().getPlayers().forEach(ClaimSyncPacket::syncClaims);
    }

    @SubscribeEvent
    public static void onClaim(ClaimEvent.Claim event)
    {
        if (!(event.level instanceof ServerLevel level)) return;
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("partial", true);
        tag.putBoolean("add", true);
        tag.put("data", event.volume.serializeNBT(level.registryAccess()));
        level.getServer().getPlayerList().getPlayers().forEach(player -> syncClaims(player, tag));
    }

    @SubscribeEvent
    public static void onUnclaim(ClaimEvent.Unclaim event)
    {
        if (!(event.level instanceof ServerLevel level)) return;
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("partial", true);
        tag.putBoolean("add", false);
        tag.put("data", event.volume.serializeNBT(level.registryAccess()));
        level.getServer().getPlayerList().getPlayers().forEach(player -> syncClaims(player, tag));
    }

    @Override
    protected void onCompleteClient(Player player)
    {
        var tag = this.getTag();
        if (tag.contains("partial"))
        {
            boolean partial = tag.getBoolean("partial");
            var volumes = CapabilityWorldVolumes.get(player.level());
            if (partial)
            {
                var v = new ClaimedVolume();
                v.deserializeNBT(player.level().registryAccess(), tag.getCompound("data"));
                if (tag.getBoolean("add")) volumes.addVolume(v);
                else volumes.removeVolume(v);
            }
            else
            {
                StructureManager.clear();
                volumes.deserializeNBT(player.level().registryAccess(), tag.getCompound("data"));
            }
        }
    }

    @Override
    protected void onCompleteServer(ServerPlayer player)
    {
        var tag = this.getTag();
        UUID id = player.getUUID();
        synchronized (KNOWN_RECIEVERS)
        {
            KNOWN_RECIEVERS.add(id);
        }
        var level = player.level();
        var dim = level.dimension();
        // Sync claims to the player
        if (tag.isEmpty()) syncClaims(player);
        else
        {
            // Otherwise, process the claim or unclaim request
            if (tag.contains("claim"))
            {
                boolean claiming = tag.getBoolean("claim");
                try
                {
                    var bounds = BoundingBox.CODEC.decode(NbtOps.INSTANCE, tag.get("bounds")).result().get().getFirst();
                    if (bounds.getXSpan() > 256 || bounds.getZSpan() > 256)
                    {
                        // TODO error message about too big?
                        return;
                    }
                    if (claiming)
                    {
                        boolean noLimit = PermNodes.getBooleanPerm(player, Claim.BYPASSLIMIT);
                        if (!noLimit && player.distanceToSqr(bounds.getCenter().getCenter()) > 128 * 128)
                        {
                            // TODO error message about too far?
                            return;
                        }
                        Claim.claimBox(player, bounds);
                    }
                    else
                    {
                        Unclaim.unclaimBox(player, bounds);
                    }
                }
                catch (Exception e)
                {
                    Essentials.LOGGER.error(e);
                }
            }
            else if (tag.getBoolean("merge"))
            {
                try
                {
                    var bounds = BoundingBox.CODEC.decode(NbtOps.INSTANCE, tag.get("bounds")).result().get().getFirst();
                    if (bounds.getXSpan() > 256 || bounds.getZSpan() > 256)
                    {
                        // TODO error message about too big?
                        return;
                    }
                    var testVol = new ClaimedVolume(bounds, new ClaimInfo());
                    var claims = StructureManager.getColliding(dim, testVol);
                    if (!claims.isEmpty())
                    {
                        Map<UUID, List<ClaimedVolume>> byGroup = new HashMap<>();
                        claims.forEach(v -> {
                            if (v instanceof ClaimedVolume b)
                                byGroup.computeIfAbsent(b.info.owner, k -> new ArrayList<>()).add(b);
                        });
                        var team = LandManager.getTeam(player);
                        var teamID = team.land.uuid;
                        if (byGroup.containsKey(teamID))
                        {
                            var mergeSet = byGroup.get(teamID);
                            // Try iteratively merging them together?
                            long totalV = mergeSet.stream().mapToLong(ClaimedVolume::computeVolume).sum();
                            List<BoundingBox> allBoxes = new ArrayList<>(
                                    mergeSet.stream().map(ClaimedVolume::getTotalBounds).toList());
                            // try simple merge
                            var bOpt = BoundingBox.encapsulatingBoxes(allBoxes);
                            if (bOpt.isPresent())
                            {
                                var vB = NamedVolumes.computeVolume(bOpt.get());
                                if (vB == totalV)
                                {
                                    // Yay, they all merge!
                                    ClaimInfo info = mergeSet.removeFirst().info;
                                    mergeSet.forEach(i -> info.mergeFrom(i.info));
                                    ClaimedVolume sum = new ClaimedVolume(bOpt.get(), info);
                                    var volumes = CapabilityWorldVolumes.get(level);
                                    mergeSet.forEach(volumes::removeVolume);
                                    volumes.addVolume(sum);
                                }
                            }
                        }
                    }
                }
                catch (Exception e)
                {
                    Essentials.LOGGER.error(e);
                }
            }
        }
    }

    private final static Type<Packet> TYPE = new Type<>(ResourceLocation.parse("thutessentials:claim_sync"));

    @Override
    public Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }
}
