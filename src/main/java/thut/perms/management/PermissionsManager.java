package thut.perms.management;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.UUID;
import java.util.function.Predicate;

import javax.annotation.Nullable;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.RootCommandNode;

import net.minecraft.command.CommandSource;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.dimension.DimensionType;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.LogicalSidedProvider;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import net.minecraftforge.server.permission.DefaultPermissionLevel;
import net.minecraftforge.server.permission.IPermissionHandler;
import net.minecraftforge.server.permission.context.IContext;
import thut.essentials.commands.CommandManager;
import thut.perms.Perms;

public class PermissionsManager implements IPermissionHandler
{
    public static final GameProfile                              testProfile          = new GameProfile(new UUID(
            1234567987, 123545787), "_permtest_");
    public static ServerPlayerEntity                             testPlayer;
    public boolean                                               SPDiabled            = false;
    private static final HashMap<String, DefaultPermissionLevel> PERMISSION_LEVEL_MAP = new HashMap<>();
    private static final HashMap<String, String>                 DESCRIPTION_MAP      = new HashMap<>();
    private boolean                                              checkedPerm          = false;
    private static Field                                         REQFIELD             = null;

    static
    {
        PermissionsManager.REQFIELD = CommandNode.class.getDeclaredFields()[3];
        PermissionsManager.REQFIELD.setAccessible(true);
    }

    public void set(final IPermissionHandler permissionHandler)
    {
        for (final String node : permissionHandler.getRegisteredNodes())
        {
            final DefaultPermissionLevel level = permissionHandler.hasPermission(PermissionsManager.testProfile, node,
                    null) ? DefaultPermissionLevel.ALL : DefaultPermissionLevel.OP;
            this.registerNode(node, level, permissionHandler.getNodeDescription(node));
            Perms.LOGGER.info("Copied values for node " + node);
        }
    }

    @Override
    public void registerNode(final String node, final DefaultPermissionLevel level, final String desc)
    {
        PermissionsManager.PERMISSION_LEVEL_MAP.put(node, level);
        if (!desc.isEmpty()) PermissionsManager.DESCRIPTION_MAP.put(node, desc);
    }

    @Override
    public Collection<String> getRegisteredNodes()
    {
        return Collections.unmodifiableSet(PermissionsManager.PERMISSION_LEVEL_MAP.keySet());
    }

    @Override
    public boolean hasPermission(final GameProfile profile, final String node, @Nullable final IContext context)
    {
        this.checkedPerm = true;
        final MinecraftServer server = LogicalSidedProvider.INSTANCE.get(LogicalSide.SERVER);
        if (this.SPDiabled && server.isSinglePlayer()) return true;
        if (GroupManager.get_instance() == null)
        {
            Perms.LOGGER.warn(node + " is being checked before load!");
            Thread.dumpStack();
            return this.getDefaultPermissionLevel(node) == DefaultPermissionLevel.ALL;
        }
        final boolean value = GroupManager.get_instance().hasPermission(profile.getId(), node);
        Perms.LOGGER.info("permnode: " + node + " " + profile + " " + value);
        return value;
    }

    @Override
    public String getNodeDescription(final String node)
    {
        final String desc = PermissionsManager.DESCRIPTION_MAP.get(node);
        return desc == null ? "" : desc;
    }

    /**
     * @return The default permission level of a node. If the permission isn't
     *         registred, it will return NONE
     */
    public DefaultPermissionLevel getDefaultPermissionLevel(final String node)
    {
        final DefaultPermissionLevel level = PermissionsManager.PERMISSION_LEVEL_MAP.get(node);
        return level == null ? DefaultPermissionLevel.NONE : level;
    }

    boolean on = false;

    public void onServerStarting(final FMLServerStartingEvent event)
    {
        final MinecraftServer server = event.getServer();
        if (server == null || this.SPDiabled && server.isSinglePlayer()) return;
        this.wrapCommands(server, event.getCommandDispatcher());
    }

    private void wrap(final CommandNode<CommandSource> node, final CommandNode<CommandSource> parent,
            final CommandSource source)
    {
        if (parent instanceof RootCommandNode<?>)
        {
            this.checkedPerm = false;
            boolean all = node.getRequirement() == null;
            if (!all) all = node.getRequirement().test(PermissionsManager.testPlayer.getCommandSource());
            if (!this.checkedPerm)
            {
                // TODO maybe also check subnodes?

                final String perm = "command." + node.getName();
                this.registerNode(perm, all ? DefaultPermissionLevel.ALL : DefaultPermissionLevel.OP,
                        "auto generated perm for command /" + node.getName());
                final Predicate<CommandSource> req = (cs) ->
                {
                    return CommandManager.hasPerm(cs, perm);
                };
                try
                {
                    PermissionsManager.REQFIELD.set(node, req);
                }
                catch (IllegalArgumentException | IllegalAccessException e)
                {
                    e.printStackTrace();
                }
            }
            return;
        }

        for (final CommandNode<CommandSource> child : node.getChildren())
            this.wrap(child, node, source);
    }

    private void wrapCommands(final MinecraftServer server, final CommandDispatcher<CommandSource> commandDispatcher)
    {
        // shouldn't be null, but might be if something goes funny connecting to
        // servers.
        if (server == null) return;
        PermissionsManager.testPlayer = FakePlayerFactory.get(server.getWorld(DimensionType.OVERWORLD),
                PermissionsManager.testProfile);

        final CommandNode<CommandSource> root = commandDispatcher.getRoot();
        this.wrap(root, null, server.getCommandSource());
    }
}
