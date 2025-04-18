package thut.essentials.util;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import thut.essentials.Essentials;
import thut.essentials.util.PermNodes.DefaultPermissionLevel;

public class PvPManager
{

    public static void init()
    {
        NeoForge.EVENT_BUS.unregister(PvPManager.class);
        if (!Essentials.config.pvpPerms) return;
        NeoForge.EVENT_BUS.register(PvPManager.class);
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
        if (!(evt.getTarget() instanceof ServerPlayer attacked)) return;
        if (!(evt.getEntity() instanceof ServerPlayer attacker)) return;
        if (PermNodes.getBooleanPerm(attacker, PvPManager.PERMPVP)
                && PermNodes.getBooleanPerm(attacked, PvPManager.PERMPVP))
            return;
        evt.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void attack(final LivingDamageEvent.Pre evt)
    {
        if (evt.getEntity().getCommandSenderWorld().isClientSide) return;
        if (!Essentials.config.pvpPerms) return;
        if (!(evt.getEntity() instanceof ServerPlayer attacked)) return;
        if (!(evt.getSource().getEntity() instanceof ServerPlayer attacker)) return;
        if (PermNodes.getBooleanPerm(attacker, PvPManager.PERMPVP)
                && PermNodes.getBooleanPerm(attacked, PvPManager.PERMPVP))
            return;
        evt.setNewDamage(0);
    }
}
