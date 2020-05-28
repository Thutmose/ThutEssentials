package thut.essentials.util;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.server.permission.DefaultPermissionLevel;
import net.minecraftforge.server.permission.PermissionAPI;
import thut.essentials.Essentials;

public class PvPManager
{

    public static void init()
    {
        MinecraftForge.EVENT_BUS.unregister(PvPManager.class);
        if (!Essentials.config.pvpPerms) return;
        MinecraftForge.EVENT_BUS.register(PvPManager.class);
    }

    private static boolean registered = false;

    public static final String PERMPVP = "thutessentials.pvp.allowed";

    public static void registerPerms()
    {
        if (PvPManager.registered) return;
        PvPManager.registered = true;
        // This defaults to OP, as the pvpPerms needed at all defaults to false.
        PermissionAPI.registerNode(PvPManager.PERMPVP, DefaultPermissionLevel.OP, "Can the player harm other players.");
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void attack(final AttackEntityEvent evt)
    {
        if (evt.getEntity().getEntityWorld().isRemote) return;
        if (!Essentials.config.pvpPerms) return;
        if (!(evt.getTarget() instanceof ServerPlayerEntity)) return;
        if (!(evt.getPlayer() instanceof ServerPlayerEntity)) return;
        final ServerPlayerEntity attacker = (ServerPlayerEntity) evt.getPlayer();
        final ServerPlayerEntity attacked = (ServerPlayerEntity) evt.getTarget();
        if (PermissionAPI.hasPermission(attacker, PvPManager.PERMPVP) && PermissionAPI.hasPermission(attacked,
                PvPManager.PERMPVP)) return;
        evt.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void attack(final LivingAttackEvent evt)
    {
        if (evt.getEntity().getEntityWorld().isRemote) return;
        if (!Essentials.config.pvpPerms) return;
        if (!(evt.getEntity() instanceof ServerPlayerEntity)) return;
        if (!(evt.getSource().getTrueSource() instanceof ServerPlayerEntity)) return;
        final ServerPlayerEntity attacker = (ServerPlayerEntity) evt.getSource().getTrueSource();
        final ServerPlayerEntity attacked = (ServerPlayerEntity) evt.getEntity();
        if (PermissionAPI.hasPermission(attacker, PvPManager.PERMPVP) && PermissionAPI.hasPermission(attacked,
                PvPManager.PERMPVP)) return;
        evt.setCanceled(true);

    }
}
