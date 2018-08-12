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
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.item.ItemFood;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingSpawnEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.BlockEvent.BreakEvent;
import net.minecraftforge.event.world.BlockEvent.PlaceEvent;
import net.minecraftforge.event.world.ExplosionEvent;
import net.minecraftforge.fml.common.eventhandler.Event.Result;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.server.permission.DefaultPermissionLevel;
import net.minecraftforge.server.permission.IPermissionHandler;
import net.minecraftforge.server.permission.PermissionAPI;
import net.minecraftforge.server.permission.context.PlayerContext;
import thut.essentials.ThutEssentials;
import thut.essentials.events.DenyItemUseEvent;
import thut.essentials.events.DenyItemUseEvent.UseType;
import thut.essentials.land.LandManager.LandTeam;
import thut.essentials.util.CompatWrapper;
import thut.essentials.util.ConfigManager;
import thut.essentials.util.Coordinate;

public class LandEventsHandler
{

    public static Set<String> itemUseWhitelist    = Sets.newHashSet();
    public static Set<String> blockUseWhiteList   = Sets.newHashSet();
    public static Set<String> blockBreakWhiteList = Sets.newHashSet();

    public static void init()
    {
        MinecraftForge.EVENT_BUS.unregister(ThutEssentials.instance.teams);
        MinecraftForge.EVENT_BUS.unregister(ThutEssentials.instance.teams.interact_handler);
        MinecraftForge.EVENT_BUS.unregister(ThutEssentials.instance.teams.entity_handler);
        MinecraftForge.EVENT_BUS.unregister(ThutEssentials.instance.teams.block_handler);
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
        MinecraftForge.EVENT_BUS.register(ThutEssentials.instance.teams);
        MinecraftForge.EVENT_BUS.register(ThutEssentials.instance.teams.interact_handler);
        MinecraftForge.EVENT_BUS.register(ThutEssentials.instance.teams.entity_handler);
        MinecraftForge.EVENT_BUS.register(ThutEssentials.instance.teams.block_handler);
    }

    public static class BlockEventHandler
    {
        @SubscribeEvent(priority = EventPriority.HIGHEST)
        public void placeBlocks(PlaceEvent evt)
        {
            if (evt.getPlayer().getEntityWorld().isRemote) return;
            if (!ConfigManager.INSTANCE.landEnabled) return;
            EntityPlayer player = (EntityPlayer) evt.getPlayer();
            if (!(player instanceof EntityPlayerMP)) return;
            Coordinate c = Coordinate.getChunkCoordFromWorldCoord(evt.getPos(),
                    evt.getPlayer().getEntityWorld().provider.getDimension());
            IPermissionHandler manager = PermissionAPI.getPermissionHandler();
            PlayerContext context = new PlayerContext(player);
            boolean ownedLand = LandManager.getInstance().isOwned(c);
            if (!ownedLand)
            {
                if (manager.hasPermission(player.getGameProfile(), PERMPLACEWILD, context)) { return; }
                // TODO better message.
                player.sendMessage(new TextComponentString("Cannot place that."));
                evt.setCanceled(true);
                ((EntityPlayerMP) player).sendAllContents(player.inventoryContainer,
                        player.inventoryContainer.inventoryItemStacks);
                return;

            }
            boolean owns = LandManager.owns(player, c);
            if (owns && !manager.hasPermission(player.getGameProfile(), PERMPLACEOWN, context))
            {
                player.sendMessage(getDenyMessage(LandManager.getInstance().getLandOwner(c)));
                evt.setCanceled(true);
                ((EntityPlayerMP) player).sendAllContents(player.inventoryContainer,
                        player.inventoryContainer.inventoryItemStacks);
                return;
            }
            if (!owns && !manager.hasPermission(player.getGameProfile(), PERMPLACEOTHER, context))
            {
                player.sendMessage(getDenyMessage(LandManager.getInstance().getLandOwner(c)));
                evt.setCanceled(true);
                ((EntityPlayerMP) player).sendAllContents(player.inventoryContainer,
                        player.inventoryContainer.inventoryItemStacks);
                return;
            }
        }

        @SubscribeEvent
        public void BreakBlock(BreakEvent evt)
        {
            if (evt.getPlayer().getEntityWorld().isRemote) return;
            EntityPlayer player = evt.getPlayer();
            if (ConfigManager.INSTANCE.landEnabled && player != null)
            {
                String name = evt.getWorld().getBlockState(evt.getPos()).getBlock().getRegistryName().toString();
                if (blockBreakWhiteList.contains(name)) { return; }
                IPermissionHandler manager = PermissionAPI.getPermissionHandler();
                PlayerContext context = new PlayerContext(player);
                Coordinate c = Coordinate.getChunkCoordFromWorldCoord(evt.getPos(),
                        player.getEntityWorld().provider.getDimension());
                boolean ownedLand = LandManager.getInstance().isOwned(c);
                if (!ownedLand)
                {
                    if (manager.hasPermission(player.getGameProfile(), PERMBREAKWILD, context)) { return; }
                    // TODO better message.
                    player.sendMessage(new TextComponentString("Cannot break that."));
                    evt.setCanceled(true);
                    return;

                }
                boolean owns = LandManager.owns(player, c);
                if (owns && !manager.hasPermission(player.getGameProfile(), PERMBREAKOWN, context))
                {
                    player.sendMessage(getDenyMessage(LandManager.getInstance().getLandOwner(c)));
                    evt.setCanceled(true);
                    return;
                }
                if (!owns && !manager.hasPermission(player.getGameProfile(), PERMBREAKOTHER, context))
                {
                    player.sendMessage(getDenyMessage(LandManager.getInstance().getLandOwner(c)));
                    evt.setCanceled(true);
                    return;
                }
                Coordinate block = new Coordinate(evt.getPos(), evt.getWorld().provider.getDimension());
                LandManager.getInstance().unsetPublic(block);
            }
        }
    }

    public static class EntityEventHandler
    {
        @SubscribeEvent(priority = EventPriority.HIGHEST)
        public void death(LivingDeathEvent evt)
        {
            if (evt.getEntity().getEntityWorld().isRemote) return;
            if (!ConfigManager.INSTANCE.landEnabled) return;

            // Cleanup the entity from protected mobs.
            UUID id = evt.getEntity().getUniqueID();
            if (LandManager.getInstance()._protected_mobs.containsKey(id))
            {
                LandTeam team = LandManager.getInstance()._protected_mobs.remove(id);
                team.protected_mobs.remove(id);
                LandSaveHandler.saveTeam(team.teamName);
            }

            // Cleanup the entity from public mobs.
            if (LandManager.getInstance()._public_mobs.containsKey(id))
            {
                LandTeam team = LandManager.getInstance()._public_mobs.remove(id);
                team.public_mobs.remove(id);
                LandSaveHandler.saveTeam(team.teamName);
            }
        }

        @SubscribeEvent
        public void update(LivingUpdateEvent evt)
        {
            if (evt.getEntity().getEntityWorld().isRemote) return;
            if (evt.getEntityLiving() instanceof EntityPlayerMP && evt.getEntityLiving().ticksExisted > 10)
            {
                EntityPlayerMP player = (EntityPlayerMP) evt.getEntityLiving();
                BlockPos here;
                BlockPos old;
                here = new BlockPos(player.chasingPosX, player.chasingPosY, player.chasingPosZ);
                old = new BlockPos(player.prevChasingPosX, player.prevChasingPosY, player.prevChasingPosZ);
                Coordinate newChunk = Coordinate.getChunkCoordFromWorldCoord(here,
                        player.getEntityWorld().provider.getDimension());
                Coordinate oldChunk = Coordinate.getChunkCoordFromWorldCoord(old,
                        player.getEntityWorld().provider.getDimension());
                if (newChunk.equals(oldChunk) || !ConfigManager.INSTANCE.landEnabled) return;
                boolean isNewOwned = LandManager.getInstance().isOwned(newChunk);
                boolean isOldOwned = LandManager.getInstance().isOwned(oldChunk);

                if (isNewOwned || isOldOwned)
                {
                    LandTeam team = LandManager.getInstance().getLandOwner(newChunk);
                    LandTeam team1 = LandManager.getInstance().getLandOwner(oldChunk);
                    if (!lastLeaveMessage.containsKey(evt.getEntity().getUniqueID()))
                        lastLeaveMessage.put(evt.getEntity().getUniqueID(), System.currentTimeMillis() - 1);
                    if (!lastEnterMessage.containsKey(evt.getEntity().getUniqueID()))
                        lastEnterMessage.put(evt.getEntity().getUniqueID(), System.currentTimeMillis() - 1);

                    IPermissionHandler manager = PermissionAPI.getPermissionHandler();
                    PlayerContext context = new PlayerContext(player);
                    if (!isNewOwned && !manager.hasPermission(player.getGameProfile(), PERMENTERWILD, context))
                    {
                        player.connection.setPlayerLocation(old.getX() + 0.5, old.getY(), old.getZ() + 0.5,
                                player.rotationYaw, player.rotationPitch);
                        // TODO better message.
                        evt.getEntity().sendMessage(new TextComponentString("You may not enter there."));
                        return;
                    }
                    boolean owns = team != null && team.isMember(player);
                    if (isNewOwned && owns && !manager.hasPermission(player.getGameProfile(), PERMENTEROWN, context))
                    {
                        player.connection.setPlayerLocation(old.getX() + 0.5, old.getY(), old.getZ() + 0.5,
                                player.rotationYaw, player.rotationPitch);
                        // TODO better message.
                        evt.getEntity().sendMessage(new TextComponentString("You may not enter there."));
                        return;
                    }
                    else if (isNewOwned && !owns
                            && !manager.hasPermission(player.getGameProfile(), PERMENTEROTHER, context))
                    {
                        player.connection.setPlayerLocation(old.getX() + 0.5, old.getY(), old.getZ() + 0.5,
                                player.rotationYaw, player.rotationPitch);
                        // TODO better message.
                        evt.getEntity().sendMessage(new TextComponentString("You may not enter there."));
                        return;
                    }

                    messages:
                    {
                        if (team != null)
                        {
                            if (team.equals(team1)) break messages;
                            if (team1 != null)
                            {
                                long last = lastLeaveMessage.get(evt.getEntity().getUniqueID());
                                if (last < System.currentTimeMillis())
                                {
                                    evt.getEntity().sendMessage(getExitMessage(team1));
                                    lastLeaveMessage.put(evt.getEntity().getUniqueID(),
                                            System.currentTimeMillis() + 100);
                                }
                            }
                            long last = lastEnterMessage.get(evt.getEntity().getUniqueID());
                            if (last < System.currentTimeMillis())
                            {
                                evt.getEntity().sendMessage(getEnterMessage(team));
                                lastLeaveMessage.put(evt.getEntity().getUniqueID(), System.currentTimeMillis() + 100);
                            }
                        }
                        else
                        {
                            long last = lastLeaveMessage.get(evt.getEntity().getUniqueID());
                            if (last < System.currentTimeMillis())
                            {
                                evt.getEntity().sendMessage(getExitMessage(team1));
                                lastLeaveMessage.put(evt.getEntity().getUniqueID(), System.currentTimeMillis() + 100);
                            }
                        }
                    }
                }
            }
        }

        @SubscribeEvent(priority = EventPriority.HIGHEST)
        public void attack(AttackEntityEvent evt)
        {
            if (evt.getEntity().getEntityWorld().isRemote || !ConfigManager.INSTANCE.landEnabled) return;
            Coordinate c = Coordinate.getChunkCoordFromWorldCoord(evt.getTarget().getPosition(),
                    evt.getEntityPlayer().getEntityWorld().provider.getDimension());
            LandTeam owner = LandManager.getInstance().getLandOwner(c);

            // TODO possible perms for attacking things in unclaimed land?
            if (owner == null) return;

            // If mob is protected, do not allow the attack, even if by owner.
            if (owner.protected_mobs.contains(evt.getTarget().getUniqueID()))
            {
                evt.setCanceled(true);
                return;
            }
        }

        @SubscribeEvent(priority = EventPriority.HIGHEST)
        public void attack(LivingAttackEvent evt)
        {
            if (evt.getEntity().getEntityWorld().isRemote) return;
            if (!ConfigManager.INSTANCE.landEnabled) return;
            Coordinate c = Coordinate.getChunkCoordFromWorldCoord(evt.getEntity().getPosition(),
                    evt.getEntity().getEntityWorld().provider.getDimension());
            LandTeam owner = LandManager.getInstance().getLandOwner(c);

            // TODO maybe add a perm for combat in non-claimed land?
            if (owner == null) return;

            if (evt.getEntity() instanceof EntityPlayer)
            {
                LandTeam players = LandManager.getTeam(evt.getEntity());
                // Check if player is protected via friendly fire settings.
                if (!players.friendlyFire)
                {
                    Entity damageSource = evt.getSource().getTrueSource();
                    if (damageSource instanceof EntityPlayer && sameTeam(damageSource, evt.getEntity()))
                    {
                        evt.setCanceled(true);
                        return;
                    }
                }

                // Check if player is protected by team settings
                if (owner.noPlayerDamage)
                {
                    evt.setCanceled(true);
                    return;
                }
            }

            // check if entity is protected by team
            if (owner.protected_mobs.contains(evt.getEntity().getUniqueID()))
            {
                evt.setCanceled(true);
                return;
            }
        }

        @SubscribeEvent(priority = EventPriority.HIGHEST)
        public void attack(LivingHurtEvent evt)
        {
            if (evt.getEntity().getEntityWorld().isRemote) return;
            if (!ConfigManager.INSTANCE.landEnabled) return;
            Coordinate c = Coordinate.getChunkCoordFromWorldCoord(evt.getEntity().getPosition(),
                    evt.getEntity().getEntityWorld().provider.getDimension());
            LandTeam owner = LandManager.getInstance().getLandOwner(c);

            // TODO maybe add a perm for combat in non-claimed land?
            if (owner == null) return;

            // Check if player is protected by team settings.
            if (owner.noPlayerDamage && evt.getEntity() instanceof EntityPlayer)
            {
                evt.setCanceled(true);
                return;
            }

            // check if entity is protected by team
            if (owner.protected_mobs.contains(evt.getEntity().getUniqueID()))
            {
                evt.setCanceled(true);
                return;
            }
        }

        @SubscribeEvent(priority = EventPriority.HIGHEST)
        public void spawn(LivingSpawnEvent.SpecialSpawn evt)
        {
            if (!ConfigManager.INSTANCE.landEnabled) return;
            if (evt.getEntity().getEntityWorld().isRemote) return;
            Coordinate c = Coordinate.getChunkCoordFromWorldCoord(evt.getEntity().getPosition(),
                    evt.getEntity().getEntityWorld().provider.getDimension());
            LandTeam owner = LandManager.getInstance().getLandOwner(c);
            if (owner == null) return;
            if (owner.noMobSpawn)
            {
                evt.setResult(Result.DENY);
                return;
            }
        }

        @SubscribeEvent(priority = EventPriority.HIGHEST)
        public void spawn(LivingSpawnEvent.CheckSpawn evt)
        {
            if (evt.getEntity().getEntityWorld().isRemote) return;
            Coordinate c = Coordinate.getChunkCoordFromWorldCoord(evt.getEntity().getPosition(),
                    evt.getEntity().getEntityWorld().provider.getDimension());
            LandTeam owner = LandManager.getInstance().getLandOwner(c);
            if (owner == null || !ConfigManager.INSTANCE.landEnabled) return;
            if (owner.noMobSpawn)
            {
                evt.setResult(Result.DENY);
                return;
            }
        }

    }

    public static class InteractEventHandler
    {
        @SubscribeEvent(priority = EventPriority.HIGHEST)
        public void interact(PlayerInteractEvent.LeftClickBlock evt)
        {
            if (evt.getEntity().getEntityWorld().isRemote) return;
            if (!ConfigManager.INSTANCE.landEnabled) return;
            Coordinate c = Coordinate.getChunkCoordFromWorldCoord(evt.getPos(),
                    evt.getEntityPlayer().getEntityWorld().provider.getDimension());
            LandTeam owner = LandManager.getInstance().getLandOwner(c);
            if (owner == null) return;
            Block block = evt.getWorld().getBlockState(evt.getPos()).getBlock();
            String name = block.getRegistryName().toString();
            if (blockBreakWhiteList.contains(name)) { return; }
            if (LandManager.owns(evt.getEntityPlayer(), c)) { return; }
            Coordinate blockLoc = new Coordinate(evt.getPos(),
                    evt.getEntityPlayer().getEntityWorld().provider.getDimension());
            if (!LandManager.getInstance().isPublic(blockLoc, owner))
            {
                evt.setUseBlock(Result.DENY);
                evt.setCanceled(true);
                if (!evt.getWorld().isRemote) evt.getEntity().sendMessage(getDenyMessage(owner));
            }
            evt.setUseItem(Result.DENY);
        }

        @SubscribeEvent(priority = EventPriority.HIGHEST)
        public void interact(PlayerInteractEvent.EntityInteract evt)
        {
            if (evt.getSide() == Side.CLIENT) return;
            if (!ConfigManager.INSTANCE.landEnabled) return;
            Coordinate c = Coordinate.getChunkCoordFromWorldCoord(evt.getPos(),
                    evt.getEntityPlayer().getEntityWorld().provider.getDimension());
            LandTeam owner = LandManager.getInstance().getLandOwner(c);
            if (owner == null) return;
            // If the player owns it, they can toggle whether the entity is
            // protected or not.
            if (LandManager.owns(evt.getEntityPlayer(), c))
            {
                // No protecting players.
                if (evt.getTarget() instanceof EntityPlayer) return;

                // check if player is holding a public toggle.
                if (!evt.getWorld().isRemote && evt.getItemStack() != null
                        && evt.getItemStack().getDisplayName().equals("Public Toggle")
                        && evt.getEntityPlayer().isSneaking())
                {
                    // If so, toggle whether the entity is public.
                    if (owner.public_mobs.contains(evt.getTarget().getUniqueID()))
                    {
                        evt.getEntityPlayer().sendMessage(
                                new TextComponentString("Removed from public: " + evt.getTarget().getName()));
                        LandManager.getInstance().toggleMobPublic(evt.getTarget().getUniqueID(), owner);
                    }
                    else
                    {
                        evt.getEntityPlayer()
                                .sendMessage(new TextComponentString("Added to Public: " + evt.getTarget().getName()));
                        LandManager.getInstance().toggleMobPublic(evt.getTarget().getUniqueID(), owner);
                    }
                    evt.setCanceled(true);
                    return;
                }
                // check if player is holding a protect toggle.
                if (!evt.getWorld().isRemote && evt.getItemStack() != null
                        && evt.getItemStack().getDisplayName().equals("Protect Toggle")
                        && evt.getEntityPlayer().isSneaking()
                        && PermissionAPI.hasPermission(evt.getEntityPlayer(), PERMPROTECTMOB))
                {
                    // If so, toggle whether the entity is protected.
                    if (owner.protected_mobs.contains(evt.getTarget().getUniqueID()))
                    {
                        evt.getEntityPlayer().sendMessage(
                                new TextComponentString("Removed from protected: " + evt.getTarget().getName()));
                        LandManager.getInstance().toggleMobProtect(evt.getTarget().getUniqueID(), owner);
                    }
                    else
                    {
                        evt.getEntityPlayer().sendMessage(
                                new TextComponentString("Added to protected: " + evt.getTarget().getName()));
                        LandManager.getInstance().toggleMobProtect(evt.getTarget().getUniqueID(), owner);
                    }
                    evt.setCanceled(true);
                }
            }

            // Check the teams relations settings
            if (owner.canUseStuff(evt.getEntityPlayer().getUniqueID())) return;

            // If all public, don't bother checking things below.
            if (owner.allPublic) return;

            // If not public, no use of mob.
            if (!owner.public_mobs.contains(evt.getTarget().getUniqueID()))
            {
                evt.setCanceled(true);
                return;
            }
        }

        @SubscribeEvent(priority = EventPriority.HIGHEST)
        public void interact(PlayerInteractEvent.RightClickItem evt)
        {
            if (evt.getSide() == Side.CLIENT) return;
            if (evt.getItemStack().getItem() instanceof ItemFood || evt.getItemStack().getItem() == Items.WRITTEN_BOOK
                    || evt.getItemStack().getItem() == Items.WRITABLE_BOOK || !ConfigManager.INSTANCE.landEnabled
                    || evt.getEntity().world.isRemote)
                return;

            EntityPlayer player = evt.getEntityPlayer();
            String name = evt.getItemStack().getItem().getRegistryName().toString();

            // Check global config for whitelisted items.
            if (itemUseWhitelist.contains(name)) { return; }
            // Check if any mods decide that the item should be whitelisted
            // regardless of team.
            if (MinecraftForge.EVENT_BUS.post(
                    new DenyItemUseEvent(evt.getEntity(), evt.getItemStack(), UseType.RIGHTCLICKBLOCK))) { return; }

            Coordinate c = Coordinate.getChunkCoordFromWorldCoord(evt.getPos(),
                    player.getEntityWorld().provider.getDimension());
            boolean ownedLand = LandManager.getInstance().isOwned(c);
            if (!ownedLand)
            {
                if (PermissionAPI.hasPermission(player, PERMUSEITEMWILD)) { return; }
                // TODO better message.
                player.sendMessage(new TextComponentString("Cannot use that."));
                evt.setCanceled(true);
                ((EntityPlayerMP) player).sendAllContents(player.inventoryContainer,
                        player.inventoryContainer.inventoryItemStacks);
                return;

            }
            LandTeam team = LandManager.getInstance().getLandOwner(c);
            boolean owns = LandManager.owns(player, c);

            // check permission
            String perm = owns ? PERMUSEITEMOWN : PERMUSEITEMOTHER;
            boolean permission = PermissionAPI.hasPermission(player, perm);
            if (!permission)
            {
                player.sendMessage(getDenyMessage(team));
                evt.setResult(Result.DENY);
                evt.setCanceled(true);
                ((EntityPlayerMP) player).sendAllContents(player.inventoryContainer,
                        player.inventoryContainer.inventoryItemStacks);
                return;
            }
            // Allow use if public block.
            Coordinate blockLoc = new Coordinate(evt.getPos(), player.getEntityWorld().provider.getDimension());
            if (LandManager.getInstance().isPublic(blockLoc, team))
            {
                evt.setResult(Result.DENY);
                return;
            }
            // Check team relations
            if (!team.canUseStuff(player.getUniqueID()))
            {
                player.sendMessage(getDenyMessage(team));
                evt.setResult(Result.DENY);
                evt.setCanceled(true);
                ((EntityPlayerMP) player).sendAllContents(player.inventoryContainer,
                        player.inventoryContainer.inventoryItemStacks);
            }
        }

        @SubscribeEvent(priority = EventPriority.HIGHEST)
        public void interact(PlayerInteractEvent.RightClickBlock evt)
        {
            if (evt.getSide() == Side.CLIENT) return;
            if (!ConfigManager.INSTANCE.landEnabled) return;
            Coordinate c = Coordinate.getChunkCoordFromWorldCoord(evt.getPos(),
                    evt.getEntityPlayer().getEntityWorld().provider.getDimension());
            LandTeam owner = LandManager.getInstance().getLandOwner(c);

            EntityPlayer player = evt.getEntityPlayer();
            if (owner == null)
            {
                if (!PermissionAPI.hasPermission(player, PERMUSEBLOCKWILD))
                {
                    // TODO better message.
                    player.sendMessage(new TextComponentString("Cannot use that."));
                    evt.setCanceled(true);
                    evt.setUseBlock(Result.DENY);
                    evt.setUseItem(Result.DENY);
                    ((EntityPlayerMP) player).sendAllContents(player.inventoryContainer,
                            player.inventoryContainer.inventoryItemStacks);
                    return;
                }
                return;
            }

            Block block = null;
            IBlockState state = evt.getWorld().getBlockState(evt.getPos());
            block = state.getBlock();
            String name = block.getRegistryName().toString();
            if (blockUseWhiteList.contains(name)) { return; }
            boolean b = true;
            boolean shouldPass = true;

            // Check permission
            boolean owns = LandManager.owns(evt.getEntityPlayer(), c);
            String perm = owns ? PERMUSEBLOCKOWN : PERMUSEBLOCKOTHER;
            boolean permission = PermissionAPI.hasPermission(player, perm);
            if (!permission)
            {
                player.sendMessage(getDenyMessage(LandManager.getInstance().getLandOwner(c)));
                evt.setCanceled(true);
                evt.setUseBlock(Result.DENY);
                evt.setUseItem(Result.DENY);
                ((EntityPlayerMP) player).sendAllContents(player.inventoryContainer,
                        player.inventoryContainer.inventoryItemStacks);
                return;
            }

            // Do stuff for toggling public
            if (owns)
            {
                if (!evt.getWorld().isRemote && evt.getItemStack() != null
                        && evt.getItemStack().getDisplayName().equals("Public Toggle")
                        && evt.getEntityPlayer().isSneaking())
                {
                    if (!owner.allPublic && LandManager.getInstance().isAdmin(evt.getEntityPlayer().getUniqueID()))
                    {
                        Coordinate blockLoc = new Coordinate(evt.getPos(),
                                evt.getEntityPlayer().getEntityWorld().provider.getDimension());
                        if (LandManager.getInstance().isPublic(blockLoc, owner))
                        {
                            evt.getEntityPlayer().sendMessage(new TextComponentString("Set Block to Team Only"));
                            LandManager.getInstance().unsetPublic(blockLoc);
                        }
                        else
                        {
                            evt.getEntityPlayer().sendMessage(new TextComponentString("Set Block to Public Use"));
                            LandManager.getInstance().setPublic(blockLoc, owner);
                        }
                        evt.setCanceled(true);
                    }
                }
                return;
            }

            // Check if the block has some custom interaction event, if so, move
            // on
            // through to that if the block is whitelisted.
            if (!(block.hasTileEntity(state)) && !evt.getWorld().isRemote)
            {
                shouldPass = MinecraftForge.EVENT_BUS
                        .post(new DenyItemUseEvent(evt.getEntity(), evt.getItemStack(), UseType.RIGHTCLICKBLOCK));
                name = evt.getItemStack().getItem().getRegistryName().toString();
                shouldPass = shouldPass || itemUseWhitelist.contains(name);
                if (shouldPass)
                {
                    BlockPos pos = evt.getPos();
                    Vec3d vec = evt.getHitVec();
                    if (vec == null) vec = new Vec3d(0, 0, 0);
                    b = CompatWrapper.interactWithBlock(block, evt.getWorld(), pos, state, evt.getEntityPlayer(),
                            evt.getHand(), null, evt.getFace(), (float) vec.x, (float) vec.y, (float) vec.z);
                }
                if (!b && shouldPass) return;
            }

            // Check if the block is public.
            Coordinate blockLoc = new Coordinate(evt.getPos(),
                    evt.getEntityPlayer().getEntityWorld().provider.getDimension());
            boolean freeuse = LandManager.getInstance().isPublic(blockLoc, owner);
            if (freeuse) return;

            // Check team relations
            if (!owner.canUseStuff(player.getUniqueID()))
            {
                player.sendMessage(getDenyMessage(owner));
                evt.setCanceled(true);
                evt.setUseBlock(Result.DENY);
                evt.setUseItem(Result.DENY);
                ((EntityPlayerMP) player).sendAllContents(player.inventoryContainer,
                        player.inventoryContainer.inventoryItemStacks);
            }
        }
    }

    public static boolean sameTeam(Entity a, Entity b)
    {
        return LandManager.getTeam(a) == LandManager.getTeam(b);
    }

    public static final String     PERMBREAKWILD        = "thutessentials.land.break.unowned";
    public static final String     PERMBREAKOWN         = "thutessentials.land.break.owned.self";
    public static final String     PERMBREAKOTHER       = "thutessentials.land.break.owned.other";

    public static final String     PERMPLACEWILD        = "thutessentials.land.place.unowned";
    public static final String     PERMPLACEOWN         = "thutessentials.land.place.owned.self";
    public static final String     PERMPLACEOTHER       = "thutessentials.land.place.owned.other";

    public static final String     PERMUSEITEMWILD      = "thutessentials.land.useitem.unowned";
    public static final String     PERMUSEITEMOWN       = "thutessentials.land.useitem.owned.self";
    public static final String     PERMUSEITEMOTHER     = "thutessentials.land.useitem.owned.other";

    public static final String     PERMUSEBLOCKWILD     = "thutessentials.land.useblock.unowned";
    public static final String     PERMUSEBLOCKOWN      = "thutessentials.land.useblock.owned.self";
    public static final String     PERMUSEBLOCKOTHER    = "thutessentials.land.useblock.owned.other";

    public static final String     PERMENTERWILD        = "thutessentials.land.enter.unowned";
    public static final String     PERMENTEROWN         = "thutessentials.land.enter.owned.self";
    public static final String     PERMENTEROTHER       = "thutessentials.land.enter.owned.other";

    public static final String     PERMCREATETEAM       = "thutessentials.teams.create";
    public static final String     PERMJOINTEAMINVITED  = "thutessentials.teams.join.invite";
    public static final String     PERMJOINTEAMNOINVITE = "thutessentials.teams.join.force";

    public static final String     PERMPROTECTMOB       = "thutessentials.teams.protect.mob";

    public static final String     PERMUNCLAIMOTHER     = "thutessentials.land.unclaim.owned.other";

    static Map<UUID, Long>         lastLeaveMessage     = Maps.newHashMap();
    static Map<UUID, Long>         lastEnterMessage     = Maps.newHashMap();

    private boolean                registered           = false;
    protected InteractEventHandler interact_handler     = new InteractEventHandler();
    protected EntityEventHandler   entity_handler       = new EntityEventHandler();
    protected BlockEventHandler    block_handler        = new BlockEventHandler();

    public LandEventsHandler()
    {
    }

    public void registerPerms()
    {
        if (registered) return;
        registered = true;
        PermissionAPI.registerNode(PERMBREAKWILD, DefaultPermissionLevel.ALL,
                "Can the player break blocks in unowned land.");
        PermissionAPI.registerNode(PERMBREAKOWN, DefaultPermissionLevel.ALL,
                "Can the player break blocks in their own land.");
        PermissionAPI.registerNode(PERMBREAKOTHER, DefaultPermissionLevel.OP,
                "Can the player break blocks in other player's land.");

        PermissionAPI.registerNode(PERMPLACEWILD, DefaultPermissionLevel.ALL,
                "Can the player place blocks in unowned land.");
        PermissionAPI.registerNode(PERMPLACEOWN, DefaultPermissionLevel.ALL,
                "Can the player place blocks in their own land.");
        PermissionAPI.registerNode(PERMPLACEOTHER, DefaultPermissionLevel.OP,
                "Can the player place blocks in other player's land.");

        PermissionAPI.registerNode(PERMUSEITEMWILD, DefaultPermissionLevel.ALL,
                "Can the player use items in unowned land.");
        PermissionAPI.registerNode(PERMUSEITEMOWN, DefaultPermissionLevel.ALL,
                "Can the player use items in their own land.");
        PermissionAPI.registerNode(PERMUSEITEMOTHER, DefaultPermissionLevel.OP,
                "Can the player use items in other player's land.");

        PermissionAPI.registerNode(PERMUSEBLOCKWILD, DefaultPermissionLevel.ALL,
                "Can the player use items in unowned land.");
        PermissionAPI.registerNode(PERMUSEBLOCKOWN, DefaultPermissionLevel.ALL,
                "Can the player use items in their own land.");
        PermissionAPI.registerNode(PERMUSEBLOCKOTHER, DefaultPermissionLevel.OP,
                "Can the player use items in other player's land.");

        PermissionAPI.registerNode(PERMENTERWILD, DefaultPermissionLevel.ALL, "Can the player enter unowned land.");
        PermissionAPI.registerNode(PERMENTEROWN, DefaultPermissionLevel.ALL, "Can the player enter their own land.");
        PermissionAPI.registerNode(PERMENTEROTHER, DefaultPermissionLevel.ALL,
                "Can the player enter other player's land.");

        PermissionAPI.registerNode(PERMCREATETEAM, DefaultPermissionLevel.ALL, "Can the player create a team.");
        PermissionAPI.registerNode(PERMJOINTEAMINVITED, DefaultPermissionLevel.ALL,
                "Can the player join a team with an invite.");
        PermissionAPI.registerNode(PERMJOINTEAMNOINVITE, DefaultPermissionLevel.OP,
                "Can the player join a team without an invite.");

        PermissionAPI.registerNode(PERMPROTECTMOB, DefaultPermissionLevel.ALL,
                "Can the player protect mobs in their team's land.");

        PermissionAPI.registerNode(PERMUNCLAIMOTHER, DefaultPermissionLevel.OP, "Can the player unclaim any land.");

    }

    @SubscribeEvent
    public void login(PlayerLoggedInEvent evt)
    {
        EntityPlayer entityPlayer = evt.player;
        LandManager.getTeam(entityPlayer);
    }

    @SubscribeEvent
    public void detonate(ExplosionEvent.Detonate evt)
    {
        if (evt.getWorld().isRemote) return;
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
