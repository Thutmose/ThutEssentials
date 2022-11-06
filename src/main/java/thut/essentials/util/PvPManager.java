package thut.essentials.util;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import thut.essentials.Essentials;
import thut.essentials.util.PermNodes.DefaultPermissionLevel;

public class PvPManager
{

    public static void init()
    {
        MinecraftForge.EVENT_BUS.unregister(PvPManager.class);
        if (!Essentials.config.pvpPerms) return;
        MinecraftForge.EVENT_BUS.register(PvPManager.class);
        PvPManager.registerPerms();
    }

    private static boolean registered = false;

    public static final String PERMPVP = "thutessentials.pvp.allowed";

    public static void registerPerms()
    {
        if (PvPManager.registered) return;
        PvPManager.registered = true;
        // This defaults to OP, as the pvpPerms needed at all defaults to false.
        PermNodes.registerBooleanNode(PvPManager.PERMPVP, DefaultPermissionLevel.OP, "Can the player harm other players.");
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void attack(final AttackEntityEvent evt)
    {
        if (evt.getEntity().getCommandSenderWorld().isClientSide) return;
        if (!Essentials.config.pvpPerms) return;
        if (!(evt.getTarget() instanceof ServerPlayer)) return;
        if (!(evt.getEntity() instanceof ServerPlayer)) return;
        final ServerPlayer attacker = (ServerPlayer) evt.getEntity();
        final ServerPlayer attacked = (ServerPlayer) evt.getTarget();
        if (PermNodes.getBooleanPerm(attacker, PvPManager.PERMPVP)
                && PermNodes.getBooleanPerm(attacked, PvPManager.PERMPVP))
            return;
        evt.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void attack(final LivingAttackEvent evt)
    {
        if (evt.getEntity().getCommandSenderWorld().isClientSide) return;
        if (!Essentials.config.pvpPerms) return;
        if (!(evt.getEntity() instanceof ServerPlayer)) return;
        if (!(evt.getSource().getEntity() instanceof ServerPlayer)) return;
        final ServerPlayer attacker = (ServerPlayer) evt.getSource().getEntity();
        final ServerPlayer attacked = (ServerPlayer) evt.getEntity();
        if (PermNodes.getBooleanPerm(attacker, PvPManager.PERMPVP)
                && PermNodes.getBooleanPerm(attacked, PvPManager.PERMPVP))
            return;
        evt.setCanceled(true);
    }
}
