package thut.essentials.util;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.IContainerListener;
import net.minecraft.item.ItemStack;
import net.minecraft.util.INameable;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.GlobalPos;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import thut.essentials.Essentials;

public class InventoryLogger
{
    private static class Listener implements IContainerListener
    {
        final PlayerEntity    player;
        final List<ItemStack> initialList = Lists.newArrayList();

        public Listener(final PlayerEntity player, final Container opened)
        {
            this.player = player;
            this.initialList.addAll(opened.inventoryItemStacks);
        }

        @Override
        public void sendAllContents(final Container containerToSend, final NonNullList<ItemStack> itemsList)
        {
        }

        @Override
        public void sendSlotContents(final Container containerToSend, final int slotInd, final ItemStack stack)
        {
            try
            {
                // Ensure that the list has room for the slot. This fixes issues
                // with inventories that change size.
                while (slotInd >= this.initialList.size())
                    this.initialList.add(ItemStack.EMPTY);
                final ItemStack oldStack = this.initialList.get(slotInd);
                final IInventory inventory = containerToSend.getSlot(slotInd).inventory;
                String invName = inventory.toString();
                if (inventory instanceof INameable && ((INameable) inventory).getName() != null)
                    invName = ((INameable) inventory).getName().getString();
                if (oldStack.isEmpty() && !stack.isEmpty()) Essentials.LOGGER.trace("slot_place " + containerToSend
                        .getClass() + " " + stack + " " + stack.getDisplayName().getString() + ", " + invName + " "
                        + this.player.getUniqueID() + " " + this.player.getName().getString());
                else if (stack.isEmpty() && !oldStack.isEmpty()) Essentials.LOGGER.trace("slot_take " + containerToSend
                        .getClass() + " " + oldStack + " " + oldStack.getDisplayName().getString() + ", "
                        + invName
                        + " " + this.player.getUniqueID() + " " + this.player.getName().getString());
                else Essentials.LOGGER.trace("slot_swap " + containerToSend.getClass() + " " + stack + " " + stack
                        .getDisplayName() + " <-> " + oldStack + " " + oldStack.getDisplayName().getString()
                        + ", "
                        + invName + " " + this.player.getUniqueID() + " " + this.player.getName().getString());
                this.initialList.set(slotInd, stack);
            }
            catch (final Exception e)
            {
                Essentials.LOGGER.error("Blacklisting Errored Inventory:" + containerToSend.getClass(), e);
                InventoryLogger.blacklist.add(containerToSend.getClass().getName());
                final List<String> temp = Lists.newArrayList(InventoryLogger.blacklist);
                Collections.sort(temp);
                Essentials.config.inventory_log_blacklist = temp;
            }
        }

        @Override
        public void sendWindowProperty(final Container containerIn, final int varToUpdate, final int newValue)
        {
        }

    }

    private static Set<String> blacklist = Sets.newHashSet();

    public static void enable()
    {
        InventoryLogger.blacklist = Sets.newHashSet(Essentials.config.inventory_log_blacklist);
        MinecraftForge.EVENT_BUS.register(InventoryLogger.class);
    }

    public static void disable()
    {
        InventoryLogger.blacklist.clear();
        MinecraftForge.EVENT_BUS.unregister(InventoryLogger.class);
    }

    @SubscribeEvent
    public static void PlayerLoggedInEvent(final PlayerLoggedInEvent event)
    {
        final GlobalPos c = CoordinateUtls.chunkPos(CoordinateUtls.forMob(event.getPlayer()));
        Essentials.LOGGER.trace(c + " log-in " + event.getPlayer().getUniqueID() + " " + event.getPlayer().getName()
                .getString());
    }

    @SubscribeEvent
    public static void PlayerLoggedOutEvent(final PlayerLoggedOutEvent event)
    {
        final GlobalPos c = CoordinateUtls.chunkPos(CoordinateUtls.forMob(event.getPlayer()));
        Essentials.LOGGER.trace(c + " log-out " + event.getPlayer().getUniqueID() + " " + event.getPlayer().getName()
                .getString());
    }

    @SubscribeEvent
    public static void openInventory(final PlayerContainerEvent.Open event)
    {
        final GlobalPos c = CoordinateUtls.chunkPos(CoordinateUtls.forMob(event.getPlayer()));
        Essentials.LOGGER.trace(c + " open " + event.getContainer().getClass() + " " + event.getPlayer().getUniqueID()
                + " " + event.getPlayer().getName().getString());
        if (!InventoryLogger.blacklist.contains(event.getContainer().getClass().getName())) event.getContainer()
                .addListener(new Listener(event.getPlayer(), event.getContainer()));
    }
}
