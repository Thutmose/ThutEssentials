package thut.essentials.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent.NameFormat;
import net.minecraftforge.fmllegacy.server.ServerLifecycleHooks;

public class NameManager
{

    private static void onPlayerDisplayName(final NameFormat event)
    {
        final CompoundTag tag = PlayerDataHandler.getCustomDataTag(event.getPlayer());
        String nick = tag.getString("nick");
        final String pref = tag.getString("nick_pref");
        final String suff = tag.getString("nick_suff");
        if (nick.trim().isEmpty() && pref.trim().isEmpty() && suff.trim().isEmpty()) return;
        final String old = event.getDisplayname().getString();
        if (nick.trim().isEmpty()) nick = old;
        nick = pref + nick + suff;
        if (nick.length() > 16) nick = nick.substring(0, 16);
        final Component comp = new TextComponent(RuleManager.format(nick));
        event.setDisplayname(comp);
        if (event.getPlayer() instanceof ServerPlayer && !old.equals(nick))
        {
            final MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            final ServerPlayer player = (ServerPlayer) event.getPlayer();
            server.getPlayerList().broadcastAll(new ClientboundPlayerInfoPacket(
                    ClientboundPlayerInfoPacket.Action.UPDATE_DISPLAY_NAME, player));
        }
    }

    public static void init()
    {
        MinecraftForge.EVENT_BUS.addListener(NameManager::onPlayerDisplayName);
    }
}