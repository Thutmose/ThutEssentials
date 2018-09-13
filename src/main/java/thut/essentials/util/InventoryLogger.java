package thut.essentials.util;

import java.util.List;
import java.util.logging.Level;

import com.google.common.collect.Lists;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IContainerListener;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
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
            ItemStack oldStack = initialList.get(slotInd);
            IInventory inventory = containerToSend.getSlot(slotInd).inventory;

            String invName = inventory.getName();

            if (oldStack.isEmpty() && !stack.isEmpty())
            {
                ThutEssentials.logger.log(Level.FINER,
                        "slot_place " + containerToSend.getClass() + " " + stack + " " + stack.getDisplayName() + ", "
                                + invName + " " + player.getUniqueID() + " " + player.getName());
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
                        "slot_swap " + containerToSend.getClass() + " " + stack + " " + stack.getDisplayName() + " <-> "
                                + oldStack + " " + oldStack.getDisplayName() + ", " + invName + " "
                                + player.getUniqueID() + " " + player.getName());
            }
            initialList.set(slotInd, stack);
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

    public static void enable()
    {
        MinecraftForge.EVENT_BUS.register(InventoryLogger.class);
    }

    public static void disable()
    {
        MinecraftForge.EVENT_BUS.unregister(InventoryLogger.class);
    }

    @SubscribeEvent
    public static void takeFromInventory(PlayerContainerEvent.Open event)
    {
        Coordinate c = Coordinate.getChunkCoordFromWorldCoord(event.getEntityPlayer().getPosition(),
                event.getEntityPlayer().dimension);
        ThutEssentials.logger.log(Level.FINER, c + " open " + event.getContainer().getClass() + " "
                + event.getEntityPlayer().getUniqueID() + " " + event.getEntityPlayer().getName());
        event.getContainer().addListener(new Listener(event.getEntityPlayer(), event.getContainer()));
    }
}
