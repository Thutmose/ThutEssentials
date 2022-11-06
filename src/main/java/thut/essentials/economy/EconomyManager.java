package thut.essentials.economy;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent.BreakEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import thut.essentials.Essentials;
import thut.essentials.commands.CommandManager;
import thut.essentials.land.LandManager.KGobalPos;
import thut.essentials.land.LandSaveHandler;
import thut.essentials.util.ChatHelper;
import thut.essentials.util.PermNodes;
import thut.essentials.util.PermNodes.DefaultPermissionLevel;

public class EconomyManager
{
    public static class Account
    {
        int balance;
        Set<Shop> shops = Sets.newHashSet();
        UUID _id;
        Map<KGobalPos, Shop> _shopMap = Maps.newHashMap();
    }

    public static class Shop
    {
        KGobalPos location;
        KGobalPos storage;
        UUID frameId;
        boolean infinite = false;
        boolean ignoreTag = false;
        boolean recycle = false;
        boolean sell;
        int cost;
        int number;

        public boolean transact(final ServerPlayer player, final ItemStack heldStack, final Account shopAccount)
        {
            ItemStack stack = ItemStack.EMPTY;
            final ServerLevel world = (ServerLevel) player.getCommandSenderWorld();
            final Entity ent = world.getEntity(this.frameId);
            if (ent instanceof ItemFrame) stack = ((ItemFrame) ent).getItem();
            final BlockEntity tile = player.level.getBlockEntity(this.location.getPos());
            if (!(tile instanceof SignBlockEntity))
            {
                EconomyManager.removeShop(this.location);
                return false;
            }
            final SignBlockEntity sign = (SignBlockEntity) tile;
            this.sell = Essentials.config.sellTags.contains(sign.messages[0].getString());
            this.recycle = Essentials.config.recycleTags.contains(sign.messages[0].getString());
            try
            {
                this.number = Integer.parseInt(sign.messages[1].getString());
                if (this.ignoreTag)
                {
                    if (this.number != 1) sign.messages[1] = Component.literal("1");
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
                this.cost = Integer.parseInt(sign.messages[3].getString());
            }
            catch (final NumberFormatException e)
            {
                e.printStackTrace();
                return false;
            }

            if (this.recycle && heldStack.isEmpty())
            {
                ChatHelper.sendSystemMessage(player,
                        CommandManager.makeFormattedComponent("thutessentials.econ.no_recycle"));
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
                    ChatHelper.sendSystemMessage(player,
                            CommandManager.makeFormattedComponent("thutessentials.econ.no_funds_you"));
                    return false;
                }
                stack = stack.copy();
                stack.setCount(this.number);
                if (!this.infinite)
                {
                    int count = 0;
                    Container inv = null;
                    final ItemStack test2 = stack.copy();
                    if (this.storage != null)
                    {
                        final BlockEntity inventory = player.level.getBlockEntity(this.storage.getPos());
                        if (inventory instanceof Container)
                        {
                            inv = (Container) inventory;
                            if (this.ignoreTag) test2.setTag(new CompoundTag());
                            for (int i = 0; i < inv.getContainerSize(); i++)
                            {
                                final ItemStack item = inv.getItem(i);
                                if (!item.isEmpty())
                                {
                                    final ItemStack test = item.copy();
                                    if (this.ignoreTag) test.setTag(new CompoundTag());
                                    test.setCount(this.number);
                                    if (ItemStack.matches(test, test2)) count += item.getCount();
                                }
                            }
                        }
                    }
                    if (count < this.number || inv == null)
                    {
                        Essentials.LOGGER.debug(this.number + " " + count + " " + this.storage);
                        ChatHelper.sendSystemMessage(player,
                                CommandManager.makeFormattedComponent("thutessentials.econ.no_items_shop"));
                        return false;
                    }
                    int i = 0;
                    final Item itemIn = test2.getItem();
                    final int removeCount = this.number;
                    final CompoundTag itemNBT = this.ignoreTag ? null : test2.getTag();
                    for (int j = 0; j < inv.getContainerSize(); ++j)
                    {
                        final ItemStack itemstack = inv.getItem(j);
                        if (!itemstack.isEmpty() && itemstack.getItem() == itemIn
                                && (itemNBT == null || NbtUtils.compareNbt(itemNBT, itemstack.getTag(), true)))
                        {

                            final int k = removeCount <= 0 ? itemstack.getCount()
                                    : Math.min(removeCount - i, itemstack.getCount());
                            i += k;
                            if (this.number == 1) stack.setTag(itemstack.getTag());
                            if (removeCount != 0)
                            {
                                itemstack.shrink(k);
                                if (itemstack.isEmpty()) inv.setItem(j, ItemStack.EMPTY);
                                if (removeCount > 0 && i >= removeCount) break;
                            }
                        }
                    }
                }
                EconomyManager.giveItem(player, stack);
                EconomyManager.addBalance(shopAccount._id, this.cost);
                EconomyManager.addBalance(player, -this.cost);
                ChatHelper.sendSystemMessage(player, CommandManager.makeFormattedComponent(
                        "thutessentials.econ.balance.remaining", null, false, EconomyManager.getBalance(player)));
            }
            else
            {
                final int balance = this.infinite ? Integer.MAX_VALUE : shopAccount.balance;
                if (balance < this.cost)
                {
                    ChatHelper.sendSystemMessage(player,
                            CommandManager.makeFormattedComponent("thutessentials.econ.no_funds_shop"));
                    return false;
                }
                int count = 0;
                final ItemStack toTest = stack.copy();
                toTest.setCount(1);
                final Predicate<ItemStack> valid = i -> {
                    final ItemStack temp = i.copy();
                    temp.setCount(1);
                    return ItemStack.matches(temp, toTest);
                };
                if (this.recycle)
                {
                    count = heldStack.getCount();
                    if (count < this.number)
                    {
                        ChatHelper.sendSystemMessage(player,
                                CommandManager.makeFormattedComponent("thutessentials.econ.no_items_you"));
                        return false;
                    }
                    stack = heldStack;
                }
                else
                {
                    stack = stack.copy();
                    stack.setCount(this.number);
                    for (final ItemStack item : player.getInventory().items)
                        if (!item.isEmpty()) if (valid.test(item)) count += item.getCount();
                    if (count < this.number)
                    {
                        ChatHelper.sendSystemMessage(player,
                                CommandManager.makeFormattedComponent("thutessentials.econ.no_items_you"));
                        return false;
                    }
                }
                if (!this.infinite)
                {
                    if (this.storage == null)
                    {
                        ChatHelper.sendSystemMessage(player,
                                CommandManager.makeFormattedComponent("thutessentials.econ.no_storage"));
                        return false;
                    }
                    final BlockEntity te = player.level.getBlockEntity(this.storage.getPos());
                    if (te instanceof Container)
                    {
                        final Container inv = (Container) te;
                        count = 0;
                        final ItemStack a = stack;
                        count = stack.getCount();
                        for (int i = 0; i < inv.getContainerSize(); i++)
                        {
                            if (inv.getItem(i).isEmpty() || a.sameItem(inv.getItem(i)))
                            {
                                int n = 0;
                                if (!inv.getItem(i).isEmpty() && (n = inv.getItem(i).getCount() + a.getCount()) < 65)
                                {
                                    a.setCount(n);
                                    count = 0;
                                    inv.setItem(i, a.copy());
                                }
                                else if (inv.getItem(i).isEmpty())
                                {
                                    count = 0;
                                    inv.setItem(i, a.copy());
                                }
                            }
                            if (count == 0) break;
                        }
                    }
                }
                player.getInventory().clearOrCountMatchingItems(valid, this.number,
                        player.inventoryMenu.getCraftSlots());
                player.inventoryMenu.broadcastChanges();
                EconomyManager.addBalance(shopAccount._id, -this.cost);
                EconomyManager.addBalance(player, this.cost);
                ChatHelper.sendSystemMessage(player, CommandManager.makeFormattedComponent(
                        "thutessentials.econ.balance.remaining", null, false, EconomyManager.getBalance(player)));
            }
            return false;
        }
    }

    public static final int VERSION = 1;

    public static final String PERMMAKESHOP = "thutessentials.economy.make_shop";
    public static final String PERMMAKEINFSHOP = "thutessentials.economy.make_infinite_shop";

    public static final String PERMKILLSHOP = "thutessentials.economy.kill_shop";
    public static final String PERMKILLSHOPOTHER = "thutessentials.economy.kill_shop_other";

    public static final UUID DEFAULT_ID = new UUID(0, 0);

    public static EconomyManager instance;

    private static boolean init = false;

    public int version = EconomyManager.VERSION;
    public int initial = 1000;

    public Map<UUID, Account> bank = Maps.newHashMap();
    public Map<KGobalPos, Account> _shopMap = Maps.newHashMap();
    public Map<Account, UUID> _revBank = Maps.newHashMap();

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
            EconomyManager.instance.initial = Essentials.config.initialBalance;
            if (!EconomyManager.init)
            {
                EconomyManager.init = true;
                PermNodes.registerBooleanNode(EconomyManager.PERMMAKESHOP, DefaultPermissionLevel.ALL,
                        "Allowed to make a shop that sells from a chest.");
                PermNodes.registerBooleanNode(EconomyManager.PERMMAKEINFSHOP, DefaultPermissionLevel.OP,
                        "Allowed to make a shop that sells infinite items.");

                PermNodes.registerBooleanNode(EconomyManager.PERMKILLSHOP, DefaultPermissionLevel.ALL,
                        "Allowed to remove a shop made by this player.");
                PermNodes.registerBooleanNode(EconomyManager.PERMKILLSHOPOTHER, DefaultPermissionLevel.OP,
                        "Allowed to remove a shop made by another player.");
            }
            MinecraftForge.EVENT_BUS.register(EconomyManager.instance);
        }
        return EconomyManager.instance;
    }

    public static void registerPerms()
    {
        PermNodes.registerBooleanNode(EconomyManager.PERMMAKESHOP, DefaultPermissionLevel.ALL,
                "Allowed to make a shop that sells from a chest.");
        PermNodes.registerBooleanNode(EconomyManager.PERMMAKEINFSHOP, DefaultPermissionLevel.OP,
                "Allowed to make a shop that sells infinite items.");

        PermNodes.registerBooleanNode(EconomyManager.PERMKILLSHOP, DefaultPermissionLevel.ALL,
                "Allowed to remove a shop made by this player.");
        PermNodes.registerBooleanNode(EconomyManager.PERMKILLSHOPOTHER, DefaultPermissionLevel.OP,
                "Allowed to remove a shop made by another player.");
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
        if (!(evt.getEntity() instanceof ServerPlayer)) return;
        if (!Essentials.config.shopsEnabled) return;
        if (evt.getTarget() instanceof ItemFrame)
        {
            final KGobalPos c = KGobalPos.getPosition(evt.getEntity().getCommandSenderWorld().dimension(),
                    evt.getPos().below());
            Shop shop = EconomyManager.getShop(c);
            final BlockEntity tile = evt.getLevel().getBlockEntity(c.getPos());
            if (evt.getItemStack() != null && tile instanceof SignBlockEntity && shop == null
                    && (evt.getItemStack().getHoverName().getString().contains("Shop")
                            || evt.getItemStack().getHoverName().getString().contains("InfShop")))
            {
                final boolean infinite = evt.getItemStack().getHoverName().getString().contains("InfShop");
                final String permission = infinite ? EconomyManager.PERMMAKEINFSHOP : EconomyManager.PERMMAKESHOP;
                if (!PermNodes.getBooleanPerm((ServerPlayer) evt.getEntity(), permission))
                {
                    ChatHelper.sendSystemMessage(evt.getEntity(),
                            CommandManager.makeFormattedComponent("thutessentials.econ.not_allowed"));
                    return;
                }
                try
                {
                    final boolean noTag = evt.getItemStack().getHoverName().getString().contains("noTag");
                    shop = EconomyManager.addShop((ServerPlayer) evt.getEntity(), (ItemFrame) evt.getTarget(), c,
                            infinite, noTag);
                    ChatHelper.sendSystemMessage(evt.getEntity(),
                            CommandManager.makeFormattedComponent("thutessentials.econ.made"));

                }
                catch (final Exception e)
                {
                    ChatHelper.sendSystemMessage(evt.getEntity(),
                            CommandManager.makeFormattedComponent("thutessentials.econ.errored"));
                    Essentials.LOGGER.error(e);
                }
            }
            if (shop != null) evt.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void projectileImpact(final ProjectileImpactEvent evt)
    {
        if (evt.getEntity().getCommandSenderWorld().isClientSide) return;
        if (!Essentials.config.shopsEnabled) return;
        if (evt.getRayTraceResult().getType() == Type.MISS) return;
        if (!(evt.getRayTraceResult() instanceof EntityHitResult)) return;
        final EntityHitResult hit = (EntityHitResult) evt.getRayTraceResult();
        final Entity target = hit.getEntity();
        if (target instanceof ItemFrame)
        {
            final KGobalPos c = KGobalPos.getPosition(target.getCommandSenderWorld().dimension(),
                    target.blockPosition().below());
            final Shop shop = EconomyManager.getShop(c);
            if (shop != null) evt.setCanceled(true);
        }
    }

    @SubscribeEvent(receiveCanceled = true)
    public void interactLeftClickEntity(final AttackEntityEvent evt)
    {
        if (evt.getEntity().getCommandSenderWorld().isClientSide) return;
        if (!Essentials.config.shopsEnabled) return;
        if (evt.getTarget() instanceof ItemFrame)
        {
            final KGobalPos c = KGobalPos.getPosition(evt.getTarget().getCommandSenderWorld().dimension(),
                    evt.getTarget().blockPosition().below());
            final Shop shop = EconomyManager.getShop(c);
            if (shop != null)
            {
                evt.setCanceled(true);
                final Account account = this._shopMap.get(c);
                final UUID owner = this._revBank.get(account);
                final String perm = evt.getEntity().getUUID().equals(owner) ? EconomyManager.PERMKILLSHOP
                        : EconomyManager.PERMKILLSHOPOTHER;
                if (PermNodes.getBooleanPerm((ServerPlayer) evt.getEntity(), perm))
                {
                    EconomyManager.removeShop(c);
                    ChatHelper.sendSystemMessage(evt.getEntity(),
                            CommandManager.makeFormattedComponent("thutessentials.econ.remove"));
                }
                else ChatHelper.sendSystemMessage(evt.getEntity(),
                        CommandManager.makeFormattedComponent("thutessentials.econ.not_allowed_remove"));
            }
        }
    }

    /**
     * Uses player interact here to also prevent opening of inventories.
     *
     * @param evt
     */
    @SubscribeEvent(receiveCanceled = true, priority = EventPriority.HIGH)
    public void interactRightClickBlock(final PlayerInteractEvent.RightClickBlock evt)
    {
        if (!(evt.getEntity() instanceof ServerPlayer) || !Essentials.config.shopsEnabled) return;
        final KGobalPos c = KGobalPos.getPosition(evt.getEntity().getCommandSenderWorld().dimension(), evt.getPos());
        final Shop shop = EconomyManager.getShop(c);
        if (shop != null)
        {
            shop.transact((ServerPlayer) evt.getEntity(), evt.getItemStack(), this._shopMap.get(c));
            evt.setCanceled(true);
        }
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

    public Account getAccount(final ServerPlayer player)
    {
        return this.getAccount(player.getUUID());
    }

    public static Shop addShop(final ServerPlayer owner, final ItemFrame frame, final KGobalPos location,
            final boolean infinite, final boolean noTag)
    {
        final Shop shop = new Shop();
        shop.infinite = infinite;
        shop.frameId = frame.getUUID();
        shop.location = location;
        shop.ignoreTag = noTag;
        // Assign the infinite shops to the default id account.
        final Account account = EconomyManager.getInstance()
                .getAccount(infinite ? EconomyManager.DEFAULT_ID : owner.getUUID());
        account.shops.add(shop);
        account._shopMap.put(location, shop);
        EconomyManager.getInstance()._shopMap.put(location, account);
        if (!shop.infinite)
        {
            final BlockEntity down = owner.getCommandSenderWorld().getBlockEntity(location.getPos().below());
            if (down instanceof SignBlockEntity)
            {
                final String[] var = ((SignBlockEntity) down).messages[0].getString().split(",");
                final int dx = Integer.parseInt(var[0]);
                final int dy = Integer.parseInt(var[1]);
                final int dz = Integer.parseInt(var[2]);

                final BlockPos pos = new BlockPos(location.getPos().getX() + dx, location.getPos().getY() + dy,
                        location.getPos().getZ() + dz);
                final BreakEvent event = new BreakEvent(owner.getCommandSenderWorld(), pos,
                        owner.getCommandSenderWorld().getBlockState(pos), owner);
                MinecraftForge.EVENT_BUS.post(event);
                if (event.isCanceled())
                {
                    ChatHelper.sendSystemMessage(owner,
                            CommandManager.makeFormattedComponent("thutessentials.econ.not_allowed_link"));
                    return null;
                }

                shop.storage = KGobalPos.getPosition(location.getDimension(), pos.below());
            }
            else shop.storage = KGobalPos.getPosition(location.getDimension(), location.getPos().below());
        }
        EconomySaveHandler.saveGlobalData();
        return shop;
    }

    public static void removeShop(final KGobalPos location)
    {
        final Account account = EconomyManager.getInstance()._shopMap.remove(location);
        if (account != null)
        {
            account.shops.remove(account._shopMap.remove(location));
            EconomySaveHandler.saveGlobalData();
        }
    }

    public static Shop getShop(final KGobalPos location)
    {
        final Account account = EconomyManager.getInstance()._shopMap.get(location);
        if (account == null) return null;
        return account._shopMap.get(location);
    }

    public static int getBalance(final ServerPlayer player)
    {
        return EconomyManager.getBalance(player.getUUID());
    }

    public static void setBalance(final ServerPlayer player, final int amount)
    {
        EconomyManager.setBalance(player.getUUID(), amount);
    }

    public static void addBalance(final ServerPlayer player, final int amount)
    {
        EconomyManager.addBalance(player.getUUID(), amount);
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

    public static void giveItem(final ServerPlayer entityplayer, final ItemStack itemstack)
    {
        final boolean flag = entityplayer.getInventory().add(itemstack);
        if (flag)
        {
            entityplayer.level.playSound((ServerPlayer) null, entityplayer.getX(), entityplayer.getY(),
                    entityplayer.getZ(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.2F,
                    ((entityplayer.getRandom().nextFloat() - entityplayer.getRandom().nextFloat()) * 0.7F + 1.0F)
                            * 2.0F);
            entityplayer.inventoryMenu.broadcastChanges();
        }
        else
        {
            final ItemEntity entityitem = entityplayer.drop(itemstack, false);
            if (entityitem != null)
            {
                entityitem.setNoPickUpDelay();
                entityitem.setOwner(entityplayer.getUUID());
            }
        }
    }
}