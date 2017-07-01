package thut.essentials.land;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemFood;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent;
import net.minecraftforge.event.entity.living.LivingSpawnEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.BlockEvent.BreakEvent;
import net.minecraftforge.event.world.ExplosionEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.Event.Result;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.fml.relauncher.Side;
import thut.essentials.events.DenyItemUseEvent;
import thut.essentials.events.DenyItemUseEvent.UseType;
import thut.essentials.land.LandManager.LandTeam;
import thut.essentials.util.CompatWrapper;
import thut.essentials.util.ConfigManager;
import thut.essentials.util.Coordinate;

public class LandEventsHandler
{
    public static Set<Class<?>> protectedEntities   = Sets.newHashSet();
    public static Set<String>   itemUseWhitelist    = Sets.newHashSet();
    public static Set<String>   blockUseWhiteList   = Sets.newHashSet();
    public static Set<String>   blockBreakWhiteList = Sets.newHashSet();

    public static void init()
    {
        protectedEntities.clear();
        for (String s : ConfigManager.INSTANCE.protectedEntities)
        {
            try
            {
                Class<?> c = Class.forName(s);
                protectedEntities.add(c);
            }
            catch (Throwable t)
            {
                t.printStackTrace();
            }
        }
        itemUseWhitelist.clear();
        for (String s : ConfigManager.INSTANCE.itemUseWhitelist)
        {
            itemUseWhitelist.add(s);
        }
        blockUseWhiteList.clear();
        for (String s : ConfigManager.INSTANCE.blockUseWhitelist)
        {
            blockUseWhiteList.add(s);
        }
        blockBreakWhiteList.clear();
        for (String s : ConfigManager.INSTANCE.blockBreakWhitelist)
        {
            blockBreakWhiteList.add(s);
        }
    }

    public static boolean sameTeam(Entity a, Entity b)
    {
        return LandManager.getTeam(a) == LandManager.getTeam(b);
    }

    Map<UUID, Long> lastLeaveMessage = Maps.newHashMap();
    Map<UUID, Long> lastEnterMessage = Maps.newHashMap();

    public LandEventsHandler()
    {
    }

    @SubscribeEvent
    public void BreakBlock(BreakEvent evt)
    {
        EntityPlayer player = evt.getPlayer();
        if (ConfigManager.INSTANCE.landEnabled && player != null)
        {
            Coordinate c = Coordinate.getChunkCoordFromWorldCoord(evt.getPos(), player.dimension);
            if (!LandManager.getInstance().isOwned(c)) return;
            if (!LandManager.owns(player, c))
            {
                player.addChatMessage(getDenyMessage(LandManager.getInstance().getLandOwner(c)));
                evt.setCanceled(true);
                return;
            }
            Coordinate block = new Coordinate(evt.getPos(), evt.getWorld().provider.getDimension());
            LandManager.getInstance().unsetPublic(block);
        }
    }

    @SubscribeEvent
    public void PlayerLoggin(PlayerLoggedInEvent evt)
    {
        EntityPlayer entityPlayer = evt.player;
        LandManager.getTeam(entityPlayer);
    }

    @SubscribeEvent
    public void EntityUpdate(LivingUpdateEvent evt)
    {
        if (evt.getEntityLiving() instanceof EntityPlayer
                && FMLCommonHandler.instance().getEffectiveSide() == Side.SERVER
                && evt.getEntityLiving().ticksExisted > 10)
        {
            EntityPlayer player = (EntityPlayer) evt.getEntityLiving();
            BlockPos here;
            BlockPos old;
            here = new BlockPos(player.chasingPosX, player.chasingPosY, player.chasingPosZ);
            old = new BlockPos(player.prevChasingPosX, player.prevChasingPosY, player.prevChasingPosZ);
            Coordinate c = Coordinate.getChunkCoordFromWorldCoord(here, player.dimension);
            Coordinate c1 = Coordinate.getChunkCoordFromWorldCoord(old, player.dimension);
            if (c.equals(c1) || !ConfigManager.INSTANCE.landEnabled) return;
            if (LandManager.getInstance().isOwned(c) || LandManager.getInstance().isOwned(c1))
            {
                LandTeam team = LandManager.getInstance().getLandOwner(c);
                LandTeam team1 = LandManager.getInstance().getLandOwner(c1);
                if (!lastLeaveMessage.containsKey(evt.getEntity().getUniqueID()))
                    lastLeaveMessage.put(evt.getEntity().getUniqueID(), System.currentTimeMillis() - 1);
                if (!lastEnterMessage.containsKey(evt.getEntity().getUniqueID()))
                    lastEnterMessage.put(evt.getEntity().getUniqueID(), System.currentTimeMillis() - 1);
                if (team != null)
                {
                    if (team.equals(team1)) return;
                    if (team1 != null)
                    {
                        long last = lastLeaveMessage.get(evt.getEntity().getUniqueID());
                        if (last < System.currentTimeMillis())
                        {
                            evt.getEntity().addChatMessage(getExitMessage(team1));
                            lastLeaveMessage.put(evt.getEntity().getUniqueID(), System.currentTimeMillis() + 100);
                        }
                    }
                    long last = lastEnterMessage.get(evt.getEntity().getUniqueID());
                    if (last < System.currentTimeMillis())
                    {
                        evt.getEntity().addChatMessage(getEnterMessage(team));
                        lastLeaveMessage.put(evt.getEntity().getUniqueID(), System.currentTimeMillis() + 100);
                    }
                }
                else
                {
                    long last = lastLeaveMessage.get(evt.getEntity().getUniqueID());
                    if (last < System.currentTimeMillis())
                    {
                        evt.getEntity().addChatMessage(getExitMessage(team1));
                        lastLeaveMessage.put(evt.getEntity().getUniqueID(), System.currentTimeMillis() + 100);
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public void ExplosionEvent(ExplosionEvent.Detonate evt)
    {
        List<BlockPos> toRemove = Lists.newArrayList();
        boolean denyBlasts = ConfigManager.INSTANCE.denyExplosions;
        if (ConfigManager.INSTANCE.landEnabled)
        {
            int dimension = evt.getWorld().provider.getDimension();
            for (BlockPos pos : evt.getAffectedBlocks())
            {
                Coordinate c = Coordinate.getChunkCoordFromWorldCoord(pos, dimension);
                LandTeam owner = LandManager.getInstance().getLandOwner(c);
                boolean deny = denyBlasts;
                if (owner == null) continue;
                deny = deny || owner.noExplosions;
                if (!deny) continue;
                toRemove.add(pos);
            }
        }
        evt.getAffectedBlocks().removeAll(toRemove);
    }

    /** Uses player interact here to also prevent opening of inventories.
     * 
     * @param evt */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void interactLeftClickBlock(PlayerInteractEvent.LeftClickBlock evt)
    {
        Coordinate c = Coordinate.getChunkCoordFromWorldCoord(evt.getPos(), evt.getEntityPlayer().dimension);
        LandTeam owner = LandManager.getInstance().getLandOwner(c);
        if (owner == null || !ConfigManager.INSTANCE.landEnabled) return;
        Block block = evt.getWorld().getBlockState(evt.getPos()).getBlock();
        String name = block.getRegistryName().toString();
        if (blockBreakWhiteList.contains(name)) { return; }
        if (LandManager.owns(evt.getEntityPlayer(), c)) { return; }
        Coordinate blockLoc = new Coordinate(evt.getPos(), evt.getEntityPlayer().dimension);
        LandManager.getInstance().isPublic(blockLoc);
        if (!LandManager.getInstance().isPublic(blockLoc))
        {
            evt.setUseBlock(Result.DENY);
            evt.setCanceled(true);
            if (!evt.getWorld().isRemote) evt.getEntity().addChatMessage(getDenyMessage(owner));
        }
        evt.setUseItem(Result.DENY);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void interactRightClickEntity(PlayerInteractEvent.EntityInteract evt)
    {
        Coordinate c = Coordinate.getChunkCoordFromWorldCoord(evt.getPos(), evt.getEntityPlayer().dimension);
        LandTeam owner = LandManager.getInstance().getLandOwner(c);
        if (owner == null || !ConfigManager.INSTANCE.landEnabled) return;
        if (LandManager.owns(evt.getEntityPlayer(), c)) { return; }
        for (Class<?> clas : protectedEntities)
        {
            if (clas.isInstance(evt.getTarget()))
            {
                evt.setCanceled(true);
                return;
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void interactLeftClickEntity(LivingAttackEvent evt)
    {
        if (!ConfigManager.INSTANCE.landEnabled) return;
        Coordinate c = Coordinate.getChunkCoordFromWorldCoord(evt.getEntity().getPosition(), evt.getEntity().dimension);
        LandTeam owner = LandManager.getInstance().getLandOwner(c);
        LandTeam players = LandManager.getTeam(evt.getEntity());
        if (players != null && !players.friendlyFire && evt.getEntity() instanceof EntityPlayer)
        {
            Entity damageSource = evt.getSource().getSourceOfDamage();
            if (damageSource instanceof EntityPlayer && sameTeam(damageSource, evt.getEntity()))
            {
                evt.setCanceled(true);
                return;
            }
        }
        if (owner == null) return;
        if (owner.noPlayerDamage && evt.getEntity() instanceof EntityPlayer)
        {
            evt.setCanceled(true);
            return;
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void interactLeftClickEntity(LivingHurtEvent evt)
    {
        Coordinate c = Coordinate.getChunkCoordFromWorldCoord(evt.getEntity().getPosition(), evt.getEntity().dimension);
        LandTeam owner = LandManager.getInstance().getLandOwner(c);
        if (owner == null || !ConfigManager.INSTANCE.landEnabled) return;
        if (owner.noPlayerDamage && evt.getEntity() instanceof EntityPlayer)
        {
            evt.setCanceled(true);
            return;
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void mobSpawnEvent(LivingSpawnEvent.SpecialSpawn evt)
    {
        Coordinate c = Coordinate.getChunkCoordFromWorldCoord(evt.getEntity().getPosition(), evt.getEntity().dimension);
        LandTeam owner = LandManager.getInstance().getLandOwner(c);
        if (owner == null || !ConfigManager.INSTANCE.landEnabled) return;
        if (owner.noMobSpawn)
        {
            evt.setResult(Result.DENY);
            return;
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void mobSpawnEvent(LivingSpawnEvent.CheckSpawn evt)
    {
        Coordinate c = Coordinate.getChunkCoordFromWorldCoord(evt.getEntity().getPosition(), evt.getEntity().dimension);
        LandTeam owner = LandManager.getInstance().getLandOwner(c);
        if (owner == null || !ConfigManager.INSTANCE.landEnabled) return;
        if (owner.noMobSpawn)
        {
            evt.setResult(Result.DENY);
            return;
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void interactLeftClickEntity(AttackEntityEvent evt)
    {
        Coordinate c = Coordinate.getChunkCoordFromWorldCoord(evt.getTarget().getPosition(),
                evt.getEntityPlayer().dimension);
        LandTeam owner = LandManager.getInstance().getLandOwner(c);
        if (owner == null || !ConfigManager.INSTANCE.landEnabled) return;
        if (LandManager.owns(evt.getEntityPlayer(), c)) { return; }
        for (Class<?> clas : protectedEntities)
        {
            if (clas.isInstance(evt.getTarget()))
            {
                evt.setCanceled(true);
                return;
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void interactRightClickItem(PlayerInteractEvent.RightClickItem evt)
    {
        Coordinate c = Coordinate.getChunkCoordFromWorldCoord(evt.getPos(), evt.getEntityPlayer().dimension);
        LandTeam owner = LandManager.getInstance().getLandOwner(c);
        if (owner == null || evt.getItemStack().getItem() instanceof ItemFood
                || evt.getItemStack().getItem() == Items.WRITTEN_BOOK
                || evt.getItemStack().getItem() == Items.WRITABLE_BOOK || !ConfigManager.INSTANCE.landEnabled
                || evt.getEntity().worldObj.isRemote)
            return;
        String name = evt.getItemStack().getItem().getRegistryName().toString();
        if (itemUseWhitelist.contains(name)) { return; }
        if (LandManager.owns(evt.getEntityPlayer(), c))
        {
            return;
        }
        else if (MinecraftForge.EVENT_BUS
                .post(new DenyItemUseEvent(evt.getEntity(), evt.getItemStack(), UseType.RIGHTCLICKBLOCK))) { return; }
        if (evt.getItemStack() == null) return;
        Coordinate blockLoc = new Coordinate(evt.getPos(), evt.getEntityPlayer().dimension);
        LandManager.getInstance().isPublic(blockLoc);
        if (!LandManager.getInstance().isPublic(blockLoc))
        {
            evt.setResult(Result.DENY);
            evt.setCanceled(true);
        }
        evt.setResult(Result.DENY);
    }

    /** Uses player interact here to also prevent opening of inventories.
     * 
     * @param evt */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void interactRightClickBlock(PlayerInteractEvent.RightClickBlock evt)
    {
        Coordinate c = Coordinate.getChunkCoordFromWorldCoord(evt.getPos(), evt.getEntityPlayer().dimension);
        LandTeam owner = LandManager.getInstance().getLandOwner(c);
        if (owner == null || !ConfigManager.INSTANCE.landEnabled) return;
        Block block = null;
        IBlockState state = evt.getWorld().getBlockState(evt.getPos());
        block = state.getBlock();
        String name = block.getRegistryName().toString();
        if (blockUseWhiteList.contains(name)) { return; }
        boolean b = true;
        boolean shouldPass = true;
        if (LandManager.owns(evt.getEntityPlayer(), c))
        {
            if (!evt.getWorld().isRemote && evt.getItemStack() != null
                    && evt.getItemStack().getDisplayName().equals("Public Toggle")
                    && evt.getEntityPlayer().isSneaking())
            {
                if (LandManager.getInstance().isAdmin(evt.getEntityPlayer().getUniqueID()))
                {
                    Coordinate blockLoc = new Coordinate(evt.getPos(), evt.getEntityPlayer().dimension);
                    if (LandManager.getInstance().isPublic(blockLoc))
                    {
                        evt.getEntityPlayer().addChatMessage(new TextComponentString("Set Block to Team Only"));
                        LandManager.getInstance().unsetPublic(blockLoc);
                    }
                    else
                    {
                        evt.getEntityPlayer().addChatMessage(new TextComponentString("Set Block to Public Use"));
                        LandManager.getInstance().setPublic(blockLoc, owner);
                    }
                    evt.setCanceled(true);
                }
            }
            return;
        }
        else if (block != null && !(block.hasTileEntity(state)) && !evt.getWorld().isRemote)
        {
            shouldPass = MinecraftForge.EVENT_BUS
                    .post(new DenyItemUseEvent(evt.getEntity(), evt.getItemStack(), UseType.RIGHTCLICKBLOCK));
            name = evt.getItemStack().getItem().getRegistryName().toString();
            shouldPass = shouldPass || itemUseWhitelist.contains(name);
            if (shouldPass) b = CompatWrapper.interactWithBlock(block, evt.getWorld(), evt.getPos(), state,
                    evt.getEntityPlayer(), evt.getHand(), null, evt.getFace(), (float) evt.getHitVec().xCoord,
                    (float) evt.getHitVec().yCoord, (float) evt.getHitVec().zCoord);
        }
        if (!b && shouldPass) return;
        Coordinate blockLoc = new Coordinate(evt.getPos(), evt.getEntityPlayer().dimension);
        LandManager.getInstance().isPublic(blockLoc);
        if (!LandManager.getInstance().isPublic(blockLoc))
        {
            evt.setUseBlock(Result.DENY);
            evt.setCanceled(true);
            if (!evt.getWorld().isRemote && evt.getHand() == EnumHand.MAIN_HAND)
            {
                evt.getEntity().addChatMessage(getDenyMessage(owner));
            }
        }
        evt.setUseItem(Result.DENY);
    }

    public void onServerStarted()
    {
        LandSaveHandler.loadGlobalData();
    }

    public void onServerStopped()
    {
        LandManager.clearInstance();
    }

    private static ITextComponent getDenyMessage(LandTeam team)
    {
        if (team != null && !team.denyMessage.isEmpty()) { return new TextComponentString(team.denyMessage); }
        return new TextComponentTranslation("msg.team.deny", team.teamName);
    }

    private static ITextComponent getEnterMessage(LandTeam team)
    {
        if (team != null && !team.enterMessage.isEmpty()) { return new TextComponentString(team.enterMessage); }
        return new TextComponentTranslation("msg.team.enterLand", team.teamName);
    }

    private static ITextComponent getExitMessage(LandTeam team)
    {
        if (team != null && !team.exitMessage.isEmpty()) { return new TextComponentString(team.exitMessage); }
        return new TextComponentTranslation("msg.team.exitLand", team.teamName);
    }
}
