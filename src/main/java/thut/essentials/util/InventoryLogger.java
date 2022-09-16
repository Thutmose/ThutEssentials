package thut.essentials.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import net.minecraft.world.Container;
import net.minecraft.world.Nameable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerListener;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import thut.essentials.Essentials;
import thut.essentials.land.LandManager.KGobalPos;

public class InventoryLogger
{
    private static class Listener implements ContainerListener
    {
        final Player    player;
        final List<ItemStack> initialList = Lists.newArrayList();

        public Listener(final Player player, final AbstractContainerMenu opened)
        {
            this.player = player;
            this.initialList.addAll(opened.lastSlots);
        }

        @Override
        public void dataChanged(final AbstractContainerMenu p_150524_, final int p_150525_, final int p_150526_)
        {
            // TODO Auto-generated method stub

        }

        @Override
        public void slotChanged(final AbstractContainerMenu containerToSend, final int slotInd, final ItemStack stack)
        {
            try
            {
                // Ensure that the list has room for the slot. This fixes issues
                // with inventories that change size.
                while (slotInd >= this.initialList.size())
                    this.initialList.add(ItemStack.EMPTY);
                final ItemStack oldStack = this.initialList.get(slotInd);
                final Container inventory = containerToSend.getSlot(slotInd).container;
                String invName = inventory.toString();
                if (inventory instanceof Nameable && ((Nameable) inventory).getName() != null)
                    invName = ((Nameable) inventory).getName().getString();
                final KGobalPos c = CoordinateUtls.chunkPos(CoordinateUtls.forMob(this.player));

                if (oldStack.isEmpty() && !stack.isEmpty()) InventoryLogger.log("slot_place {}: {} {}, {} {} {}", c,
                        containerToSend.getClass(), stack, stack.getHoverName().getString(), invName, this.player
                                .getUUID(), this.player.getName().getString());
                else if (stack.isEmpty() && !oldStack.isEmpty()) InventoryLogger.log("slot_take {}: {} {}, {} {} {}", c,
                        containerToSend.getClass(), oldStack, oldStack.getHoverName().getString(), invName,
                        this.player.getUUID(), this.player.getName().getString());
                else InventoryLogger.log("slot_swap {}: {} {} <-> {} {}, {} {} {}", c, containerToSend.getClass(),
                        stack, stack.getHoverName().getString(), oldStack, oldStack.getHoverName().getString(),
                        invName, this.player.getUUID(), this.player.getName().getString());
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
        final KGobalPos c = CoordinateUtls.chunkPos(CoordinateUtls.forMob(event.getEntity()));
        InventoryLogger.log("log-in {} {}", c, event.getEntity().getUUID(), event.getEntity().getName()
                .getString());
    }

    @SubscribeEvent
    public static void PlayerLoggedOutEvent(final PlayerLoggedOutEvent event)
    {
        final KGobalPos c = CoordinateUtls.chunkPos(CoordinateUtls.forMob(event.getEntity()));
        InventoryLogger.log("log-out {} {}", c, event.getEntity().getUUID(), event.getEntity().getName()
                .getString());
    }

    @SubscribeEvent
    public static void openInventory(final PlayerContainerEvent.Open event)
    {
        final KGobalPos c = CoordinateUtls.chunkPos(CoordinateUtls.forMob(event.getEntity()));
        InventoryLogger.log("open {} {} {}", c, event.getContainer().getClass(), event.getEntity().getUUID(), event
                .getEntity().getName().getString());
        if (!InventoryLogger.blacklist.contains(event.getContainer().getClass().getName())) event.getContainer()
                .addSlotListener(new Listener(event.getEntity(), event.getContainer()));
    }

    public static void log(String format, final KGobalPos location, final Object... args)
    {
        final DateTimeFormatter dtf = DateTimeFormatter.ISO_LOCAL_TIME;
        final String header = LocalDateTime.now().format(dtf) + " " + location.getDimension().location() + ", "
                + location.getPos() + ": ";
        format = header + format;
        Essentials.LOGGER.trace(format, args);
    }
}
