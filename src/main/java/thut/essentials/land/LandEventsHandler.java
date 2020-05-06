package thut.essentials.land;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.mojang.authlib.GameProfile;

import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.INPC;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.item.ItemFrameEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.network.IPacket;
import net.minecraft.network.play.server.SSpawnParticlePacket;
import net.minecraft.particles.IParticleData;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.EntityRayTraceResult;
import net.minecraft.util.math.RayTraceResult.Type;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.dimension.Dimension;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.TickEvent.ServerTickEvent;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingSpawnEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.FillBucketEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.event.world.BlockEvent.BreakEvent;
import net.minecraftforge.event.world.BlockEvent.EntityPlaceEvent;
import net.minecraftforge.event.world.BlockEvent.FarmlandTrampleEvent;
import net.minecraftforge.event.world.ExplosionEvent;
import net.minecraftforge.eventbus.api.Event.Result;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.LogicalSidedProvider;
import net.minecraftforge.server.permission.DefaultPermissionLevel;
import net.minecraftforge.server.permission.PermissionAPI;
import thut.essentials.Essentials;
import thut.essentials.commands.CommandManager;
import thut.essentials.events.DenyItemUseEvent;
import thut.essentials.events.DenyItemUseEvent.UseType;
import thut.essentials.land.LandManager.LandTeam;
import thut.essentials.util.Coordinate;
import thut.essentials.util.OwnerManager;
import thut.essentials.util.PlayerDataHandler;

public class LandEventsHandler
{
    public static enum DenyReason
    {
        NONE, WILD, OTHER, OURS;

        public boolean test()
        {
            return this == NONE;
        }
    }

    public static final LandEventsHandler TEAMMANAGER = new LandEventsHandler();

    public static Set<ResourceLocation> mobUseWhitelist     = Sets.newHashSet();
    public static Set<ResourceLocation> itemUseWhitelist    = Sets.newHashSet();
    public static Set<ResourceLocation> blockUseWhiteList   = Sets.newHashSet();
    public static Set<ResourceLocation> blockBreakWhiteList = Sets.newHashSet();
    public static Set<ResourceLocation> blockPlaceWhiteList = Sets.newHashSet();

    public static Set<ResourceLocation> invuln = Sets.newHashSet();

    public static void init()
    {
        MinecraftForge.EVENT_BUS.unregister(LandEventsHandler.TEAMMANAGER);
        MinecraftForge.EVENT_BUS.unregister(LandEventsHandler.TEAMMANAGER.interact_handler);
        MinecraftForge.EVENT_BUS.unregister(LandEventsHandler.TEAMMANAGER.entity_handler);
        MinecraftForge.EVENT_BUS.unregister(LandEventsHandler.TEAMMANAGER.block_handler);
        LandEventsHandler.itemUseWhitelist.clear();
        for (final String s : Essentials.config.itemUseWhitelist)
            LandEventsHandler.itemUseWhitelist.add(new ResourceLocation(s));
        LandEventsHandler.blockUseWhiteList.clear();
        for (final String s : Essentials.config.blockUseWhitelist)
            LandEventsHandler.blockUseWhiteList.add(new ResourceLocation(s));
        LandEventsHandler.blockBreakWhiteList.clear();
        for (final String s : Essentials.config.blockBreakWhitelist)
            LandEventsHandler.blockBreakWhiteList.add(new ResourceLocation(s));
        LandEventsHandler.blockPlaceWhiteList.clear();
        for (final String s : Essentials.config.blockPlaceWhitelist)
            LandEventsHandler.blockPlaceWhiteList.add(new ResourceLocation(s));
        for (final String s : Essentials.config.mobUseWhitelist)
            LandEventsHandler.mobUseWhitelist.add(new ResourceLocation(s));
        MinecraftForge.EVENT_BUS.register(LandEventsHandler.TEAMMANAGER);
        MinecraftForge.EVENT_BUS.register(LandEventsHandler.TEAMMANAGER.interact_handler);
        MinecraftForge.EVENT_BUS.register(LandEventsHandler.TEAMMANAGER.entity_handler);
        MinecraftForge.EVENT_BUS.register(LandEventsHandler.TEAMMANAGER.block_handler);
        LandEventsHandler.invuln.clear();
        for (final String s : Essentials.config.invulnMobs)
            LandEventsHandler.invuln.add(new ResourceLocation(s));
    }

    private static boolean isPublicToggle(final ItemStack stack)
    {
        return stack.getDisplayName().getUnformattedComponentText().equalsIgnoreCase("public toggle");
    }

    private static boolean isProtectToggle(final ItemStack stack)
    {
        return stack.getDisplayName().getUnformattedComponentText().equalsIgnoreCase("protect toggle");
    }

    private static boolean isBreakToggle(final ItemStack stack)
    {
        return stack.getDisplayName().getUnformattedComponentText().equalsIgnoreCase("break toggle");
    }

    private static boolean isPlaceToggle(final ItemStack stack)
    {
        return stack.getDisplayName().getUnformattedComponentText().equalsIgnoreCase("place toggle");
    }

    public static class BlockEventHandler
    {
        public void checkPlace(final BlockEvent evt, final PlayerEntity player)
        {
            if (!(player instanceof ServerPlayerEntity)) return;
            // check whitelist first.
            if (LandEventsHandler.blockPlaceWhiteList.contains(evt.getWorld().getBlockState(evt.getPos()).getBlock()
                    .getRegistryName())) return;
            // Chunk Coordinate
            final Coordinate c = Coordinate.getChunkCoordFromWorldCoord(evt.getPos(), player.dimension.getId());
            // Block coordinate
            final Coordinate b = new Coordinate(evt.getPos(), player.dimension.getId());
            final LandTeam team = LandManager.getInstance().getLandOwner(c);

            // Check permission for breaking wilderness, then return.
            if (team == null)
            {
                if (PermissionAPI.hasPermission(player, LandEventsHandler.PERMPLACEWILD)) return;
                player.sendMessage(CommandManager.makeFormattedComponent("msg.team.nowildperms.placeblock"));
                evt.setCanceled(true);
                ((ServerPlayerEntity) player).sendAllContents(player.container, player.container.getInventory());
                return;

            }
            final boolean isFakePlayer = player instanceof FakePlayer;
            // Check if the team allows fakeplayers
            if (team.fakePlayers && isFakePlayer)
            {

            }
            else // Otherwise check normal behaviour
            {
                // Treat relation place perm as owning the land.
                final boolean owns = team.canPlaceBlock(player.getUniqueID(), b);
                if (owns && !PermissionAPI.hasPermission(player, LandEventsHandler.PERMPLACEOWN))
                {
                    evt.setCanceled(true);
                    if (!isFakePlayer)
                    {
                        LandEventsHandler.sendMessage(player, team, LandEventsHandler.DENY);
                        ((ServerPlayerEntity) player).sendAllContents(player.container, player.container
                                .getInventory());
                    }
                    return;
                }
                if (!owns && !PermissionAPI.hasPermission(player, LandEventsHandler.PERMPLACEOTHER))
                {
                    evt.setCanceled(true);
                    if (!isFakePlayer)
                    {
                        LandEventsHandler.sendMessage(player, team, LandEventsHandler.DENY);
                        ((ServerPlayerEntity) player).sendAllContents(player.container, player.container
                                .getInventory());
                    }
                    return;
                }
            }
        }

        public void checkBreak(final BlockEvent evt, final PlayerEntity player)
        {
            if (Essentials.config.landEnabled && player != null)
            {
                // check whitelist first.
                if (LandEventsHandler.blockBreakWhiteList.contains(evt.getWorld().getBlockState(evt.getPos()).getBlock()
                        .getRegistryName())) return;
                // Chunk Coordinate
                final Coordinate c = Coordinate.getChunkCoordFromWorldCoord(evt.getPos(), player.dimension.getId());
                // Block coordinate
                final Coordinate b = new Coordinate(evt.getPos(), player.dimension.getId());
                final LandTeam team = LandManager.getInstance().getLandOwner(c);

                // Check permission for breaking wilderness, then return.
                if (team == null)
                {
                    if (PermissionAPI.hasPermission(player, LandEventsHandler.PERMBREAKWILD)) return;
                    player.sendMessage(CommandManager.makeFormattedComponent("msg.team.nowildperms.breakblock"));
                    evt.setCanceled(true);
                    return;

                }
                // Check if the team allows fakeplayers
                if (team.fakePlayers && player instanceof FakePlayer)
                {

                }
                else // Otherwise check normal behaviour
                {
                    // Treat relation break perm as owning the land.
                    final boolean owns = team.canBreakBlock(player.getUniqueID(), b);
                    if (owns && !PermissionAPI.hasPermission(player, LandEventsHandler.PERMBREAKOWN))
                    {
                        LandEventsHandler.sendMessage(player, team, LandEventsHandler.DENY);
                        evt.setCanceled(true);
                        return;
                    }
                    if (!owns && !PermissionAPI.hasPermission(player, LandEventsHandler.PERMBREAKOTHER))
                    {
                        LandEventsHandler.sendMessage(player, team, LandEventsHandler.DENY);
                        evt.setCanceled(true);
                        return;
                    }
                }
            }
        }

        @SubscribeEvent(priority = EventPriority.HIGHEST)
        public void trample(final FarmlandTrampleEvent evt)
        {
            if (evt.getEntity().getEntityWorld().isRemote) return;
            if (!Essentials.config.landEnabled) return;
            final Coordinate c = Coordinate.getChunkCoordFromWorldCoord(evt.getPos(), evt.getWorld().getDimension());
            final Entity trampler = evt.getEntity();
            final LandTeam team = LandManager.getInstance().getLandOwner(c);
            if (team == null) return;
            PlayerEntity player = null;
            if (trampler instanceof PlayerEntity) player = (PlayerEntity) trampler;
            LivingEntity test;
            if ((test = OwnerManager.OWNERCHECK.getOwner(trampler)) instanceof PlayerEntity)
                player = (PlayerEntity) test;
            this.checkBreak(evt, player);
            if (!evt.isCanceled() && Essentials.config.log_interactions) Essentials.LOGGER.trace(c + " trample " + evt
                    .getPos() + " " + trampler.getUniqueID() + " " + trampler.getName().getFormattedText());
        }

        @SubscribeEvent(priority = EventPriority.HIGHEST)
        public void placeBlocks(final EntityPlaceEvent evt)
        {
            if (!(evt.getEntity() instanceof ServerPlayerEntity)) return;
            if (!Essentials.config.landEnabled) return;
            this.checkPlace(evt, (PlayerEntity) evt.getEntity());
            if (!evt.isCanceled() && Essentials.config.log_interactions)
            {
                final Coordinate c = Coordinate.getChunkCoordFromWorldCoord(evt.getPos(), evt.getWorld()
                        .getDimension());
                Essentials.LOGGER.trace(c + " place " + evt.getPos() + " " + evt.getPlacedAgainst() + " " + evt
                        .getPlacedBlock() + " " + evt.getEntity().getUniqueID() + " " + evt.getEntity().getName()
                                .getFormattedText());
            }
        }

        @SubscribeEvent(priority = EventPriority.HIGHEST)
        public void BreakBlock(final BreakEvent evt)
        {
            if (!(evt.getPlayer() instanceof ServerPlayerEntity)) return;
            if (!Essentials.config.landEnabled) return;
            final PlayerEntity player = evt.getPlayer();
            this.checkBreak(evt, player);
            if (!evt.isCanceled() && Essentials.config.log_interactions)
            {
                final Coordinate c = Coordinate.getChunkCoordFromWorldCoord(evt.getPos(), evt.getWorld()
                        .getDimension());
                Essentials.LOGGER.trace(c + " break " + evt.getPos() + " " + evt.getState() + " " + evt.getPlayer()
                        .getUniqueID() + " " + evt.getPlayer().getName().getFormattedText());
            }
        }

        @SubscribeEvent(priority = EventPriority.HIGHEST)
        public void bucket(final FillBucketEvent event)
        {
            if (event.getPlayer().getEntityWorld().isRemote) return;
            if (!Essentials.config.landEnabled) return;
            BlockPos pos = event.getPlayer().getPosition();
            if (event.getTarget() instanceof BlockRayTraceResult && event.getTarget().getType() != Type.MISS)
            {
                final BlockRayTraceResult trace = (BlockRayTraceResult) event.getTarget();
                pos = trace.getPos().offset(trace.getFace());
            }
            final PlayerEntity player = event.getPlayer();
            final BlockEvent evt = new BreakEvent(event.getWorld(), pos, event.getWorld().getBlockState(pos), player);
            this.checkPlace(evt, player);
            this.checkBreak(evt, player);
            if (evt.isCanceled()) event.setCanceled(true);
            else if (Essentials.config.log_interactions)
            {
                final Coordinate c = Coordinate.getChunkCoordFromWorldCoord(evt.getPos(), evt.getWorld()
                        .getDimension());
                Essentials.LOGGER.trace(c + " bucket " + evt.getPos() + " " + player.getUniqueID() + " " + player
                        .getName().getFormattedText() + " " + event.getFilledBucket() + " " + event.getEmptyBucket());
            }
        }
    }

    public static class EntityEventHandler
    {
        public static Set<UUID> showLandSet = Sets.newHashSet();

        private boolean canTakeDamage(final Entity in, final LandTeam land_owner)
        {
            if (land_owner == null)
            {
                if (in != null && LandEventsHandler.invuln.contains(in.getType().getRegistryName())) return false;
                return true;
            }
            if (in instanceof ServerPlayerEntity) return !land_owner.noPlayerDamage;
            if (in instanceof INPC) return !land_owner.noNPCDamage;
            if (in instanceof ItemFrameEntity) return !land_owner.protectFrames;
            return true;
        }

        private void sendNearbyChunks(final ServerPlayerEntity player)
        {
            final IParticleData otherowned = ParticleTypes.BARRIER;
            final IParticleData owned = ParticleTypes.HAPPY_VILLAGER;
            IParticleData show = null;
            final LandTeam us = LandManager.getTeam(player);
            int x, y, z;
            int x1, y1, z1;
            IPacket<?> packet;
            final int dim = player.dimension.getId();
            for (int i = -3; i <= 3; i++)
                for (int j = -3; j <= 3; j++)
                    for (int k = -3; k <= 3; k++)
                    {
                        x = player.chunkCoordX + i;
                        y = player.chunkCoordY + j;
                        z = player.chunkCoordZ + k;
                        final Coordinate c = new Coordinate(x, y, z, dim);

                        final LandTeam team = LandManager.getInstance().getLandOwner(c);
                        show = team == null ? null : team == us ? owned : otherowned;

                        if (show != null && y >= 0 && y < 16)
                        {
                            x1 = x * 16;
                            y1 = y * 16;
                            z1 = z * 16;

                            for (int i1 = 1; i1 < 16; i1 += 4)
                                for (int j1 = 1; j1 < 16; j1 += 4)
                                {
                                    packet = new SSpawnParticlePacket(show, false, x1 + i1, y1 + j1, z1 + 1, 0, 0, 0, 0,
                                            1);
                                    player.connection.sendPacket(packet);
                                    packet = new SSpawnParticlePacket(show, false, x1 + i1, y1 + j1, z1 + 15, 0, 0, 0,
                                            0, 1);
                                    player.connection.sendPacket(packet);
                                    packet = new SSpawnParticlePacket(show, false, x1 + 1, y1 + j1, z1 + i1, 0, 0, 0, 0,
                                            1);
                                    player.connection.sendPacket(packet);
                                    packet = new SSpawnParticlePacket(show, false, x1 + 15, y1 + j1, z1 + i1, 0, 0, 0,
                                            0, 1);
                                    player.connection.sendPacket(packet);
                                }
                        }
                    }

        }

        @SubscribeEvent(priority = EventPriority.HIGHEST)
        public void death(final LivingDeathEvent evt)
        {
            if (evt.getEntity().getEntityWorld().isRemote) return;
            if (!Essentials.config.landEnabled) return;

            // Cleanup the entity from protected mobs.
            final UUID id = evt.getEntity().getUniqueID();
            if (LandManager.getInstance()._protected_mobs.containsKey(id))
            {
                final LandTeam team = LandManager.getInstance()._protected_mobs.remove(id);
                team.protected_mobs.remove(id);
                LandSaveHandler.saveTeam(team.teamName);
            }

            // Cleanup the entity from public mobs.
            if (LandManager.getInstance()._public_mobs.containsKey(id))
            {
                final LandTeam team = LandManager.getInstance()._public_mobs.remove(id);
                team.public_mobs.remove(id);
                LandSaveHandler.saveTeam(team.teamName);
            }
        }

        @SubscribeEvent
        public void update(final LivingUpdateEvent evt)
        {
            if (evt.getEntity().getEntityWorld().isRemote) return;
            if (!Essentials.config.landEnabled) return;
            if (evt.getEntityLiving() instanceof ServerPlayerEntity && evt.getEntityLiving().ticksExisted > 10)
            {
                final ServerPlayerEntity player = (ServerPlayerEntity) evt.getEntityLiving();
                if (EntityEventHandler.showLandSet.contains(player.getUniqueID()) && player.ticksExisted % 20 == 0) this
                        .sendNearbyChunks(player);
                BlockPos here;
                BlockPos old;
                here = new BlockPos(player.chasingPosX, player.chasingPosY, player.chasingPosZ);
                old = new BlockPos(player.prevChasingPosX, player.prevChasingPosY, player.prevChasingPosZ);

                if (old.equals(here)) return;

                final Coordinate newChunk = Coordinate.getChunkCoordFromWorldCoord(here, player.dimension.getId());
                final Coordinate oldChunk = Coordinate.getChunkCoordFromWorldCoord(old, player.getEntityWorld()
                        .getDimension());
                final boolean isNewOwned = LandManager.getInstance().isOwned(newChunk);
                final boolean isOldOwned = LandManager.getInstance().isOwned(oldChunk);

                final CompoundNBT tag = PlayerDataHandler.getCustomDataTag(player);
                final CompoundNBT entry_log = tag.getCompound("last_entered_chunk");
                BlockPos entry_point = old;
                Coordinate last_chunk = oldChunk;

                if (!(isNewOwned || isOldOwned))
                {
                    entry_point = old;
                    entry_log.put("from", NBTUtil.writeBlockPos(entry_point));
                    final BlockPos p = new BlockPos(newChunk.x, newChunk.y, newChunk.z);
                    final CompoundNBT prev_chunk = NBTUtil.writeBlockPos(p);
                    prev_chunk.putInt("W", newChunk.dim);
                    entry_log.put("chunk", prev_chunk);
                    tag.put("last_entered_chunk", entry_log);
                    PlayerDataHandler.saveCustomData(player);
                }
                else
                {
                    final LandTeam team = LandManager.getInstance().getLandOwner(newChunk);
                    final LandTeam team1 = LandManager.getInstance().getLandOwner(oldChunk);

                    if (!entry_log.isEmpty())
                    {
                        entry_point = NBTUtil.readBlockPos(entry_log.getCompound("from"));
                        final CompoundNBT prev_chunk = entry_log.getCompound("chunk");
                        last_chunk = new Coordinate(NBTUtil.readBlockPos(prev_chunk), prev_chunk.getInt("W"));
                    }

                    if (!LandEventsHandler.lastLeaveMessage.containsKey(evt.getEntity().getUniqueID()))
                        LandEventsHandler.lastLeaveMessage.put(evt.getEntity().getUniqueID(), System.currentTimeMillis()
                                - 1);
                    if (!LandEventsHandler.lastEnterMessage.containsKey(evt.getEntity().getUniqueID()))
                        LandEventsHandler.lastEnterMessage.put(evt.getEntity().getUniqueID(), System.currentTimeMillis()
                                - 1);

                    ITextComponent message = null;

                    final boolean owns = team != null && team.isMember(player);
                    if (!isNewOwned && !PermissionAPI.hasPermission(player, LandEventsHandler.PERMENTERWILD))
                        message = CommandManager.makeFormattedComponent("msg.team.nowildperms.noenter");
                    else if (isNewOwned && owns && !PermissionAPI.hasPermission(player, LandEventsHandler.PERMENTEROWN))
                        message = CommandManager.makeFormattedComponent("msg.team.owned.noenter");
                    else if (isNewOwned && !owns && !PermissionAPI.hasPermission(player,
                            LandEventsHandler.PERMENTEROTHER)) message = CommandManager.makeFormattedComponent(
                                    "msg.team.other.noenter");
                    if (message != null)
                    {
                        if (!last_chunk.equals(newChunk))
                        {
                            entry_point = old;
                            // entry_log.putString("last_name", last_name);
                            entry_log.put("from", NBTUtil.writeBlockPos(entry_point));
                            final BlockPos p = new BlockPos(newChunk.x, newChunk.y, newChunk.z);
                            final CompoundNBT prev_chunk = NBTUtil.writeBlockPos(p);
                            prev_chunk.putInt("W", newChunk.dim);
                            entry_log.put("chunk", prev_chunk);
                            tag.put("last_entered_chunk", entry_log);
                            PlayerDataHandler.saveCustomData(player);
                        }
                        final Vec3d oldV = new Vec3d(entry_point);
                        player.connection.setPlayerLocation(oldV.x + 0.5, oldV.y + 0.5, oldV.z + 0.5,
                                player.rotationYaw, player.rotationPitch);
                        player.sendMessage(message);
                        return;
                    }

                    // If we got to here, it means this is a valid location, so
                    // lets save it and current team.
                    // entry_log.putString("last_name", last_name);
                    entry_log.put("from", NBTUtil.writeBlockPos(here));
                    tag.put("last_entered_chunk", entry_log);
                    PlayerDataHandler.saveCustomData(player);

                    messages:
                    {
                        if (team != null)
                        {
                            if (team.equals(team1)) break messages;
                            if (team1 != null)
                            {
                                final long last = LandEventsHandler.lastLeaveMessage.get(evt.getEntity().getUniqueID());
                                if (last < System.currentTimeMillis())
                                {
                                    LandEventsHandler.sendMessage(player, team1, LandEventsHandler.EXIT);
                                    LandEventsHandler.lastLeaveMessage.put(evt.getEntity().getUniqueID(), System
                                            .currentTimeMillis() + 100);
                                }
                            }
                            final long last = LandEventsHandler.lastEnterMessage.get(evt.getEntity().getUniqueID());
                            if (last < System.currentTimeMillis())
                            {
                                LandEventsHandler.sendMessage(player, team, LandEventsHandler.ENTER);
                                LandEventsHandler.lastLeaveMessage.put(evt.getEntity().getUniqueID(), System
                                        .currentTimeMillis() + 100);
                            }
                        }
                        else
                        {
                            final long last = LandEventsHandler.lastLeaveMessage.get(evt.getEntity().getUniqueID());
                            if (last < System.currentTimeMillis())
                            {
                                LandEventsHandler.sendMessage(player, team1, LandEventsHandler.EXIT);
                                LandEventsHandler.lastLeaveMessage.put(evt.getEntity().getUniqueID(), System
                                        .currentTimeMillis() + 100);
                            }
                        }
                    }
                }
            }
        }

        @SubscribeEvent(priority = EventPriority.HIGHEST)
        public void attack(final AttackEntityEvent evt)
        {
            if (evt.getEntity().getEntityWorld().isRemote || !Essentials.config.landEnabled) return;
            final Coordinate c = Coordinate.getChunkCoordFromWorldCoord(evt.getTarget().getPosition(), evt
                    .getPlayer().dimension.getId());
            final LandTeam owner = LandManager.getInstance().getLandOwner(c);

            if (!this.canTakeDamage(evt.getTarget(), owner))
            {
                evt.setCanceled(true);
                return;
            }

            // TODO possible perms for attacking things in unclaimed land?
            if (owner == null) return;

            final PlayerEntity attacker = evt.getPlayer();

            // Check if the team allows fakeplayers
            if (owner.fakePlayers && evt.getPlayer() instanceof FakePlayer) return;

            final BlockPos pos = evt.getTarget().getPosition();
            final Coordinate b = Coordinate.getChunkCoordFromWorldCoord(pos.getX(), pos.getY(), pos.getZ(),
                    attacker.dimension);

            // Check if item frame
            if (evt.getTarget() instanceof ItemFrameEntity && !owner.canBreakBlock(attacker.getUniqueID(), b))
            {
                evt.setCanceled(true);
                return;
            }

            // If mob is protected, do not allow the attack, even if by owner.
            if (owner.protected_mobs.contains(evt.getTarget().getUniqueID()))
            {
                evt.setCanceled(true);
                return;
            }
        }

        @SubscribeEvent(priority = EventPriority.HIGHEST)
        public void attack(final LivingAttackEvent evt)
        {
            if (evt.getEntity().getEntityWorld().isRemote) return;
            if (!Essentials.config.landEnabled) return;
            final Coordinate c = Coordinate.getChunkCoordFromWorldCoord(evt.getEntity().getPosition(), evt
                    .getEntity().dimension.getId());
            final LandTeam owner = LandManager.getInstance().getLandOwner(c);

            if (!this.canTakeDamage(evt.getEntity(), owner))
            {
                evt.setCanceled(true);
                return;
            }

            // TODO maybe add a perm for combat in non-claimed land?
            if (owner == null) return;

            if (evt.getEntity() instanceof PlayerEntity)
            {
                final LandTeam players = LandManager.getTeam(evt.getEntity());
                // Check if player is protected via friendly fire settings.
                if (!players.friendlyFire)
                {
                    final Entity damageSource = evt.getSource().getTrueSource();
                    if (damageSource instanceof PlayerEntity && LandEventsHandler.sameTeam(damageSource, evt
                            .getEntity()))
                    {
                        evt.setCanceled(true);
                        return;
                    }
                }
            }

            // Check if the team allows fakeplayers
            if (owner.fakePlayers && evt.getSource().getTrueSource() instanceof FakePlayer) return;

            // check if entity is protected by team
            if (owner.protected_mobs.contains(evt.getEntity().getUniqueID()))
            {
                evt.setCanceled(true);
                return;
            }
        }

        @SubscribeEvent
        public void projectileImpact(final ProjectileImpactEvent evt)
        {
            if (evt.getEntity().getEntityWorld().isRemote) return;
            if (!Essentials.config.landEnabled) return;
            if (evt.getRayTraceResult().getType() == Type.MISS) return;
            if (!(evt.getRayTraceResult() instanceof EntityRayTraceResult)) return;
            final EntityRayTraceResult hit = (EntityRayTraceResult) evt.getRayTraceResult();

            final Entity target = hit.getEntity();

            final Coordinate c = Coordinate.getChunkCoordFromWorldCoord(target.getPosition(), evt.getEntity().dimension
                    .getId());
            final LandTeam owner = LandManager.getInstance().getLandOwner(c);

            if (!this.canTakeDamage(target, owner))
            {
                evt.setCanceled(true);
                return;
            }

            // TODO maybe add a perm for combat in non-claimed land?
            if (owner == null) return;

            // check if entity is protected by team
            if (owner.protected_mobs.contains(target.getUniqueID()))
            {
                evt.setCanceled(true);
                return;
            }

        }

        @SubscribeEvent(priority = EventPriority.HIGHEST)
        public void attack(final LivingHurtEvent evt)
        {
            if (evt.getEntity().getEntityWorld().isRemote) return;
            if (!Essentials.config.landEnabled) return;
            final Coordinate c = Coordinate.getChunkCoordFromWorldCoord(evt.getEntity().getPosition(), evt
                    .getEntity().dimension.getId());
            final LandTeam owner = LandManager.getInstance().getLandOwner(c);

            if (!this.canTakeDamage(evt.getEntity(), owner))
            {
                evt.setCanceled(true);
                return;
            }

            // TODO maybe add a perm for combat in non-claimed land?
            if (owner == null) return;

            // check if entity is protected by team
            if (owner.protected_mobs.contains(evt.getEntity().getUniqueID()))
            {
                evt.setCanceled(true);
                return;
            }
        }

        @SubscribeEvent(priority = EventPriority.HIGHEST)
        public void spawn(final LivingSpawnEvent.SpecialSpawn evt)
        {
            if (!Essentials.config.landEnabled) return;
            if (evt.getEntity().getEntityWorld().isRemote) return;
            final Coordinate c = Coordinate.getChunkCoordFromWorldCoord(evt.getEntity().getPosition(), evt
                    .getEntity().dimension.getId());
            final LandTeam owner = LandManager.getInstance().getLandOwner(c);
            if (owner == null) return;
            if (owner.noMobSpawn)
            {
                evt.setResult(Result.DENY);
                return;
            }
        }

        @SubscribeEvent(priority = EventPriority.HIGHEST)
        public void spawn(final LivingSpawnEvent.CheckSpawn evt)
        {
            if (!Essentials.config.landEnabled) return;
            if (evt.getEntity().getEntityWorld().isRemote) return;
            final Coordinate c = Coordinate.getChunkCoordFromWorldCoord(evt.getEntity().getPosition(), evt
                    .getEntity().dimension.getId());
            final LandTeam owner = LandManager.getInstance().getLandOwner(c);
            if (owner == null) return;
            if (owner.noMobSpawn)
            {
                evt.setResult(Result.DENY);
                return;
            }
        }

    }

    public static class InteractEventHandler
    {
        private DenyReason canUseBlock(final PlayerInteractEvent evt)
        {
            final BlockState state = evt.getWorld().getBlockState(evt.getPos());
            if (LandEventsHandler.blockUseWhiteList.contains(state.getBlock().getRegistryName()))
                return DenyReason.NONE;

            final ServerPlayerEntity player = (ServerPlayerEntity) evt.getPlayer();
            // Chunk Coordinate
            final Coordinate c = Coordinate.getChunkCoordFromWorldCoord(evt.getPos(), evt.getPlayer().dimension
                    .getId());
            // Block coordinate
            final Coordinate b = new Coordinate(evt.getPos(), evt.getPlayer().getEntityWorld().getDimension());
            final LandTeam owner = LandManager.getInstance().getLandOwner(c);
            if (owner == null)
            {
                if (PermissionAPI.hasPermission(player, LandEventsHandler.PERMUSEBLOCKWILD)) return DenyReason.NONE;
                return DenyReason.WILD;
            }
            final boolean isFakePlayer = player instanceof FakePlayer;
            // Check if the team allows fakeplayers
            if (owner.fakePlayers && isFakePlayer) return DenyReason.NONE;
            else // Otherwise check normal behaviour
            {
                // Treat relation place perm as owning the land.
                final boolean owns = owner.canUseStuff(player.getUniqueID(), b);
                if (owns && !PermissionAPI.hasPermission(player, LandEventsHandler.PERMUSEBLOCKOWN))
                    return DenyReason.OURS;
                if (!owns && !PermissionAPI.hasPermission(player, LandEventsHandler.PERMUSEBLOCKOTHER))
                    return DenyReason.OTHER;
            }
            return DenyReason.NONE;
        }

        private DenyReason canUseItem(final PlayerInteractEvent evt)
        {
            // Check our global whitelist
            if (LandEventsHandler.itemUseWhitelist.contains(evt.getItemStack().getItem().getRegistryName()))
                return DenyReason.NONE;

            // Check our specific event allowances
            if (MinecraftForge.EVENT_BUS.post(new DenyItemUseEvent(evt.getEntity(), evt.getItemStack(),
                    UseType.RIGHTCLICKBLOCK))) return DenyReason.NONE;

            final ServerPlayerEntity player = (ServerPlayerEntity) evt.getPlayer();
            // Chunk Coordinate
            final Coordinate c = Coordinate.getChunkCoordFromWorldCoord(evt.getPos(), evt.getPlayer().dimension
                    .getId());
            // Block coordinate
            final Coordinate b = new Coordinate(evt.getPos(), evt.getPlayer().getEntityWorld().getDimension());
            final LandTeam owner = LandManager.getInstance().getLandOwner(c);
            if (owner == null)
            {
                if (PermissionAPI.hasPermission(player, LandEventsHandler.PERMUSEITEMWILD)) return DenyReason.NONE;
                return DenyReason.WILD;
            }
            final boolean isFakePlayer = player instanceof FakePlayer;
            // Check if the team allows fakeplayers
            if (owner.fakePlayers && isFakePlayer) return DenyReason.NONE;
            else // Otherwise check normal behaviour
            {
                // Treat relation place perm as owning the land.
                final boolean owns = owner.canUseStuff(player.getUniqueID(), b);
                if (owns && !PermissionAPI.hasPermission(player, LandEventsHandler.PERMUSEITEMOWN))
                    return DenyReason.OURS;
                if (!owns && !PermissionAPI.hasPermission(player, LandEventsHandler.PERMUSEITEMOTHER))
                    return DenyReason.OTHER;
            }
            return DenyReason.NONE;
        }

        private DenyReason canUseMob(final PlayerInteractEvent evt, final Entity mob)
        {
            // Check our global whitelist
            if (LandEventsHandler.mobUseWhitelist.contains(mob.getType().getRegistryName())) return DenyReason.NONE;

            // Check our specific event allowances
            if (MinecraftForge.EVENT_BUS.post(new DenyItemUseEvent(evt.getEntity(), evt.getItemStack(),
                    UseType.RIGHTCLICKBLOCK))) return DenyReason.NONE;

            final ServerPlayerEntity player = (ServerPlayerEntity) evt.getPlayer();
            // Chunk Coordinate
            final Coordinate c = Coordinate.getChunkCoordFromWorldCoord(evt.getPos(), evt.getPlayer().dimension
                    .getId());
            // Block coordinate
            final Coordinate b = new Coordinate(evt.getPos(), evt.getPlayer().getEntityWorld().getDimension());
            final LandTeam owner = LandManager.getInstance().getLandOwner(c);
            if (owner == null)
            {
                if (PermissionAPI.hasPermission(player, LandEventsHandler.PERMUSEITEMWILD)) return DenyReason.NONE;
                return DenyReason.WILD;
            }
            final boolean isFakePlayer = player instanceof FakePlayer;
            // Check if the team allows fakeplayers
            if (owner.fakePlayers && isFakePlayer) return DenyReason.NONE;
            else // Otherwise check normal behaviour
            {
                // Treat relation place perm as owning the land.
                final boolean owns = owner.canUseStuff(player.getUniqueID(), b);
                if (owns && !PermissionAPI.hasPermission(player, LandEventsHandler.PERMUSEITEMOWN))
                    return DenyReason.OURS;
                if (!owns && !PermissionAPI.hasPermission(player, LandEventsHandler.PERMUSEITEMOTHER))
                    return DenyReason.OTHER;
            }
            return DenyReason.NONE;
        }

        @SubscribeEvent(priority = EventPriority.HIGHEST)
        public void interact(final PlayerInteractEvent.LeftClickBlock evt)
        {
            if (!(evt.getPlayer() instanceof ServerPlayerEntity)) return;
            if (!Essentials.config.landEnabled) return;
            final DenyReason rsult = this.canUseBlock(evt);
            // First check if we do not have permission to act here.
            if (!rsult.test())
            {
                // Chunk Coordinate
                final Coordinate c = Coordinate.getChunkCoordFromWorldCoord(evt.getPos(), evt.getPlayer().dimension
                        .getId());
                final boolean isFakePlayer = evt.getPlayer() instanceof FakePlayer;
                if (!isFakePlayer)
                {
                    final ServerPlayerEntity player = (ServerPlayerEntity) evt.getPlayer();
                    switch (rsult)
                    {
                    case OTHER:
                    case OURS:
                        final LandTeam owner = LandManager.getInstance().getLandOwner(c);
                        LandEventsHandler.sendMessage(player, owner, LandEventsHandler.DENY);
                        break;
                    case WILD:
                        player.sendMessage(CommandManager.makeFormattedComponent("msg.team.nowildperms.useblock"));
                        break;
                    default:
                        break;
                    }
                    player.sendAllContents(player.container, player.container.getInventory());
                }
                if (Essentials.config.log_interactions) Essentials.LOGGER.trace(
                        "Cancelled interact due to not allowed to left click that." + c + " " + evt.getPlayer()
                                .getUniqueID() + " " + evt.getPlayer().getName().getFormattedText());
                evt.setCanceled(true);
                evt.setUseBlock(Result.DENY);
                evt.setUseItem(Result.DENY);
                return;
            }

        }

        @SubscribeEvent(priority = EventPriority.HIGHEST)
        public void interact(final PlayerInteractEvent.EntityInteract evt)
        {
            if (evt.getSide() == LogicalSide.CLIENT) return;
            if (!Essentials.config.landEnabled) return;
            final DenyReason rsult = this.canUseMob(evt, evt.getTarget());
            // First check if we do not have permission to act here.
            if (!rsult.test())
            {
                // Chunk Coordinate
                final Coordinate c = Coordinate.getChunkCoordFromWorldCoord(evt.getPos(), evt.getPlayer().dimension
                        .getId());
                final boolean isFakePlayer = evt.getPlayer() instanceof FakePlayer;
                if (!isFakePlayer)
                {
                    final ServerPlayerEntity player = (ServerPlayerEntity) evt.getPlayer();
                    switch (rsult)
                    {
                    case OTHER:
                    case OURS:
                        final LandTeam owner = LandManager.getInstance().getLandOwner(c);
                        LandEventsHandler.sendMessage(player, owner, LandEventsHandler.DENY);
                        break;
                    case WILD:
                        player.sendMessage(CommandManager.makeFormattedComponent("msg.team.nowildperms.usemob"));
                        break;
                    default:
                        break;
                    }
                    player.sendAllContents(player.container, player.container.getInventory());
                }
                if (Essentials.config.log_interactions) Essentials.LOGGER.trace(
                        "Cancelled interact due to not allowed to use that mob." + c + " " + evt.getPlayer()
                                .getUniqueID() + " " + evt.getPlayer().getName().getFormattedText());
                evt.setCanceled(true);
                evt.setCancellationResult(ActionResultType.FAIL);
                return;
            }

            // Chunk Coordinate
            final Coordinate c = Coordinate.getChunkCoordFromWorldCoord(evt.getPos(), evt.getPlayer().dimension
                    .getId());
            final LandTeam owner = LandManager.getInstance().getLandOwner(c);
            if (owner == null) return;

            // Not letting these manage this stuff.
            if (evt.getPlayer() instanceof FakePlayer) return;

            // If the player owns it, they can toggle whether the entity is
            // protected or not, Only team admins can do this.
            if (owner.isAdmin(evt.getPlayer()))
            {
                // No protecting players.
                if (evt.getTarget() instanceof PlayerEntity) return;

                // check if player is holding a public toggle.
                if (!evt.getWorld().isRemote && LandEventsHandler.isPublicToggle(evt.getItemStack()) && evt.getPlayer()
                        .isCrouching())
                {
                    // If so, toggle whether the entity is public.
                    final UUID id = evt.getTarget().getUniqueID();
                    final boolean isPublic = owner.public_mobs.contains(id);
                    LandManager.getInstance().toggleMobPublic(id, owner);
                    evt.getPlayer().sendMessage(Essentials.config.getMessage("msg.team.setmob.public." + !isPublic));
                    evt.setCanceled(true);
                    if (Essentials.config.log_interactions) Essentials.LOGGER.trace(
                            "Cancelled interact due to toggling public mob." + c + " " + evt.getPlayer().getUniqueID()
                                    + " " + evt.getPlayer().getName().getFormattedText());
                    return;
                }
                // check if player is holding a protect toggle.
                if (!evt.getWorld().isRemote && LandEventsHandler.isProtectToggle(evt.getItemStack()) && evt.getPlayer()
                        .isCrouching() && PermissionAPI.hasPermission(evt.getPlayer(), LandEventsHandler.PERMPROTECTMOB))
                {
                    // If so, toggle whether the entity is protected.
                    final UUID id = evt.getTarget().getUniqueID();
                    final boolean isPublic = owner.protected_mobs.contains(id);
                    LandManager.getInstance().toggleMobProtect(id, owner);
                    evt.getPlayer().sendMessage(Essentials.config.getMessage("msg.team.setmob.protect." + !isPublic));
                    evt.setCanceled(true);
                    if (Essentials.config.log_interactions) Essentials.LOGGER.trace(
                            "Cancelled interact due to toggling protected mob." + c + " " + evt.getPlayer()
                                    .getUniqueID() + " " + evt.getPlayer().getName().getFormattedText());
                    return;
                }
            }
        }

        @SubscribeEvent(priority = EventPriority.HIGHEST)
        public void interact(final PlayerInteractEvent.RightClickItem evt)
        {
            if (!(evt.getPlayer() instanceof ServerPlayerEntity)) return;
            if (evt.getItemStack().getItem().isFood() || evt.getItemStack().getItem() == Items.WRITTEN_BOOK || evt
                    .getItemStack().getItem() == Items.WRITABLE_BOOK || !Essentials.config.landEnabled || evt
                            .getEntity().world.isRemote) return;

            final DenyReason rsult = this.canUseItem(evt);
            // First check if we do not have permission to act here.
            if (!rsult.test())
            {
                // Chunk Coordinate
                final Coordinate c = Coordinate.getChunkCoordFromWorldCoord(evt.getPos(), evt.getPlayer().dimension
                        .getId());
                final boolean isFakePlayer = evt.getPlayer() instanceof FakePlayer;
                if (!isFakePlayer)
                {
                    final ServerPlayerEntity player = (ServerPlayerEntity) evt.getPlayer();
                    switch (rsult)
                    {
                    case OTHER:
                    case OURS:
                        final LandTeam owner = LandManager.getInstance().getLandOwner(c);
                        LandEventsHandler.sendMessage(player, owner, LandEventsHandler.DENY);
                        break;
                    case WILD:
                        player.sendMessage(CommandManager.makeFormattedComponent("msg.team.nowildperms.useblock"));
                        break;
                    default:
                        break;
                    }
                    player.sendAllContents(player.container, player.container.getInventory());
                }
                if (Essentials.config.log_interactions) Essentials.LOGGER.trace(
                        "Cancelled interact due to not allowed to right click with that." + c + " " + evt.getPlayer()
                                .getUniqueID() + " " + evt.getPlayer().getName().getFormattedText());
                evt.setCancellationResult(ActionResultType.FAIL);
                evt.setCanceled(true);
                return;
            }
        }

        @SubscribeEvent(priority = EventPriority.HIGHEST)
        public void interact(final PlayerInteractEvent.RightClickBlock evt)
        {
            if (!(evt.getPlayer() instanceof ServerPlayerEntity)) return;
            if (!Essentials.config.landEnabled) return;
            // Chunk Coordinate
            final Coordinate c = Coordinate.getChunkCoordFromWorldCoord(evt.getPos(), evt.getPlayer().dimension
                    .getId());
            // Block coordinate
            final Coordinate b = new Coordinate(evt.getPos(), evt.getPlayer().getEntityWorld().getDimension());
            final LandTeam owner = LandManager.getInstance().getLandOwner(c);
            final DenyReason rsult = this.canUseBlock(evt);
            // First check if we do not have permission to act here.
            if (!rsult.test())
            {
                final boolean isFakePlayer = evt.getPlayer() instanceof FakePlayer;
                if (!isFakePlayer)
                {
                    final ServerPlayerEntity player = (ServerPlayerEntity) evt.getPlayer();

                    switch (rsult)
                    {
                    case OTHER:
                    case OURS:
                        LandEventsHandler.sendMessage(player, owner, LandEventsHandler.DENY);
                        break;
                    case WILD:
                        player.sendMessage(CommandManager.makeFormattedComponent("msg.team.nowildperms.useblock"));
                        break;
                    default:
                        break;
                    }
                    player.sendAllContents(player.container, player.container.getInventory());
                }
                if (Essentials.config.log_interactions) Essentials.LOGGER.trace(
                        "Cancelled interact due to not allowed to right click that." + c + " " + evt.getPlayer()
                                .getUniqueID() + " " + evt.getPlayer().getName().getFormattedText());
                evt.setCanceled(true);
                evt.setUseBlock(Result.DENY);
                evt.setUseItem(Result.DENY);
                return;
            }
            // We don't care about anything else beyond here is unowned
            if (owner == null) return;

            final PlayerEntity player = evt.getPlayer();

            // Don't allow fake players to act below
            if (evt.getPlayer() instanceof FakePlayer) return;
            // Check permission, Treat relation public perm as if we own this
            // for this check.
            final boolean owns = owner.canUseStuff(player.getUniqueID(), b) || owner.canPlaceBlock(player.getUniqueID(),
                    b);
            // Check if the block is public.
            Coordinate blockLoc = new Coordinate(evt.getPos(), evt.getPlayer().getEntityWorld().getDimension());
            // If we own this, we can return here, first check public toggle
            // though.
            if (owns)
            {
                // Do stuff for toggling public
                if (!evt.getWorld().isRemote && LandEventsHandler.isPublicToggle(evt.getItemStack()) && evt.getPlayer()
                        .isCrouching() && !owner.allPublic && LandManager.getInstance().isAdmin(evt.getPlayer()
                                .getUniqueID()))
                {
                    blockLoc = new Coordinate(evt.getPos(), evt.getPlayer().getEntityWorld().getDimension());
                    final boolean isPublic = LandManager.getInstance().isPublic(blockLoc, owner);
                    if (isPublic) LandManager.getInstance().unsetPublic(blockLoc, owner);
                    else LandManager.getInstance().setPublic(blockLoc, owner);
                    evt.getPlayer().sendMessage(Essentials.config.getMessage("msg.team.setpublic.block." + !isPublic));
                    evt.setCanceled(true);
                    if (Essentials.config.log_interactions) Essentials.LOGGER.trace(
                            "Cancelled interact due to public toggling. " + c + " " + evt.getPlayer().getUniqueID()
                                    + " " + evt.getPlayer().getName().getFormattedText());
                }
                // Do stuff for toggling break
                if (!evt.getWorld().isRemote && LandEventsHandler.isBreakToggle(evt.getItemStack()) && evt.getPlayer()
                        .isCrouching() && LandManager.getInstance().isAdmin(evt.getPlayer().getUniqueID()))
                {
                    blockLoc = new Coordinate(evt.getPos(), evt.getPlayer().getEntityWorld().getDimension());
                    final boolean isPublic = owner.anyBreakSet.contains(blockLoc);
                    if (owner.anyBreakSet.contains(blockLoc)) owner.anyBreakSet.remove(blockLoc);
                    else owner.anyBreakSet.add(blockLoc);
                    evt.getPlayer().sendMessage(Essentials.config.getMessage("msg.team.setbreak.block." + !isPublic));
                    LandSaveHandler.saveTeam(owner.teamName);
                    evt.setCanceled(true);
                    if (Essentials.config.log_interactions) Essentials.LOGGER.trace(
                            "Cancelled interact due to break toggling. " + c + " " + evt.getPlayer().getUniqueID() + " "
                                    + evt.getPlayer().getName().getFormattedText());
                }
                // Do stuff for toggling place
                if (!evt.getWorld().isRemote && LandEventsHandler.isPlaceToggle(evt.getItemStack()) && evt.getPlayer()
                        .isCrouching() && LandManager.getInstance().isAdmin(evt.getPlayer().getUniqueID()))
                {
                    blockLoc = new Coordinate(evt.getPos(), evt.getPlayer().getEntityWorld().getDimension());
                    final boolean isPublic = owner.anyPlaceSet.contains(blockLoc);
                    if (owner.anyPlaceSet.contains(blockLoc)) owner.anyPlaceSet.remove(blockLoc);
                    else owner.anyPlaceSet.add(blockLoc);
                    evt.getPlayer().sendMessage(Essentials.config.getMessage("msg.team.setplace.block." + !isPublic));
                    LandSaveHandler.saveTeam(owner.teamName);
                    evt.setCanceled(true);
                    if (Essentials.config.log_interactions) Essentials.LOGGER.trace(
                            "Cancelled interact due to place toggling. " + c + " " + evt.getPlayer().getUniqueID() + " "
                                    + evt.getPlayer().getName().getFormattedText());
                }
                return;
            }
        }
    }

    public static class ChunkLoadHandler
    {
        public static MinecraftServer server;

        public static boolean removeChunks(final Coordinate location)
        {
            if (!Essentials.config.chunkLoading) return false;
            final DimensionType dim = DimensionType.getById(location.dim);
            if (dim == null) return false;
            final ServerWorld world = ChunkLoadHandler.server.getWorld(dim);
            if (world == null) return false;
            world.getChunkProvider().forceChunk(new ChunkPos(location.x, location.z), false);
            return true;
        }

        public static boolean addChunks(final Coordinate location, final UUID placer)
        {
            if (!Essentials.config.chunkLoading) return false;
            final DimensionType dim = DimensionType.getById(location.dim);
            if (dim == null) return false;
            final ServerWorld world = ChunkLoadHandler.server.getWorld(dim);
            if (world == null) return false;
            world.getChunkProvider().forceChunk(new ChunkPos(location.x, location.z), true);
            return true;
        }
    }

    public static boolean sameTeam(final Entity a, final Entity b)
    {
        return LandManager.getTeam(a) == LandManager.getTeam(b);
    }

    public static final String PERMBREAKWILD  = "thutessentials.land.break.unowned";
    public static final String PERMBREAKOWN   = "thutessentials.land.break.owned.self";
    public static final String PERMBREAKOTHER = "thutessentials.land.break.owned.other";

    public static final String PERMPLACEWILD  = "thutessentials.land.place.unowned";
    public static final String PERMPLACEOWN   = "thutessentials.land.place.owned.self";
    public static final String PERMPLACEOTHER = "thutessentials.land.place.owned.other";

    public static final String PERMUSEITEMWILD  = "thutessentials.land.useitem.unowned";
    public static final String PERMUSEITEMOWN   = "thutessentials.land.useitem.owned.self";
    public static final String PERMUSEITEMOTHER = "thutessentials.land.useitem.owned.other";

    public static final String PERMUSEBLOCKWILD  = "thutessentials.land.useblock.unowned";
    public static final String PERMUSEBLOCKOWN   = "thutessentials.land.useblock.owned.self";
    public static final String PERMUSEBLOCKOTHER = "thutessentials.land.useblock.owned.other";

    public static final String PERMUSEMOBWILD  = "thutessentials.land.usemob.unowned";
    public static final String PERMUSEMOBOWN   = "thutessentials.land.usemob.owned.self";
    public static final String PERMUSEMOBOTHER = "thutessentials.land.usemob.owned.other";

    public static final String PERMENTERWILD  = "thutessentials.land.enter.unowned";
    public static final String PERMENTEROWN   = "thutessentials.land.enter.owned.self";
    public static final String PERMENTEROTHER = "thutessentials.land.enter.owned.other";

    public static final String PERMCREATETEAM       = "thutessentials.teams.create";
    public static final String PERMJOINTEAMINVITED  = "thutessentials.teams.join.invite";
    public static final String PERMJOINTEAMNOINVITE = "thutessentials.teams.join.force";

    public static final String PERMPROTECTMOB = "thutessentials.teams.protect.mob";

    public static final String PERMUNCLAIMOTHER = "thutessentials.land.unclaim.owned.other";

    static Map<UUID, Long> lastLeaveMessage = Maps.newHashMap();
    static Map<UUID, Long> lastEnterMessage = Maps.newHashMap();

    private boolean                registered       = false;
    protected InteractEventHandler interact_handler = new InteractEventHandler();
    protected EntityEventHandler   entity_handler   = new EntityEventHandler();
    protected BlockEventHandler    block_handler    = new BlockEventHandler();

    public Set<UUID>         checked = Sets.newHashSet();
    public List<GameProfile> toCheck = Lists.newArrayList();

    public LandEventsHandler()
    {
    }

    public void registerPerms()
    {
        if (this.registered) return;
        this.registered = true;
        PermissionAPI.registerNode(LandEventsHandler.PERMBREAKWILD, DefaultPermissionLevel.ALL,
                "Can the player break blocks in unowned land.");
        PermissionAPI.registerNode(LandEventsHandler.PERMBREAKOWN, DefaultPermissionLevel.ALL,
                "Can the player break blocks in their own land.");
        PermissionAPI.registerNode(LandEventsHandler.PERMBREAKOTHER, DefaultPermissionLevel.OP,
                "Can the player break blocks in other player's land.");

        PermissionAPI.registerNode(LandEventsHandler.PERMPLACEWILD, DefaultPermissionLevel.ALL,
                "Can the player place blocks in unowned land.");
        PermissionAPI.registerNode(LandEventsHandler.PERMPLACEOWN, DefaultPermissionLevel.ALL,
                "Can the player place blocks in their own land.");
        PermissionAPI.registerNode(LandEventsHandler.PERMPLACEOTHER, DefaultPermissionLevel.OP,
                "Can the player place blocks in other player's land.");

        PermissionAPI.registerNode(LandEventsHandler.PERMUSEITEMWILD, DefaultPermissionLevel.ALL,
                "Can the player use items in unowned land.");
        PermissionAPI.registerNode(LandEventsHandler.PERMUSEITEMOWN, DefaultPermissionLevel.ALL,
                "Can the player use items in their own land.");
        PermissionAPI.registerNode(LandEventsHandler.PERMUSEITEMOTHER, DefaultPermissionLevel.OP,
                "Can the player use items in other player's land.");

        PermissionAPI.registerNode(LandEventsHandler.PERMUSEBLOCKWILD, DefaultPermissionLevel.ALL,
                "Can the player use items in unowned land.");
        PermissionAPI.registerNode(LandEventsHandler.PERMUSEBLOCKOWN, DefaultPermissionLevel.ALL,
                "Can the player use items in their own land.");
        PermissionAPI.registerNode(LandEventsHandler.PERMUSEBLOCKOTHER, DefaultPermissionLevel.OP,
                "Can the player use items in other player's land.");

        PermissionAPI.registerNode(LandEventsHandler.PERMUSEMOBWILD, DefaultPermissionLevel.ALL,
                "Can the player interact with mobs in unowned land.");
        PermissionAPI.registerNode(LandEventsHandler.PERMUSEMOBOWN, DefaultPermissionLevel.ALL,
                "Can the player interact with mobs in their own land.");
        PermissionAPI.registerNode(LandEventsHandler.PERMUSEMOBOTHER, DefaultPermissionLevel.ALL,
                "Can the player interact with mobs in other player's land.");

        PermissionAPI.registerNode(LandEventsHandler.PERMENTERWILD, DefaultPermissionLevel.ALL,
                "Can the player enter unowned land.");
        PermissionAPI.registerNode(LandEventsHandler.PERMENTEROWN, DefaultPermissionLevel.ALL,
                "Can the player enter their own land.");
        PermissionAPI.registerNode(LandEventsHandler.PERMENTEROTHER, DefaultPermissionLevel.ALL,
                "Can the player enter other player's land.");

        PermissionAPI.registerNode(LandEventsHandler.PERMCREATETEAM, DefaultPermissionLevel.ALL,
                "Can the player create a team.");
        PermissionAPI.registerNode(LandEventsHandler.PERMJOINTEAMINVITED, DefaultPermissionLevel.ALL,
                "Can the player join a team with an invite.");
        PermissionAPI.registerNode(LandEventsHandler.PERMJOINTEAMNOINVITE, DefaultPermissionLevel.OP,
                "Can the player join a team without an invite.");

        PermissionAPI.registerNode(LandEventsHandler.PERMPROTECTMOB, DefaultPermissionLevel.ALL,
                "Can the player protect mobs in their team's land.");

        PermissionAPI.registerNode(LandEventsHandler.PERMUNCLAIMOTHER, DefaultPermissionLevel.OP,
                "Can the player unclaim any land.");

    }

    public void queueUpdate(final GameProfile profile)
    {
        if (profile.getId() == null) return;
        if (this.checked.contains(profile.getId())) return;
        this.toCheck.add(profile);
    }

    @SubscribeEvent
    public void tick(final ServerTickEvent event)
    {
        if (this.toCheck.isEmpty() || event.phase != Phase.END) return;
        final MinecraftServer server = LogicalSidedProvider.INSTANCE.get(LogicalSide.SERVER);
        if (server.getServerTime() % 200 != 0) return;
        GameProfile profile = this.toCheck.get(0);
        try
        {
            profile = server.getMinecraftSessionService().fillProfileProperties(profile, true);
            if (profile.getName() == null || profile.getId() == null) return;
            server.getPlayerProfileCache().addEntry(profile);
        }
        catch (final Exception e)
        {
            return;
        }
        this.toCheck.remove(0);
        if (profile.getId() != null) this.checked.add(profile.getId());
    }

    @SubscribeEvent
    public void login(final PlayerLoggedInEvent evt)
    {
        final PlayerEntity entityPlayer = evt.getPlayer();
        final LandTeam team = LandManager.getTeam(entityPlayer);
        final MinecraftServer server = LogicalSidedProvider.INSTANCE.get(LogicalSide.SERVER);
        team.lastSeen = server.getServerTime();
    }

    @SubscribeEvent
    public void logout(final PlayerLoggedOutEvent evt)
    {
        final PlayerEntity entityPlayer = evt.getPlayer();
        final LandTeam team = LandManager.getTeam(entityPlayer);
        final MinecraftServer server = LogicalSidedProvider.INSTANCE.get(LogicalSide.SERVER);
        team.lastSeen = server.getServerTime();
    }

    @SubscribeEvent
    public void detonate(final ExplosionEvent.Detonate evt)
    {
        if (evt.getWorld().isRemote) return;
        final List<BlockPos> toRemove = Lists.newArrayList();
        final boolean denyBlasts = Essentials.config.denyExplosions;
        if (Essentials.config.landEnabled)
        {
            final Dimension dimension = evt.getWorld().getDimension();
            for (final BlockPos pos : evt.getAffectedBlocks())
            {
                final Coordinate c = Coordinate.getChunkCoordFromWorldCoord(pos, dimension);
                final LandTeam owner = LandManager.getInstance().getLandOwner(c);
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

    private static final byte DENY  = 0;
    private static final byte ENTER = 1;
    private static final byte EXIT  = 2;

    private static Map<UUID, Long> denyFloodControl  = Maps.newHashMap();
    private static Map<UUID, Long> enterFloodControl = Maps.newHashMap();
    private static Map<UUID, Long> exitFloodControl  = Maps.newHashMap();

    private static long getTime(final Entity player)
    {
        return player.getServer().getWorld(DimensionType.OVERWORLD).getGameTime();
    }

    private static void sendMessage(final Entity player, final LandTeam team, final byte index)
    {
        ITextComponent message = null;
        final long time = LandEventsHandler.getTime(player);
        final int delay = 10;
        switch (index)
        {
        case DENY:
            message = LandEventsHandler.getDenyMessage(team);
            if (LandEventsHandler.denyFloodControl.getOrDefault(player.getUniqueID(), (long) 0) > time) message = null;
            else LandEventsHandler.denyFloodControl.put(player.getUniqueID(), time + delay);
            break;
        case ENTER:
            message = LandEventsHandler.getEnterMessage(team);
            if (LandEventsHandler.enterFloodControl.getOrDefault(player.getUniqueID(), (long) 0) > time) message = null;
            else LandEventsHandler.enterFloodControl.put(player.getUniqueID(), time + delay);
            break;
        case EXIT:
            message = LandEventsHandler.getExitMessage(team);
            if (LandEventsHandler.exitFloodControl.getOrDefault(player.getUniqueID(), (long) 0) > time) message = null;
            else LandEventsHandler.exitFloodControl.put(player.getUniqueID(), time + delay);
            break;
        }
        if (message != null) player.sendMessage(message);
    }

    private static ITextComponent getDenyMessage(final LandTeam team)
    {
        if (team != null && !team.denyMessage.isEmpty()) return new StringTextComponent(team.denyMessage);
        if (!Essentials.config.defaultMessages) return null;
        return CommandManager.makeFormattedComponent("msg.team.deny", null, false, team.teamName);
    }

    private static ITextComponent getEnterMessage(final LandTeam team)
    {
        if (team != null && !team.enterMessage.isEmpty()) return new StringTextComponent(team.enterMessage);
        if (!Essentials.config.defaultMessages) return null;
        return CommandManager.makeFormattedComponent("msg.team.enterLand", null, false, team.teamName);
    }

    private static ITextComponent getExitMessage(final LandTeam team)
    {
        if (team != null && !team.exitMessage.isEmpty()) return new StringTextComponent(team.exitMessage);
        if (!Essentials.config.defaultMessages) return null;
        return CommandManager.makeFormattedComponent("msg.team.exitLand", null, false, team.teamName);
    }
}
