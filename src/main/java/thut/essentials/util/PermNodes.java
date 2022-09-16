package thut.essentials.util;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.mojang.authlib.GameProfile;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;
import net.minecraftforge.server.permission.PermissionAPI;
import net.minecraftforge.server.permission.events.PermissionGatherEvent;
import net.minecraftforge.server.permission.nodes.PermissionNode;
import net.minecraftforge.server.permission.nodes.PermissionTypes;
import thut.essentials.Essentials;
import thut.essentials.economy.EconomyManager;
import thut.essentials.land.LandEventsHandler;

@Mod.EventBusSubscriber
public class PermNodes
{
    public static enum DefaultPermissionLevel
    {
        ALL, OP, NONE;

        public boolean matches(UUID player)
        {
            if (this == NONE) return false;
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            Optional<GameProfile> profile = server != null ? server.getProfileCache().get(player) : Optional.empty();
            boolean op = false;
            if (profile.isPresent())
            {
                op = server.getPlayerList().isOp(profile.get());
            }
            else
            {
                op = server.getPlayerList().isOp(testProfile);
            }
            return op ? true : this == ALL;
        }
    }

    public static final GameProfile testProfile = new GameProfile(new UUID(1234567987, 123545787), "_permtest_");
    public static ServerPlayer testPlayer;

    private static final Map<String, PermissionNode<?>> NODES = Maps.newHashMap();

    @SuppressWarnings("unchecked")
    public static PermissionNode<Boolean> getBooleanNode(String name)
    {
        return (PermissionNode<Boolean>) NODES.get(name);
    }

    public static boolean getBooleanPerm(ServerPlayer player, String name)
    {
        PermissionNode<Boolean> node = getBooleanNode(name);
        return PermissionAPI.getPermission(player, node);
    }

    public static void registerNode(String name, DefaultPermissionLevel level, String message)
    {
        if (NODES.containsKey(name)) return;

        PermissionNode<Boolean> node = new PermissionNode<>(Essentials.MODID, name, PermissionTypes.BOOLEAN,
                (player, playerUUID, context) -> level.matches(playerUUID));
        node.setInformation(Component.literal(node.getNodeName()), Component.literal(message));
        NODES.put(name, node);
        NODES.put(node.getNodeName(), node);
    }

    public static void registerNode(PermissionNode<?> node)
    {
        NODES.put(node.getNodeName(), node);
    }

    @SubscribeEvent
    public static void gatherPerms(PermissionGatherEvent.Nodes event)
    {
        HomeManager.registerPerms();
        KitManager.registerPerms();
        WarpManager.registerPerms();
        LandEventsHandler.TEAMMANAGER.registerPerms();
        EconomyManager.registerPerms();

        Set<PermissionNode<?>> nodes = Sets.newHashSet();
        nodes.addAll(NODES.values());
        for (var node : nodes) try
        {
            event.addNodes(node);
        }
        catch (Exception e)
        {
            continue;
        }
    }
}
