package thut.essentials.util;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import thut.essentials.events.NameEvent;
import thut.essentials.land.LandManager;
import thut.essentials.land.LandManager.LandTeam;
import thut.essentials.land.LandManager.LandTeam.Rank;

public class ChatHandler
{

    @SubscribeEvent
    public void chatEvent(ServerChatEvent event)
    {
        if (!ConfigManager.INSTANCE.forceChatFormat) return;
        String format = ConfigManager.INSTANCE.chatFormat;
        String message = format.replace("[name]", event.getUsername());
        message = message.replaceAll("[message]", event.getMessage());
        event.setComponent(new TextComponentString(message));
    }

    @SubscribeEvent
    public void getDisplayNameEvent(PlayerEvent.NameFormat event)
    {
        String displayName = event.getDisplayname();
        NBTTagCompound tag = PlayerDataHandler.getCustomDataTag(event.getEntityPlayer());
        NBTTagCompound nametag = tag.getCompoundTag("name");
        if (nametag.hasKey("name") && ConfigManager.INSTANCE.name)
        {
            displayName = nametag.getString("name");
        }
        if (nametag.hasKey("prefix") && ConfigManager.INSTANCE.prefix)
        {
            if (!nametag.getString("prefix").trim().isEmpty())
                displayName = nametag.getString("prefix") + " " + displayName;
        }
        if (nametag.hasKey("suffix") && ConfigManager.INSTANCE.suffix)
        {
            if (!nametag.getString("suffix").trim().isEmpty())
                displayName = displayName + " " + nametag.getString("suffix");
        }
        if (ConfigManager.INSTANCE.landEnabled)
        {
            LandTeam team = LandManager.getTeam(event.getEntity());
            Rank rank = team.ranksMembers.get(event.getEntity().getUniqueID());
            if (rank != null)
            {
                String rankPrefix = rank.prefix;
                if (rankPrefix != null)
                {
                    displayName = rankPrefix + TextFormatting.RESET + " " + displayName;
                }
            }
            if (!team.prefix.isEmpty()) displayName = team.prefix + TextFormatting.RESET + " " + displayName;
        }
        NameEvent event1 = new NameEvent(event.getEntityPlayer(), displayName);
        MinecraftForge.EVENT_BUS.post(event1);
        event.setDisplayname(event1.getName());
    }
}
