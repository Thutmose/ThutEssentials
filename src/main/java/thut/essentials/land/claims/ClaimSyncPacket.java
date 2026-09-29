package thut.essentials.land.claims;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import thut.essentials.Essentials;
import thut.essentials.network.Packet;
import thut.essentials.network.nbtpacket.NBTPacket;
import thut.essentials.network.nbtpacket.PacketAssembly;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@EventBusSubscriber
public class ClaimSyncPacket extends NBTPacket
{
    public static final PacketAssembly<ClaimSyncPacket> ASSEMBLER = PacketAssembly.registerAssembler(
            ClaimSyncPacket.class, ClaimSyncPacket::new, Essentials.packets);

    private static final Set<UUID> KNOWN_RECIEVERS = new HashSet<>();

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

    @Override
    protected void onCompleteClient(Player player)
    {
        var tag = this.getTag();
        if (tag.contains("partial"))
        {
            boolean partial = tag.getBoolean("partial");
            if (partial)
            {
                // TODO add partial support
            }
            else
            {
                var volumes = CapabilityWorldVolumes.get(player.level());
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
        // Sync claims to the player
        if(tag.isEmpty()) syncClaims(player);
        else {
            // Otherwise, process the claim or unclaim request
            System.out.println(tag);
        }
    }

    private final static Type<Packet> TYPE = new Type<Packet>(ResourceLocation.parse("thutessentials:claim_sync"));

    @Override
    public Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }
}
