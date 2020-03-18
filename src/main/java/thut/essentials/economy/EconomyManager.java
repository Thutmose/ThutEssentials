package thut.essentials.economy;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import net.minecraft.entity.Entity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.item.ItemFrameEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.tileentity.SignTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.EntityRayTraceResult;
import net.minecraft.util.math.RayTraceResult.Type;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.BlockEvent.BreakEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.server.permission.DefaultPermissionLevel;
import net.minecraftforge.server.permission.PermissionAPI;
import thut.essentials.Essentials;
import thut.essentials.commands.CommandManager;
import thut.essentials.land.LandSaveHandler;
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

        public boolean transact(final ServerPlayerEntity player, final ItemStack heldStack, final Account shopAccount)
        {
            ItemStack stack = ItemStack.EMPTY;
            final Entity ent = player.getServer().getWorld(player.dimension).getEntityByUuid(this.frameId);
            if (ent instanceof ItemFrameEntity) stack = ((ItemFrameEntity) ent).getDisplayedItem();
            final TileEntity tile = player.world.getTileEntity(new BlockPos(this.location.x, this.location.y,
                    this.location.z));
            if (!(tile instanceof SignTileEntity))
            {
                EconomyManager.removeShop(this.location);
                return false;
            }
            final SignTileEntity sign = (SignTileEntity) tile;
            this.sell = sign.signText[0].getUnformattedComponentText().contains("Sell") || sign.signText[0]
                    .getUnformattedComponentText().contains("Sale");
            this.recycle = sign.signText[0].getUnformattedComponentText().contains("Recycle");
            try
            {
                this.number = Integer.parseInt(sign.signText[1].getUnformattedComponentText());
                if (this.ignoreTag)
                {
                    if (this.number != 1) sign.signText[1] = new StringTextComponent("1");
                    this.number = 1;
                }
            }
            catch (final NumberFormatException e)
            {
                e.printStackTrace();
                return false;
            }
            try
            {
                this.cost = Integer.parseInt(sign.signText[3].getUnformattedComponentText());
            }
            catch (final NumberFormatException e)
            {
                e.printStackTrace();
                return false;
            }

            if (this.recycle && heldStack.isEmpty())
            {
                player.sendMessage(CommandManager.makeFormattedComponent("thutessentials.econ.no_recycle"));
                return false;
            }
            else if (stack.isEmpty())
            {
                EconomyManager.removeShop(this.location);
                return false;
            }
            if (this.sell)
            {
                final int balance = EconomyManager.getBalance(player);
                if (balance < this.cost)
                {
                    player.sendMessage(CommandManager.makeFormattedComponent("thutessentials.econ.no_funds_you"));
                    return false;
                }
                stack = stack.copy();
                stack.setCount(this.number);
                if (!this.infinite)
                {
                    int count = 0;
                    IInventory inv = null;
                    final ItemStack test2 = stack.copy();
                    if (this.storage != null)
                    {
                        final TileEntity inventory = player.world.getTileEntity(new BlockPos(this.storage.x,
                                this.storage.y, this.storage.z));
                        if (inventory instanceof IInventory)
                        {
                            inv = (IInventory) inventory;
                            if (this.ignoreTag) test2.setTag(new CompoundNBT());
                            for (int i = 0; i < inv.getSizeInventory(); i++)
                            {
                                final ItemStack item = inv.getStackInSlot(i);
                                if (!item.isEmpty())
                                {
                                    final ItemStack test = item.copy();
                                    if (this.ignoreTag) test.setTag(new CompoundNBT());
                                    test.setCount(this.number);
                                    if (ItemStack.areItemStacksEqual(test, test2)) count += item.getCount();
                                }
                            }
                        }
                    }
                    if (count < this.number || inv == null)
                    {
                        System.out.println(this.number + " " + count + " " + this.storage);
                        player.sendMessage(CommandManager.makeFormattedComponent("thutessentials.econ.no_items_shop"));
                        return false;
                    }
                    int i = 0;
                    final Item itemIn = test2.getItem();
                    final int removeCount = this.number;
                    final CompoundNBT itemNBT = this.ignoreTag ? null : test2.getTag();
                    for (int j = 0; j < inv.getSizeInventory(); ++j)
                    {
                        final ItemStack itemstack = inv.getStackInSlot(j);
                        if (!itemstack.isEmpty() && itemstack.getItem() == itemIn && (itemNBT == null || NBTUtil
                                .areNBTEquals(itemNBT, itemstack.getTag(), true)))
                        {

                            final int k = removeCount <= 0 ? itemstack.getCount()
                                    : Math.min(removeCount - i, itemstack.getCount());
                            i += k;
                            if (this.number == 1) stack.setTag(itemstack.getTag());
                            if (removeCount != 0)
                            {
                                itemstack.shrink(k);
                                if (itemstack.isEmpty()) inv.setInventorySlotContents(j, ItemStack.EMPTY);
                                if (removeCount > 0 && i >= removeCount) break;
                            }
                        }
                    }
                }
                EconomyManager.giveItem(player, stack);
                EconomyManager.addBalance(shopAccount._id, this.cost);
                EconomyManager.addBalance(player, -this.cost);
                player.sendMessage(CommandManager.makeFormattedComponent("thutessentials.econ.balance.remaining", null,
                        false, EconomyManager.getBalance(player)));
            }
            else
            {
                final int balance = this.infinite ? Integer.MAX_VALUE : shopAccount.balance;
                if (balance < this.cost)
                {
                    player.sendMessage(CommandManager.makeFormattedComponent("thutessentials.econ.no_funds_shop"));
                    return false;
                }
                int count = 0;
                if (this.recycle)
                {
                    count = heldStack.getCount();
                    if (count < this.number)
                    {
                        player.sendMessage(CommandManager.makeFormattedComponent("thutessentials.econ.no_items_you"));
                        return false;
                    }
                    stack = heldStack;
                }
                else
                {
                    stack = stack.copy();
                    stack.setCount(this.number);
                    for (final ItemStack item : player.inventory.mainInventory)
                        if (!item.isEmpty())
                        {
                            final ItemStack test = item.copy();
                            test.setCount(this.number);
                            if (ItemStack.areItemStacksEqual(test, stack)) count += item.getCount();
                        }
                    if (count < this.number)
                    {
                        player.sendMessage(CommandManager.makeFormattedComponent("thutessentials.econ.no_items_you"));
                        return false;
                    }
                }
                if (!this.infinite)
                {
                    if (this.storage == null)
                    {
                        player.sendMessage(CommandManager.makeFormattedComponent("thutessentials.econ.no_storage"));
                        return false;
                    }
                    final TileEntity te = player.world.getTileEntity(new BlockPos(this.storage.x, this.storage.y,
                            this.storage.z));
                    if (te instanceof IInventory)
                    {
                        final IInventory inv = (IInventory) te;
                        count = 0;
                        final ItemStack a = stack;
                        count = stack.getCount();
                        for (int i = 0; i < inv.getSizeInventory(); i++)
                        {
                            if (inv.getStackInSlot(i).isEmpty() || a.isItemEqual(inv.getStackInSlot(i)))
                            {
                                int n = 0;
                                if (!inv.getStackInSlot(i).isEmpty() && (n = inv.getStackInSlot(i).getCount() + a
                                        .getCount()) < 65)
                                {
                                    a.setCount(n);
                                    count = 0;
                                    inv.setInventorySlotContents(i, a.copy());
                                }
                                else if (inv.getStackInSlot(i).isEmpty())
                                {
                                    count = 0;
                                    inv.setInventorySlotContents(i, a.copy());
                                }
                            }
                            if (count == 0) break;
                        }
                    }
                }
                final ItemStack comp = stack;
                player.inventory.clearMatchingItems((i) -> ItemStack.areItemStacksEqual(i, comp), this.number);
                EconomyManager.addBalance(shopAccount._id, -this.cost);
                EconomyManager.addBalance(player, this.cost);
                player.sendMessage(CommandManager.makeFormattedComponent("thutessentials.econ.balance.remaining", null,
                        false, EconomyManager.getBalance(player)));
            }
            return false;
        }
    }

    public static final int VERSION = 1;

    public static final String PERMMAKESHOP    = "thutessentials.economy.make_shop";
    public static final String PERMMAKEINFSHOP = "thutessentials.economy.make_infinite_shop";

    public static final String PERMKILLSHOP      = "thutessentials.economy.kill_shop";
    public static final String PERMKILLSHOPOTHER = "thutessentials.economy.kill_shop_other";

    public static final UUID DEFAULT_ID = new UUID(0, 0);

    public static EconomyManager    instance;
    private static boolean          init     = false;
    public int                      version  = EconomyManager.VERSION;
    public int                      initial  = 1000;
    public Map<UUID, Account>       bank     = Maps.newHashMap();
    public Map<Coordinate, Account> _shopMap = Maps.newHashMap();
    public Map<Account, UUID>       _revBank = Maps.newHashMap();

    public static void clearInstance()
    {
        if (EconomyManager.instance != null)
        {
            LandSaveHandler.saveGlobalData();
            MinecraftForge.EVENT_BUS.unregister(EconomyManager.instance);
        }
        EconomyManager.instance = null;
    }

    public static EconomyManager getInstance()
    {
        if (EconomyManager.instance == null)
        {
            EconomySaveHandler.loadGlobalData();
            if (!EconomyManager.init)
            {
                EconomyManager.init = true;
                PermissionAPI.registerNode(EconomyManager.PERMMAKESHOP, DefaultPermissionLevel.ALL,
                        "Allowed to make a shop that sells from a chest.");
                PermissionAPI.registerNode(EconomyManager.PERMMAKEINFSHOP, DefaultPermissionLevel.OP,
                        "Allowed to make a shop that sells infinite items.");

                PermissionAPI.registerNode(EconomyManager.PERMKILLSHOP, DefaultPermissionLevel.ALL,
                        "Allowed to remove a shop made by this player.");
                PermissionAPI.registerNode(EconomyManager.PERMKILLSHOPOTHER, DefaultPermissionLevel.OP,
                        "Allowed to remove a shop made by another player.");
            }
            MinecraftForge.EVENT_BUS.register(EconomyManager.instance);
        }
        return EconomyManager.instance;
    }

    public EconomyManager()
    {
        MinecraftForge.EVENT_BUS.register(this);
        final Account master = new Account();
        master.balance = Integer.MAX_VALUE;
        this.bank.put(EconomyManager.DEFAULT_ID, master);
    }

    @SubscribeEvent(receiveCanceled = true)
    public void interactRightClickEntity(final PlayerInteractEvent.EntityInteract evt)
    {
        if (!(evt.getPlayer() instanceof ServerPlayerEntity)) return;
        if (!Essentials.config.shopsEnabled) return;
        if (evt.getTarget() instanceof ItemFrameEntity)
        {
            final Coordinate c = new Coordinate(evt.getPos().down(), evt.getPlayer().dimension);
            Shop shop = EconomyManager.getShop(c);
            final TileEntity tile = evt.getWorld().getTileEntity(new BlockPos(c.x, c.y, c.z));
            if (evt.getItemStack() != null && tile instanceof SignTileEntity && shop == null && (evt.getItemStack()
                    .getDisplayName().getUnformattedComponentText().contains("Shop") || evt.getItemStack()
                            .getDisplayName().getUnformattedComponentText().contains("InfShop")))
            {
                final boolean infinite = evt.getItemStack().getDisplayName().getUnformattedComponentText().contains(
                        "InfShop");
                final String permission = infinite ? EconomyManager.PERMMAKEINFSHOP : EconomyManager.PERMMAKESHOP;
                if (!PermissionAPI.hasPermission(evt.getPlayer(), permission))
                {
                    evt.getPlayer().sendMessage(CommandManager.makeFormattedComponent(
                            "thutessentials.econ.not_allowed"));
                    return;
                }
                try
                {
                    final boolean noTag = evt.getItemStack().getDisplayName().getUnformattedComponentText().contains(
                            "noTag");
                    shop = EconomyManager.addShop((ServerPlayerEntity) evt.getPlayer(), (ItemFrameEntity) evt
                            .getTarget(), c, infinite, noTag);
                    evt.getPlayer().sendMessage(CommandManager.makeFormattedComponent("thutessentials.econ.made"));

                }
                catch (final Exception e)
                {
                    evt.getPlayer().sendMessage(CommandManager.makeFormattedComponent("thutessentials.econ.errored"));
                    Essentials.LOGGER.error(e);
                }
            }
            if (shop != null) evt.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void projectileImpact(final ProjectileImpactEvent evt)
    {
        if (evt.getEntity().getEntityWorld().isRemote) return;
        if (!Essentials.config.shopsEnabled) return;
        if (evt.getRayTraceResult().getType() == Type.MISS) return;
        if (!(evt.getRayTraceResult() instanceof EntityRayTraceResult)) return;
        final EntityRayTraceResult hit = (EntityRayTraceResult) evt.getRayTraceResult();
        final Entity target = hit.getEntity();
        if (target instanceof ItemFrameEntity)
        {
            final Coordinate c = new Coordinate(target.getPosition().down(2), target.dimension);
            final Shop shop = EconomyManager.getShop(c);
            if (shop != null) evt.setCanceled(true);
        }
    }

    @SubscribeEvent(receiveCanceled = true)
    public void interactLeftClickEntity(final AttackEntityEvent evt)
    {
        if (evt.getPlayer().getEntityWorld().isRemote) return;
        if (!Essentials.config.shopsEnabled) return;
        if (evt.getTarget() instanceof ItemFrameEntity)
        {
            final Coordinate c = new Coordinate(evt.getTarget().getPosition().down(2), evt.getTarget().dimension);
            final Shop shop = EconomyManager.getShop(c);
            if (shop != null)
            {
                evt.setCanceled(true);
                final Account account = this._shopMap.get(c);
                final UUID owner = this._revBank.get(account);
                final String perm = evt.getPlayer().getUniqueID().equals(owner) ? EconomyManager.PERMKILLSHOP
                        : EconomyManager.PERMKILLSHOPOTHER;
                if (PermissionAPI.hasPermission(evt.getPlayer(), perm))
                {
                    EconomyManager.removeShop(c);
                    evt.getPlayer().sendMessage(CommandManager.makeFormattedComponent("thutessentials.econ.remove"));
                }
                else evt.getPlayer().sendMessage(CommandManager.makeFormattedComponent(
                        "thutessentials.econ.not_allowed_remove"));
            }
        }
    }

    /**
     * Uses player interact here to also prevent opening of inventories.
     *
     * @param evt
     */
    @SubscribeEvent(receiveCanceled = true)
    public void interactRightClickBlock(final PlayerInteractEvent.RightClickBlock evt)
    {
        if (!(evt.getPlayer() instanceof ServerPlayerEntity) || !Essentials.config.shopsEnabled) return;
        final Coordinate c = new Coordinate(evt.getPos(), evt.getPlayer().dimension);
        final Shop shop = EconomyManager.getShop(c);
        if (shop != null) shop.transact((ServerPlayerEntity) evt.getPlayer(), evt.getItemStack(), this._shopMap.get(c));
    }

    public Account getAccount(final UUID player)
    {
        Account account = this.bank.get(player);
        if (account == null)
        {
            this.bank.put(player, account = new Account());
            account._id = player;
            account.balance = this.initial;
            EconomySaveHandler.saveGlobalData();
        }
        return account;
    }

    public Account getAccount(final ServerPlayerEntity player)
    {
        return this.getAccount(player.getUniqueID());
    }

    public static Shop addShop(final ServerPlayerEntity owner, final ItemFrameEntity frame, final Coordinate location,
            final boolean infinite, final boolean noTag)
    {
        final Shop shop = new Shop();
        shop.infinite = infinite;
        shop.frameId = frame.getUniqueID();
        shop.location = location;
        shop.ignoreTag = noTag;
        // Assign the infinite shops to the default id account.
        final Account account = EconomyManager.getInstance().getAccount(infinite ? EconomyManager.DEFAULT_ID
                : owner.getUniqueID());
        account.shops.add(shop);
        account._shopMap.put(location, shop);
        EconomyManager.getInstance()._shopMap.put(location, account);
        if (!shop.infinite)
        {
            final TileEntity down = owner.getEntityWorld().getTileEntity(new BlockPos(location.x, location.y - 1,
                    location.z));
            if (down instanceof SignTileEntity)
            {
                final String[] var = ((SignTileEntity) down).signText[0].getUnformattedComponentText().split(",");
                final int dx = Integer.parseInt(var[0]);
                final int dy = Integer.parseInt(var[1]);
                final int dz = Integer.parseInt(var[2]);

                final BlockPos pos = new BlockPos(location.x + dx, location.y + dy, location.z + dz);
                final BreakEvent event = new BreakEvent(owner.getEntityWorld(), pos, owner.getEntityWorld()
                        .getBlockState(pos), owner);
                MinecraftForge.EVENT_BUS.post(event);
                if (event.isCanceled())
                {
                    owner.sendMessage(CommandManager.makeFormattedComponent("thutessentials.econ.not_allowed_link"));
                    return null;
                }

                shop.storage = new Coordinate(location.x + dx, location.y - 1 + dy, location.z + dz, location.dim);
            }
            else shop.storage = new Coordinate(location.x, location.y - 1, location.z, location.dim);
        }
        EconomySaveHandler.saveGlobalData();
        return shop;
    }

    public static void removeShop(final Coordinate location)
    {
        final Account account = EconomyManager.getInstance()._shopMap.remove(location);
        if (account != null)
        {
            account.shops.remove(account._shopMap.remove(location));
            EconomySaveHandler.saveGlobalData();
        }
    }

    public static Shop getShop(final Coordinate location)
    {
        final Account account = EconomyManager.getInstance()._shopMap.get(location);
        if (account == null) return null;
        return account._shopMap.get(location);
    }

    public static int getBalance(final ServerPlayerEntity player)
    {
        return EconomyManager.getBalance(player.getUniqueID());
    }

    public static void setBalance(final ServerPlayerEntity player, final int amount)
    {
        EconomyManager.setBalance(player.getUniqueID(), amount);
    }

    public static void addBalance(final ServerPlayerEntity player, final int amount)
    {
        EconomyManager.addBalance(player.getUniqueID(), amount);
    }

    public static int getBalance(final UUID player)
    {
        return EconomyManager.getInstance().getAccount(player).balance;
    }

    public static void setBalance(final UUID player, final int amount)
    {
        final Account account = EconomyManager.getInstance().getAccount(player);
        account.balance = amount;
        EconomySaveHandler.saveGlobalData();
    }

    public static void addBalance(final UUID player, final int amount)
    {
        final Account account = EconomyManager.getInstance().getAccount(player);
        account.balance += amount;
        EconomySaveHandler.saveGlobalData();
    }

    public static void giveItem(final ServerPlayerEntity entityplayer, final ItemStack itemstack)
    {
        final boolean flag = entityplayer.inventory.addItemStackToInventory(itemstack);
        if (flag)
        {
            entityplayer.world.playSound((ServerPlayerEntity) null, entityplayer.posX, entityplayer.posY,
                    entityplayer.posZ, SoundEvents.ENTITY_ITEM_PICKUP, SoundCategory.PLAYERS, 0.2F, ((entityplayer
                            .getRNG().nextFloat() - entityplayer.getRNG().nextFloat()) * 0.7F + 1.0F) * 2.0F);
            entityplayer.container.detectAndSendChanges();
        }
        else
        {
            final ItemEntity entityitem = entityplayer.dropItem(itemstack, false);
            if (entityitem != null)
            {
                entityitem.setNoPickupDelay();
                entityitem.setOwnerId(entityplayer.getUniqueID());
            }
        }
    }
}