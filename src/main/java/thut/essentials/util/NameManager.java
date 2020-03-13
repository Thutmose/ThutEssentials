package thut.essentials.util;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import com.mojang.authlib.GameProfile;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.play.server.SPlayerListItemPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.dedicated.DedicatedServer;
import thut.essentials.Essentials;

public class NameManager
{

    private static Field NAME = null;

    static
    {
        try
        {
            NameManager.NAME = GameProfile.class.getDeclaredFields()[1];
            NameManager.NAME.setAccessible(true);
            final Field modifiersField = Field.class.getDeclaredField("modifiers");
            modifiersField.setAccessible(true);
            modifiersField.setInt(NameManager.NAME, NameManager.NAME.getModifiers() & ~Modifier.FINAL);
        }
        catch (SecurityException | NoSuchFieldException | IllegalArgumentException | IllegalAccessException e)
        {
            NameManager.NAME = null;
        }
    }

    private static void setName(final String name, final GameProfile profile)
    {
        try
        {
            NameManager.NAME.set(profile, name);
        }
        catch (IllegalArgumentException | IllegalAccessException e)
        {
            Essentials.LOGGER.warn("Error setting name", e);
        }
    }

    public static void init(final MinecraftServer server)
    {
        if (!(server instanceof DedicatedServer)) return;
        // server.setPlayerList(new NameManager((DedicatedServer) server));
    }

    public static void onLogin(final ServerPlayerEntity player, final MinecraftServer server)
    {
        final String name = PlayerDataHandler.getCustomDataTag(player).getString("nick");
        if (!name.isEmpty()) NameManager.setName(name, player.getGameProfile(), server);
    }

    public static void setName(final String name, final GameProfile profile, final MinecraftServer server)
    {
        if (NameManager.NAME == null)
        {
            Essentials.LOGGER.warn("Setting custom name currently disabled.");
            return;
        }
        final GameProfile newProfile = new GameProfile(profile.getId(), name);
        server.getPlayerProfileCache().addEntry(newProfile);
        final ServerPlayerEntity player = server.getPlayerList().getPlayerByUUID(profile.getId());
        // This is null on login
        if (player != null)
        {
            NameManager.setName(name, player.getGameProfile());
            PlayerDataHandler.getCustomDataTag(player).putString("nick", name);
            server.getPlayerList().sendPacketToAllPlayers(new SPlayerListItemPacket(
                    SPlayerListItemPacket.Action.UPDATE_DISPLAY_NAME, player));
        }
        else NameManager.setName(name, profile);
    }
}
