package thut.essentials.itemcontrol;

import java.util.Set;

import com.google.common.collect.Sets;

import net.minecraft.entity.item.ItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.util.Hand;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import thut.essentials.ThutEssentials;
import thut.essentials.util.ConfigManager;

public class ItemControl
{
    public static Set<String> blacklist;

    public static void init()
    {
        MinecraftForge.EVENT_BUS.unregister(ThutEssentials.instance.items);
        blacklist = Sets.newHashSet();
        for (String s : ConfigManager.INSTANCE.itemBlacklist)
            blacklist.add(s);
        MinecraftForge.EVENT_BUS.register(ThutEssentials.instance.items);
    }

    public ItemControl()
    {
    }

    @SubscribeEvent
    public void itemLife(EntityJoinWorldEvent event)
    {
        if (!ConfigManager.INSTANCE.itemLifeTweak || event.getEntity().world.isRemote
                || !(event.getEntity() instanceof ItemEntity))
            return;
        ItemEntity item = (ItemEntity) event.getEntity();
        item.lifespan = ConfigManager.INSTANCE.itemLifeSpan;
    }

    @SubscribeEvent
    public void denyPlayerHeldEvent(LivingUpdateEvent event)
    {
        if (!ConfigManager.INSTANCE.itemControlEnabled || event.getEntity().world.isRemote) return;
        for (Hand hand : Hand.values())
        {
            Item item;
            if (!event.getMobEntity().getHeldItem(hand).isEmpty()
                    && (item = event.getMobEntity().getHeldItem(hand).getItem()) != null)
            {
                String name = item.getRegistryName().toString();
                if (blacklist.contains(name))
                {
                    ItemEntity drop = event.getMobEntity().entityDropItem(event.getMobEntity().getHeldItem(hand),
                            0);
                    drop.setPickupDelay(40);
                    event.getMobEntity().setHeldItem(Hand.MAIN_HAND, ItemStack.EMPTY);
                    event.getMobEntity().attackEntityFrom(DamageSource.GENERIC,
                            (float) ConfigManager.INSTANCE.blacklistDamage);
                    event.getMobEntity().sendMessage(new StringTextComponent("That item is not allowed to be held"));
                }
            }
        }
    }
}
