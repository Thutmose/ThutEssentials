package thut.essentials.util;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.container.IContainerListener;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedOutEvent;
import thut.essentials.ThutEssentials;

public class InventoryLogger
{
    private static class Listener implements IContainerListener
    {
        final EntityPlayer    player;
        final List<ItemStack> initialList = Lists.newArrayList();

        public Listener(EntityPlayer player, Container opened)
        {
            this.player = player;
            initialList.addAll(opened.inventoryItemStacks);
        }

        @Override
        public void sendAllContents(Container containerToSend, NonNullList<ItemStack> itemsList)
        {
        }

        @Override
        public void sendSlotContents(Container containerToSend, int slotInd, ItemStack stack)
        {
            try
            {
                // Ensure that the list has room for the slot. This fixes issues
                // with inventories that change size.
                while (slotInd >= initialList.size())
                {
                    initialList.add(ItemStack.EMPTY);
                }

                ItemStack oldStack = initialList.get(slotInd);
                IInventory inventory = containerToSend.getSlot(slotInd).inventory;

                String invName = inventory.getName();

                if (oldStack.isEmpty() && !stack.isEmpty())
                {
                    ThutEssentials.logger.log(Level.FINER,
                            "slot_place " + containerToSend.getClass() + " " + stack + " " + stack.getDisplayName()
                                    + ", " + invName + " " + player.getUniqueID() + " " + player.getName());
                }
                else if (stack.isEmpty() && !oldStack.isEmpty())
                {
                    ThutEssentials.logger.log(Level.FINER,
                            "slot_take " + containerToSend.getClass() + " " + oldStack + " " + oldStack.getDisplayName()
                                    + ", " + invName + " " + player.getUniqueID() + " " + player.getName());
                }
                else
                {
                    ThutEssentials.logger.log(Level.FINER,
                            "slot_swap " + containerToSend.getClass() + " " + stack + " " + stack.getDisplayName()
                                    + " <-> " + oldStack + " " + oldStack.getDisplayName() + ", " + invName + " "
                                    + player.getUniqueID() + " " + player.getName());
                }
                initialList.set(slotInd, stack);
            }
            catch (Exception e)
            {
                ThutEssentials.logger.log(Level.SEVERE, "Blacklisting Errored Inventory:" + containerToSend.getClass(),
                        e);
                blacklist.add(containerToSend.getClass().getName());
                List<String> temp = Lists.newArrayList(blacklist);
                Collections.sort(temp);
                ConfigManager.INSTANCE.inventory_log_blacklist = temp.toArray(new String[0]);
            }
        }

        @Override
        public void sendWindowProperty(Container containerIn, int varToUpdate, int newValue)
        {
        }

        @Override
        public void sendAllWindowProperties(Container containerIn, IInventory inventory)
        {
        }

    }

    private static Set<String> blacklist = Sets.newHashSet();

    public static void enable()
    {
        blacklist = Sets.newHashSet(ConfigManager.INSTANCE.inventory_log_blacklist);
        MinecraftForge.EVENT_BUS.register(InventoryLogger.class);
    }

    public static void disable()
    {
        blacklist.clear();
        MinecraftForge.EVENT_BUS.unregister(InventoryLogger.class);
    }

    @SubscribeEvent
    public void PlayerLoggedInEvent(PlayerLoggedInEvent event)
    {
        Coordinate c = Coordinate.getChunkCoordFromWorldCoord(event.player.getPosition(), event.player.dimension);
        ThutEssentials.logger.log(Level.FINER,
                c + " log-in " + event.player.getUniqueID() + " " + event.player.getName());
    }

    @SubscribeEvent
    public void PlayerLoggedOutEvent(PlayerLoggedOutEvent event)
    {
        Coordinate c = Coordinate.getChunkCoordFromWorldCoord(event.player.getPosition(), event.player.dimension);
        ThutEssentials.logger.log(Level.FINER,
                c + " log-out " + event.player.getUniqueID() + " " + event.player.getName());
    }

    @SubscribeEvent
    public static void openInventory(PlayerContainerEvent.Open event)
    {
        Coordinate c = Coordinate.getChunkCoordFromWorldCoord(event.getEntityPlayer().getPosition(),
                event.getEntityPlayer().dimension);
        ThutEssentials.logger.log(Level.FINER, c + " open " + event.getContainer().getClass() + " "
                + event.getEntityPlayer().getUniqueID() + " " + event.getEntityPlayer().getName());
        if (!blacklist.contains(event.getContainer().getClass().getName()))
            event.getContainer().addListener(new Listener(event.getEntityPlayer(), event.getContainer()));
    }
}
