package thut.essentials.economy;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import net.minecraft.entity.Entity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.item.ItemEntityFrame;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntitySign;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.BlockEvent.BreakEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.server.permission.DefaultPermissionLevel;
import net.minecraftforge.server.permission.PermissionAPI;
import thut.essentials.land.LandSaveHandler;
import thut.essentials.util.CompatWrapper;
import thut.essentials.util.ConfigManager;
import thut.essentials.util.Coordinate;

public class EconomyManager
{
    public static class Account
    {
        int                   balance;
        Set<Shop>             shops    = Sets.newHashSet();
        UUID                  _id;
        Map<Coordinate, Shop> _shopMap = Maps.newHashMap();
    }

    public static class Shop
    {
        Coordinate location;
        Coordinate storage;
        UUID       frameId;
        boolean    infinite  = false;
        boolean    ignoreTag = false;
        boolean    recycle   = false;
        boolean    sell;
        int        cost;
        int        number;

        public boolean transact(PlayerEntity player, ItemStack heldStack, Account shopAccount)
        {
            ItemStack stack = ItemStack.EMPTY;
            Entity ent = player.getServer().getWorld(player.dimension).getEntityFromUuid(frameId);
            if (ent instanceof ItemEntityFrame) stack = ((ItemEntityFrame) ent).getDisplayedItem();
            TileEntity tile = player.world.getTileEntity(new BlockPos(location.x, location.y, location.z));
            if (!(tile instanceof TileEntitySign))
            {
                removeShop(location);
                return false;
            }
            TileEntitySign sign = (TileEntitySign) tile;
            sell = sign.signText[0].getUnformattedText().contains("Sell")
                    || sign.signText[0].getUnformattedText().contains("Sale");
            recycle = sign.signText[0].getUnformattedText().contains("Recycle");
            try
            {
                number = Integer.parseInt(sign.signText[1].getUnformattedText());
                if (ignoreTag)
                {
                    if (number != 1) sign.signText[1] = new StringTextComponent("1");
                    number = 1;
                }
            }
            catch (NumberFormatException e)
            {
                e.printStackTrace();
                return false;
            }
            try
            {
                cost = Integer.parseInt(sign.signText[3].getUnformattedText());
            }
            catch (NumberFormatException e)
            {
                e.printStackTrace();
                return false;
            }

            if (recycle && heldStack.isEmpty())
            {
                player.sendMessage(
                        new StringTextComponent(TextFormatting.RED + "You need to hold the item you want to recycle"));
                return false;
            }
            else if ((stack.isEmpty()))
            {
                removeShop(location);
                return false;
            }
            if (sell)
            {
                int balance = getBalance(player);
                if (balance < cost)
                {
                    player.sendMessage(new StringTextComponent(TextFormatting.RED + "You have Insufficient Funds"));
                    return false;
                }
                stack = stack.copy();
                CompatWrapper.setStackSize(stack, number);
                if (!infinite)
                {
                    int count = 0;
                    IInventory inv = null;
                    ItemStack test2 = stack.copy();
                    if (storage != null)
                    {
                        TileEntity inventory = player.world
                                .getTileEntity(new BlockPos(storage.x, storage.y, storage.z));
                        if (inventory instanceof IInventory)
                        {
                            inv = (IInventory) inventory;
                            if (ignoreTag) test2.setTag(new CompoundNBT());
                            for (int i = 0; i < inv.getSizeInventory(); i++)
                            {
                                ItemStack item = inv.getStackInSlot(i);
                                if (!item.isEmpty())
                                {
                                    ItemStack test = item.copy();
                                    if (ignoreTag) test.setTag(new CompoundNBT());
                                    CompatWrapper.setStackSize(test, number);
                                    if (ItemStack.areItemStacksEqual(test, test2))
                                        count += CompatWrapper.getStackSize(item);
                                }
                            }
                        }
                    }
                    if (count < number || inv == null)
                    {
                        player.sendMessage(new StringTextComponent(TextFormatting.RED + "Shop Has Insufficient Items"));
                        return false;
                    }
                    int i = 0;
                    Item itemIn = test2.getItem();
                    int metadataIn = test2.getItemDamage();
                    int removeCount = number;
                    CompoundNBT itemNBT = ignoreTag ? null : test2.getTag();
                    for (int j = 0; j < inv.getSizeInventory(); ++j)
                    {
                        ItemStack itemstack = inv.getStackInSlot(j);
                        if (!itemstack.isEmpty() && (itemstack.getItem() == itemIn)
                                && (metadataIn <= -1 || itemstack.getMetadata() == metadataIn)
                                && (itemNBT == null || NBTUtil.areNBTEquals(itemNBT, itemstack.getTag(), true)))
                        {

                            int k = removeCount <= 0 ? CompatWrapper.getStackSize(itemstack)
                                    : Math.min(removeCount - i, CompatWrapper.getStackSize(itemstack));
                            i += k;
                            if (number == 1) stack.setTag(itemstack.getTag());
                            if (removeCount != 0)
                            {
                                CompatWrapper.increment(itemstack, -k);
                                if (!CompatWrapper.isValid(itemstack))
                                {
                                    inv.setInventorySlotContents(j, ItemStack.EMPTY);
                                }
                                if (removeCount > 0 && i >= removeCount)
                                {
                                    break;
                                }
                            }
                        }
                    }
                }
                giveItem(player, stack);
                addBalance(shopAccount._id, cost);
                addBalance(player, -cost);
                player.sendMessage(new StringTextComponent(
                        TextFormatting.GREEN + "Remaining Balance: " + TextFormatting.GOLD + getBalance(player)));
            }
            else
            {
                int balance = infinite ? Integer.MAX_VALUE : shopAccount.balance;
                if (balance < cost)
                {
                    player.sendMessage(new StringTextComponent(TextFormatting.RED + "Shop has Insufficient Funds"));
                    return false;
                }
                int count = 0;
                if (recycle)
                {
                    count = CompatWrapper.getStackSize(heldStack);
                    if (count < number)
                    {
                        player.sendMessage(new StringTextComponent(TextFormatting.RED + "You have Insufficient Items"));
                        return false;
                    }
                    stack = heldStack;
                }
                else
                {
                    stack = stack.copy();
                    CompatWrapper.setStackSize(stack, number);
                    for (ItemStack item : player.inventory.mainInventory)
                    {
                        if (!item.isEmpty())
                        {
                            ItemStack test = item.copy();
                            CompatWrapper.setStackSize(test, number);
                            if (ItemStack.areItemStacksEqual(test, stack)) count += CompatWrapper.getStackSize(item);
                        }
                    }
                    if (count < number)
                    {
                        player.sendMessage(new StringTextComponent(TextFormatting.RED + "You have Insufficient Items"));
                        return false;
                    }
                }
                if (!infinite)
                {
                    if (storage == null)
                    {
                        player.sendMessage(new StringTextComponent(TextFormatting.RED + "No Storage"));
                        return false;
                    }
                    TileEntity te = player.world.getTileEntity(new BlockPos(storage.x, storage.y, storage.z));
                    if (te instanceof IInventory)
                    {
                        IInventory inv = (IInventory) te;
                        count = 0;
                        ItemStack a = stack;
                        count = CompatWrapper.getStackSize(a);
                        for (int i = 0; i < inv.getSizeInventory(); i++)
                        {
                            if (!CompatWrapper.isValid(inv.getStackInSlot(i)) || a.isItemEqual(inv.getStackInSlot(i)))
                            {
                                int n = 0;
                                if (CompatWrapper.isValid(inv.getStackInSlot(i))
                                        && (n = CompatWrapper.getStackSize(inv.getStackInSlot(i))
                                                + CompatWrapper.getStackSize(a)) < 65)
                                {
                                    CompatWrapper.setStackSize(a, n);
                                    count = 0;
                                    inv.setInventorySlotContents(i, a.copy());
                                }
                                else if (!CompatWrapper.isValid(inv.getStackInSlot(i)))
                                {
                                    count = 0;
                                    inv.setInventorySlotContents(i, a.copy());
                                }
                            }
                            if (count == 0) break;
                        }
                    }
                }
                player.inventory.clearMatchingItems(stack.getItem(), stack.getItemDamage(), number,
                        stack.getTag());
                addBalance(shopAccount._id, -cost);
                addBalance(player, cost);
                player.sendMessage(new StringTextComponent(
                        TextFormatting.GREEN + "Remaining Balance: " + TextFormatting.GOLD + getBalance(player)));
            }
            return false;
        }
    }

    public static final int         VERSION           = 1;

    public static final String      PERMMAKESHOP      = "thutessentials.economy.make_shop";
    public static final String      PERMMAKEINFSHOP   = "thutessentials.economy.make_infinite_shop";

    public static final String      PERMKILLSHOP      = "thutessentials.economy.kill_shop";
    public static final String      PERMKILLSHOPOTHER = "thutessentials.economy.kill_shop_other";

    public static final UUID        DEFAULT_ID        = new UUID(0, 0);

    public static EconomyManager    instance;
    private static boolean          init              = false;
    public int                      version           = VERSION;
    public int                      initial           = 1000;
    public Map<UUID, Account>       bank              = Maps.newHashMap();
    public Map<Coordinate, Account> _shopMap          = Maps.newHashMap();
    public Map<Account, UUID>       _revBank          = Maps.newHashMap();

    public static void clearInstance()
    {
        if (instance != null)
        {
            LandSaveHandler.saveGlobalData();
            MinecraftForge.EVENT_BUS.unregister(instance);
        }
        instance = null;
    }

    public static EconomyManager getInstance()
    {
        if (instance == null)
        {
            EconomySaveHandler.loadGlobalData();
            if (!init)
            {
                init = true;
                PermissionAPI.registerNode(PERMMAKESHOP, DefaultPermissionLevel.ALL,
                        "Allowed to make a shop that sells from a chest.");
                PermissionAPI.registerNode(PERMMAKEINFSHOP, DefaultPermissionLevel.OP,
                        "Allowed to make a shop that sells infinite items.");

                PermissionAPI.registerNode(PERMKILLSHOP, DefaultPermissionLevel.ALL,
                        "Allowed to remove a shop made by this player.");
                PermissionAPI.registerNode(PERMKILLSHOPOTHER, DefaultPermissionLevel.OP,
                        "Allowed to remove a shop made by another player.");
            }
        }
        return instance;
    }

    public EconomyManager()
    {
        MinecraftForge.EVENT_BUS.register(this);
        Account master = new Account();
        master.balance = Integer.MAX_VALUE;
        bank.put(DEFAULT_ID, master);
    }

    @SubscribeEvent(receiveCanceled = true)
    public void interactRightClickEntity(PlayerInteractEvent.EntityInteract evt)
    {
        if (evt.getWorld().isRemote) return;
        if (!ConfigManager.INSTANCE.shopsEnabled) return;
        if (evt.getTarget() instanceof ItemEntityFrame)
        {
            Coordinate c = new Coordinate(evt.getPos().down(), evt.getPlayerEntity().dimension);
            Shop shop = getShop(c);
            TileEntity tile = evt.getWorld().getTileEntity(new BlockPos(c.x, c.y, c.z));
            if (evt.getItemStack() != null && tile instanceof TileEntitySign && shop == null
                    && (evt.getItemStack().getDisplayName().contains("Shop")
                            || evt.getItemStack().getDisplayName().contains("InfShop")))
            {
                boolean infinite = evt.getItemStack().getDisplayName().contains("InfShop");
                String permission = infinite ? PERMMAKEINFSHOP : PERMMAKESHOP;
                if (!PermissionAPI.hasPermission(evt.getPlayerEntity(), permission))
                {
                    evt.getPlayerEntity().sendMessage(
                            new StringTextComponent(TextFormatting.RED + "You are not allowed to make that shop."));
                    return;
                }
                try
                {
                    boolean noTag = evt.getItemStack().getDisplayName().contains("noTag");
                    shop = addShop(evt.getPlayerEntity(), (ItemEntityFrame) evt.getTarget(), c, infinite, noTag);
                    evt.getPlayerEntity().sendMessage(
                            new StringTextComponent(TextFormatting.GREEN + "Successfully created the shop. "));

                }
                catch (Exception e)
                {
                    evt.getPlayerEntity()
                            .sendMessage(new StringTextComponent(TextFormatting.RED + "Error making shop. " + e));
                }
            }
            if (shop != null)
            {
                evt.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public void projectileImpact(ProjectileImpactEvent evt)
    {
        if (evt.getEntity().getEntityWorld().isRemote) return;
        if (!ConfigManager.INSTANCE.shopsEnabled) return;
        if (evt.getRayTraceResult() == null) return;
        if (evt.getRayTraceResult().entityHit == null) return;
        Entity target = evt.getRayTraceResult().entityHit;
        if (target instanceof ItemEntityFrame)
        {
            Coordinate c = new Coordinate(target.getPosition().down(2), target.dimension);
            Shop shop = getShop(c);
            if (shop != null)
            {
                evt.setCanceled(true);
            }
        }
    }

    @SubscribeEvent(receiveCanceled = true)
    public void interactLeftClickEntity(AttackEntityEvent evt)
    {
        if (evt.getPlayerEntity().getEntityWorld().isRemote) return;
        if (!ConfigManager.INSTANCE.shopsEnabled) return;
        if (evt.getTarget() instanceof ItemEntityFrame)
        {
            Coordinate c = new Coordinate(evt.getTarget().getPosition().down(2), evt.getTarget().dimension);
            Shop shop = getShop(c);
            if (shop != null)
            {
                evt.setCanceled(true);
                Account account = _shopMap.get(c);
                UUID owner = _revBank.get(account);
                String perm = evt.getPlayerEntity().getUniqueID().equals(owner) ? PERMKILLSHOP : PERMKILLSHOPOTHER;
                if (PermissionAPI.hasPermission(evt.getPlayerEntity(), perm))
                {
                    removeShop(c);
                    evt.getPlayerEntity()
                            .sendMessage(new StringTextComponent(TextFormatting.GREEN + "Removed the shop."));
                }
                else
                {
                    evt.getPlayerEntity()
                            .sendMessage(new StringTextComponent(TextFormatting.RED + "Cannot remove the shop."));
                }
            }
        }
    }

    /** Uses player interact here to also prevent opening of inventories.
     * 
     * @param evt */
    @SubscribeEvent(receiveCanceled = true)
    public void interactRightClickBlock(PlayerInteractEvent.RightClickBlock evt)
    {
        if (evt.getWorld().isRemote || !ConfigManager.INSTANCE.shopsEnabled) return;
        Coordinate c = new Coordinate(evt.getPos(), evt.getPlayerEntity().dimension);
        Shop shop = getShop(c);
        if (shop != null)
        {
            shop.transact(evt.getPlayerEntity(), evt.getItemStack(), _shopMap.get(c));
        }
    }

    public Account getAccount(UUID player)
    {
        Account account = bank.get(player);
        if (account == null)
        {
            bank.put(player, account = new Account());
            account._id = player;
            account.balance = initial;
            EconomySaveHandler.saveGlobalData();
        }
        return account;
    }

    public Account getAccount(PlayerEntity player)
    {
        return getAccount(player.getUniqueID());
    }

    public static Shop addShop(PlayerEntity owner, ItemEntityFrame frame, Coordinate location, boolean infinite,
            boolean noTag)
    {
        Shop shop = new Shop();
        shop.infinite = infinite;
        shop.frameId = frame.getUniqueID();
        shop.location = location;
        shop.ignoreTag = noTag;
        // Assign the infinite shops to the default id account.
        Account account = getInstance().getAccount(infinite ? DEFAULT_ID : owner.getUniqueID());
        account.shops.add(shop);
        account._shopMap.put(location, shop);
        getInstance()._shopMap.put(location, account);
        if (!shop.infinite)
        {
            TileEntity down = owner.getEntityWorld()
                    .getTileEntity(new BlockPos(location.x, location.y - 1, location.z));
            if (down instanceof TileEntitySign)
            {
                String[] var = ((TileEntitySign) down).signText[0].getUnformattedText().split(",");
                int dx = Integer.parseInt(var[0]);
                int dy = Integer.parseInt(var[1]);
                int dz = Integer.parseInt(var[2]);

                BlockPos pos = new BlockPos(location.x + dx, location.y + dy, location.z + dz);
                BreakEvent event = new BreakEvent(owner.getEntityWorld(), pos,
                        owner.getEntityWorld().getBlockState(pos), owner);
                MinecraftForge.EVENT_BUS.post(event);
                if (event.isCanceled())
                {
                    owner.sendMessage(new StringTextComponent(
                            TextFormatting.RED + "You may not link that inventory to the shop!"));
                    return null;
                }

                shop.storage = new Coordinate(location.x + dx, location.y + dy, location.z + dz, location.dim);
            }
            else shop.storage = new Coordinate(location.x, location.y - 1, location.z, location.dim);
        }
        EconomySaveHandler.saveGlobalData();
        return shop;
    }

    public static void removeShop(Coordinate location)
    {
        Account account = getInstance()._shopMap.remove(location);
        if (account != null)
        {
            account.shops.remove(account._shopMap.remove(location));
            EconomySaveHandler.saveGlobalData();
        }
    }

    public static Shop getShop(Coordinate location)
    {
        Account account = getInstance()._shopMap.get(location);
        if (account == null) return null;
        return account._shopMap.get(location);
    }

    public static int getBalance(PlayerEntity player)
    {
        return getBalance(player.getUniqueID());
    }

    public static void setBalance(PlayerEntity player, int amount)
    {
        setBalance(player.getUniqueID(), amount);
    }

    public static void addBalance(PlayerEntity player, int amount)
    {
        addBalance(player.getUniqueID(), amount);
    }

    public static int getBalance(UUID player)
    {
        return getInstance().getAccount(player).balance;
    }

    public static void setBalance(UUID player, int amount)
    {
        Account account = getInstance().getAccount(player);
        account.balance = amount;
        EconomySaveHandler.saveGlobalData();
    }

    public static void addBalance(UUID player, int amount)
    {
        Account account = getInstance().getAccount(player);
        account.balance += amount;
        EconomySaveHandler.saveGlobalData();
    }

    public static void giveItem(PlayerEntity PlayerEntity, ItemStack itemstack)
    {
        boolean flag = PlayerEntity.inventory.addItemStackToInventory(itemstack);
        if (flag)
        {
            PlayerEntity.world.playSound((PlayerEntity) null, PlayerEntity.posX, PlayerEntity.posY, PlayerEntity.posZ,
                    SoundEvents.ENTITY_ITEM_PICKUP, SoundCategory.PLAYERS, 0.2F,
                    ((PlayerEntity.getRNG().nextFloat() - PlayerEntity.getRNG().nextFloat()) * 0.7F + 1.0F) * 2.0F);
            PlayerEntity.inventoryContainer.detectAndSendChanges();
        }
        else
        {
            ItemEntity ItemEntity = PlayerEntity.dropItem(itemstack, false);
            if (ItemEntity != null)
            {
                ItemEntity.setNoPickupDelay();
                ItemEntity.setOwner(PlayerEntity.getName());
            }
        }
    }
}
