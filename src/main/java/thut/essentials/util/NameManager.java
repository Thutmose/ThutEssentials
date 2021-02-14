package thut.essentials.util;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.play.server.SPlayerListItemPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent.NameFormat;
import net.minecraftforge.fml.server.ServerLifecycleHooks;

public class NameManager
{

    private static void onPlayerDisplayName(final NameFormat event)
    {
        final CompoundNBT tag = PlayerDataHandler.getCustomDataTag(event.getPlayer());
        String nick = tag.getString("nick");
        final String pref = tag.getString("nick_pref");
        final String suff = tag.getString("nick_suff");
        if (nick.trim().isEmpty() && pref.trim().isEmpty() && suff.trim().isEmpty()) return;
        final String old = event.getDisplayname().getString();
        if (nick.trim().isEmpty()) nick = old;
        nick = pref + nick + suff;
        if (nick.length() > 16) nick = nick.substring(0, 16);
        final ITextComponent comp = new StringTextComponent(RuleManager.format(nick));
        event.setDisplayname(comp);
        if (event.getPlayer() instanceof ServerPlayerEntity && !old.equals(nick))
        {
            final MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            final ServerPlayerEntity player = (ServerPlayerEntity) event.getPlayer();
            server.getPlayerList().sendPacketToAllPlayers(new SPlayerListItemPacket(
                    SPlayerListItemPacket.Action.UPDATE_DISPLAY_NAME, player));
        }
    }

    public static void init()
    {
        MinecraftForge.EVENT_BUS.addListener(NameManager::onPlayerDisplayName);
    }
}