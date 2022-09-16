package thut.essentials.land;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.mojang.authlib.GameProfile;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.npc.Npc;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.TickEvent.ServerTickEvent;
import net.minecraftforge.event.entity.EntityMobGriefingEvent;
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
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.event.world.BlockEvent.BreakEvent;
import net.minecraftforge.event.world.BlockEvent.EntityPlaceEvent;
import net.minecraftforge.event.world.BlockEvent.FarmlandTrampleEvent;
import net.minecraftforge.event.world.ExplosionEvent;
import net.minecraftforge.eventbus.api.Event.Result;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import thut.essentials.Essentials;
import thut.essentials.commands.CommandManager;
import thut.essentials.events.DenyItemUseEvent;
import thut.essentials.events.DenyItemUseEvent.UseType;
import thut.essentials.land.LandManager.KGobalPos;
import thut.essentials.land.LandManager.LandTeam;
import thut.essentials.util.ChatHelper;
import thut.essentials.util.CoordinateUtls;
import thut.essentials.util.InventoryLogger;
import thut.essentials.util.ItemList;
import thut.essentials.util.MobManager;
import thut.essentials.util.OwnerManager;
import thut.essentials.util.PermNodes;
import thut.essentials.util.PermNodes.DefaultPermissionLevel;
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

    public static Set<ResourceLocation> mobUseWhitelist = Sets.newHashSet();
    public static Set<ResourceLocation> itemUseWhitelist = Sets.newHashSet();
    public static Set<ResourceLocation> blockUseWhiteList = Sets.newHashSet();
    public static Set<ResourceLocation> blockBreakWhiteList = Sets.newHashSet();
    public static Set<ResourceLocation> blockPlaceWhiteList = Sets.newHashSet();

    public static Set<ResourceLocation> invuln = Sets.newHashSet();

    public static Map<ResourceLocation, String[]> customMobPerms = Maps.newHashMap();

    public static final String[] defaultMobPerms =
    { LandEventsHandler.PERMUSEMOBWILD, LandEventsHandler.PERMUSEMOBOWN, LandEventsHandler.PERMUSEMOBOTHER };

    public static void init()
    {
        MinecraftForge.EVENT_BUS.unregister(LandEventsHandler.TEAMMANAGER);
        MinecraftForge.EVENT_BUS.unregister(LandEventsHandler.TEAMMANAGER.interact_handler);
        MinecraftForge.EVENT_BUS.unregister(LandEventsHandler.TEAMMANAGER.entity_handler);
        MinecraftForge.EVENT_BUS.unregister(LandEventsHandler.TEAMMANAGER.block_handler);

        if (!Essentials.config.landEnabled) return;

        LandEventsHandler.customMobPerms.clear();
        for (final String s : Essentials.config.customMobUsePerms)
        {
            final String[] args = s.split("->");
            final String[] perms = args[1].split(",");
            LandEventsHandler.customMobPerms.put(new ResourceLocation(args[0]), perms);
        }

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
        for (final String s : Essentials.config.invulnMobs) LandEventsHandler.invuln.add(new ResourceLocation(s));
    }

    private static boolean isPublicToggle(final ItemStack stack)
    {
        return stack.getHoverName().getString().equalsIgnoreCase("public toggle");
    }

    private static boolean isProtectToggle(final ItemStack stack)
    {
        return stack.getHoverName().getString().equalsIgnoreCase("protect toggle");
    }

    private static boolean isBreakToggle(final ItemStack stack)
    {
        return stack.getHoverName().getString().equalsIgnoreCase("break toggle");
    }

    private static boolean isPlaceToggle(final ItemStack stack)
    {
        return stack.getHoverName().getString().equalsIgnoreCase("place toggle");
    }

    public static class BlockEventHandler
    {
        public void checkPlace(final BlockEvent evt, final ServerPlayer player)
        {
            if (!(player instanceof ServerPlayer)) return;
            // check whitelist first.
            if (LandEventsHandler.blockPlaceWhiteList
                    .contains(evt.getWorld().getBlockState(evt.getPos()).getBlock().getRegistryName()))
                return;

            final Level world = player.getCommandSenderWorld();
            // Block coordinate
            final KGobalPos b = KGobalPos.getPosition(player.getCommandSenderWorld().dimension(), evt.getPos());
            final LandTeam team = LandManager.getInstance().getLandOwner(world, evt.getPos());

            // Check permission for breaking wilderness, then return.
            if (team == null)
            {
                if (PermNodes.getBooleanPerm(player, LandEventsHandler.PERMPLACEWILD)) return;
                ChatHelper.sendSystemMessage(player,
                        CommandManager.makeFormattedComponent("msg.team.nowildperms.placeblock"));
                evt.setCanceled(true);
                player.inventoryMenu.sendAllDataToRemote();
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
                final boolean owns = team.canPlaceBlock(player.getUUID(), b);
                if (owns && !PermNodes.getBooleanPerm(player, LandEventsHandler.PERMPLACEOWN))
                {
                    evt.setCanceled(true);
                    if (!isFakePlayer)
                    {
                        LandEventsHandler.sendMessage(player, team, LandEventsHandler.DENY);
                        player.inventoryMenu.sendAllDataToRemote();
                    }
                    return;
                }
                if (!owns && !PermNodes.getBooleanPerm(player, LandEventsHandler.PERMPLACEOTHER))
                {
                    evt.setCanceled(true);
                    if (!isFakePlayer)
                    {
                        LandEventsHandler.sendMessage(player, team, LandEventsHandler.DENY);
                        player.inventoryMenu.sendAllDataToRemote();
                    }
                    return;
                }
            }
        }

        public void checkBreak(final BlockEvent evt, final ServerPlayer player)
        {
            if (Essentials.config.landEnabled && player != null)
            {
                // check whitelist first.
                if (LandEventsHandler.blockBreakWhiteList
                        .contains(evt.getWorld().getBlockState(evt.getPos()).getBlock().getRegistryName()))
                    return;

                final Level world = player.getCommandSenderWorld();
                // Block coordinate
                final KGobalPos b = KGobalPos.getPosition(player.getCommandSenderWorld().dimension(), evt.getPos());
                final LandTeam team = LandManager.getInstance().getLandOwner(world, evt.getPos());

                // Check permission for breaking wilderness, then return.
                if (team == null)
                {
                    if (PermNodes.getBooleanPerm(player, LandEventsHandler.PERMBREAKWILD)) return;
                    ChatHelper.sendSystemMessage(player,
                            CommandManager.makeFormattedComponent("msg.team.nowildperms.breakblock"));
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
                    final boolean owns = team.canBreakBlock(player.getUUID(), b);
                    if (owns && !PermNodes.getBooleanPerm(player, LandEventsHandler.PERMBREAKOWN))
                    {
                        LandEventsHandler.sendMessage(player, team, LandEventsHandler.DENY);
                        evt.setCanceled(true);
                        return;
                    }
                    if (!owns && !PermNodes.getBooleanPerm(player, LandEventsHandler.PERMBREAKOTHER))
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
            if (evt.getEntity().getCommandSenderWorld().isClientSide) return;
            if (!Essentials.config.landEnabled) return;
            // Block coordinate
            final KGobalPos b = KGobalPos.getPosition(evt.getEntity().getCommandSenderWorld().dimension(),
                    evt.getPos());
            // Chunk Coordinate
            final KGobalPos c = CoordinateUtls.chunkPos(b);
            final Entity trampler = evt.getEntity();
            final LandTeam team = LandManager.getInstance().getLandOwner(trampler.getCommandSenderWorld(),
                    evt.getPos());
            if (team == null) return;
            ServerPlayer player = null;
            if (trampler instanceof ServerPlayer) player = (ServerPlayer) trampler;
            LivingEntity test;
            if ((test = OwnerManager.OWNERCHECK.getOwner(trampler)) instanceof ServerPlayer)
                player = (ServerPlayer) test;
            this.checkBreak(evt, player);
            if (!evt.isCanceled() && Essentials.config.log_interactions) InventoryLogger.log("trample at {} by {} {}",
                    c, evt.getPos(), trampler.getUUID(), trampler.getName().getString());
        }

        @SubscribeEvent(priority = EventPriority.HIGHEST)
        public void placeBlocks(final EntityPlaceEvent evt)
        {
            if (!(evt.getEntity() instanceof ServerPlayer)) return;
            if (!Essentials.config.landEnabled) return;
            this.checkPlace(evt, (ServerPlayer) evt.getEntity());
            if (!evt.isCanceled() && Essentials.config.log_interactions)
            {
                // Block coordinate
                final KGobalPos b = KGobalPos.getPosition(evt.getEntity().getCommandSenderWorld().dimension(),
                        evt.getPos());
                // Chunk Coordinate
                final KGobalPos c = CoordinateUtls.chunkPos(b);
                InventoryLogger.log("place {} at {} on {} by {} {}", c, evt.getPlacedBlock(), evt.getPos(),
                        evt.getPlacedAgainst(), evt.getEntity().getUUID(), evt.getEntity().getName().getString());
            }
        }

        @SubscribeEvent(priority = EventPriority.HIGHEST)
        public void BreakBlock(final BreakEvent evt)
        {
            if (!(evt.getPlayer() instanceof ServerPlayer)) return;
            if (!Essentials.config.landEnabled) return;
            final Player player = evt.getPlayer();
            this.checkBreak(evt, (ServerPlayer) player);
            if (!evt.isCanceled() && Essentials.config.log_interactions)
            {
                // Block coordinate
                final KGobalPos b = KGobalPos.getPosition(evt.getPlayer().getCommandSenderWorld().dimension(),
                        evt.getPos());
                // Chunk Coordinate
                final KGobalPos c = CoordinateUtls.chunkPos(b);
                InventoryLogger.log("break {} at {} by {} {}", c, evt.getState(), evt.getPos(),
                        evt.getPlayer().getUUID(), evt.getPlayer().getName().getString());
            }
        }

        @SubscribeEvent(priority = EventPriority.HIGHEST)
        public void bucket(final FillBucketEvent event)
        {
            if (event.getPlayer().getCommandSenderWorld().isClientSide) return;
            if (!Essentials.config.landEnabled) return;
            BlockPos pos = event.getPlayer().blockPosition();
            if (event.getTarget() instanceof BlockHitResult && event.getTarget().getType() != Type.MISS)
            {
                final BlockHitResult trace = (BlockHitResult) event.getTarget();
                pos = trace.getBlockPos().relative(trace.getDirection());
            }
            final Player player = event.getPlayer();
            final BlockEvent evt = new BreakEvent(event.getWorld(), pos, event.getWorld().getBlockState(pos), player);
            this.checkPlace(evt, (ServerPlayer) player);
            this.checkBreak(evt, (ServerPlayer) player);
            if (evt.isCanceled()) event.setCanceled(true);
            else if (Essentials.config.log_interactions)
            {
                // Block coordinate
                final KGobalPos b = KGobalPos.getPosition(event.getPlayer().getCommandSenderWorld().dimension(),
                        evt.getPos());
                // Chunk Coordinate
                final KGobalPos c = CoordinateUtls.chunkPos(b);

                InventoryLogger.log("bucket {} -> {} at {} by {} {}", c, event.getEmptyBucket(),
                        event.getFilledBucket(), pos, player.getUUID(), player.getName().getString());
            }
        }
    }

    public static class EntityEventHandler
    {
        public static Set<UUID> showLandSet = Sets.newHashSet();

        private boolean canTakeDamage(final Entity in, final LandTeam land_owner)
        {
            if (LandManager.isWild(land_owner))
            {
                if (in != null && LandEventsHandler.invuln.contains(in.getType().getRegistryName())) return false;
                return true;
            }
            if (LandManager.getInstance().isPublicMob(in.getUUID())) return false;
            if (LandManager.getInstance().isProtectedMob(in.getUUID())) return false;
            if (in instanceof ServerPlayer) return !land_owner.noPlayerDamage;
            if (in instanceof Npc) return !land_owner.noNPCDamage;
            if (in instanceof ItemFrame) return !land_owner.protectFrames;
            return true;
        }

        private void sendNearbyChunks(final ServerPlayer player)
        {
            final ParticleOptions otherowned = ParticleTypes.ANGRY_VILLAGER;
            final ParticleOptions owned = ParticleTypes.HAPPY_VILLAGER;
            final ParticleOptions chunkloaded = ParticleTypes.HEART;
            ParticleOptions show = null;
            final LandTeam us = LandManager.getTeam(player);
            int x, y, z;
            int x1, y1, z1;
            Packet<?> packet;
            final Level world = player.getCommandSenderWorld();
            final ResourceKey<Level> dim = world.dimension();
            final int cx = SectionPos.blockToSectionCoord(player.getBlockX());
            final int cz = SectionPos.blockToSectionCoord(player.getBlockZ());
            final int cy = SectionPos.blockToSectionCoord(player.getBlockY());
            for (int dx = -3; dx <= 3; dx++) for (int dz = -3; dz <= 3; dz++)
            {
                x = cx + dx;
                z = cz + dz;
                final KGobalPos c = KGobalPos.getPosition(dim, new BlockPos(x, 0, z));
                y = cy;
                final boolean cl = ChunkLoadHandler.allLoaded.contains(c);

                for (int dy = -3; dy <= 3; dy++)
                {
                    y = cy + dy;

                    if (cl)
                    {
                        show = chunkloaded;
                        x1 = x * 16;
                        y1 = y * 16;
                        z1 = z * 16;
                        for (int i1 = 3; i1 < 14; i1 += 4) for (int j1 = 3; j1 < 14; j1 += 4)
                        {
                            packet = new ClientboundLevelParticlesPacket(show, false, x1 + i1, y1 + j1, z1 + 3, 0, 0, 0,
                                    0, 1);
                            player.connection.send(packet);
                            packet = new ClientboundLevelParticlesPacket(show, false, x1 + i1, y1 + j1, z1 + 13, 0, 0,
                                    0, 0, 1);
                            player.connection.send(packet);
                            packet = new ClientboundLevelParticlesPacket(show, false, x1 + 3, y1 + j1, z1 + i1, 0, 0, 0,
                                    0, 1);
                            player.connection.send(packet);
                            packet = new ClientboundLevelParticlesPacket(show, false, x1 + 13, y1 + j1, z1 + i1, 0, 0,
                                    0, 0, 1);
                            player.connection.send(packet);
                        }
                    }

                    final LandTeam team = LandManager.getInstance().getLandOwner(world, new BlockPos(x, y, z), true);
                    show = team == null ? null : team == us ? owned : otherowned;

                    if (show != null && y >= world.getMinSection() && y < world.getMaxSection())
                    {
                        x1 = x * 16;
                        y1 = y * 16;
                        z1 = z * 16;

                        for (int i1 = 1; i1 < 16; i1 += 4) for (int j1 = 1; j1 < 16; j1 += 4)
                        {
                            packet = new ClientboundLevelParticlesPacket(show, false, x1 + i1, y1 + j1, z1 + 1, 0, 0, 0,
                                    0, 1);
                            player.connection.send(packet);
                            packet = new ClientboundLevelParticlesPacket(show, false, x1 + i1, y1 + j1, z1 + 15, 0, 0,
                                    0, 0, 1);
                            player.connection.send(packet);
                            packet = new ClientboundLevelParticlesPacket(show, false, x1 + 1, y1 + j1, z1 + i1, 0, 0, 0,
                                    0, 1);
                            player.connection.send(packet);
                            packet = new ClientboundLevelParticlesPacket(show, false, x1 + 15, y1 + j1, z1 + i1, 0, 0,
                                    0, 0, 1);
                            player.connection.send(packet);
                        }
                    }
                }
            }

        }

        @SubscribeEvent(priority = EventPriority.HIGHEST)
        public void death(final LivingDeathEvent evt)
        {
            if (evt.getEntity().getCommandSenderWorld().isClientSide) return;
            if (!Essentials.config.landEnabled) return;

            // Cleanup the entity from protected mobs.
            final UUID id = evt.getEntity().getUUID();
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
            if (evt.getEntity().getCommandSenderWorld().isClientSide) return;
            if (!Essentials.config.landEnabled) return;
            if (evt.getEntityLiving() instanceof ServerPlayer && evt.getEntityLiving().tickCount > 10)
            {
                final ServerPlayer player = (ServerPlayer) evt.getEntityLiving();
                if (EntityEventHandler.showLandSet.contains(player.getUUID()) && player.tickCount % 20 == 0)
                    this.sendNearbyChunks(player);
                BlockPos here;
                BlockPos old;
                here = new BlockPos(player.xCloak, player.yCloak, player.zCloak);
                old = new BlockPos(player.xCloakO, player.yCloakO, player.zCloakO);

                if (old.equals(here)) return;

                final Level world = player.getCommandSenderWorld();

                final LandTeam newClaimer = LandManager.getInstance().getLandOwner(world, here);
                final LandTeam oldClaimer = LandManager.getInstance().getLandOwner(world, old);

                if (newClaimer == oldClaimer) return;

                final boolean isNewOwned = !LandManager.isWild(newClaimer);
                final boolean isOldOwned = !LandManager.isWild(oldClaimer);

                final boolean isWild = !isNewOwned;
                final boolean notLoaded = newClaimer == LandManager.getNotLoaded();

                final CompoundTag tag = PlayerDataHandler.getCustomDataTag(player);
                final CompoundTag entry_log = tag.getCompound("last_entered_chunk");

                BlockPos entry_point = old;

                if (!(isNewOwned || isOldOwned))
                {
                    entry_point = old;
                    entry_log.put("from", NbtUtils.writeBlockPos(entry_point));
                    entry_log.putString("owner", "");
                }
                else
                {
                    if (!entry_log.isEmpty()) entry_point = NbtUtils.readBlockPos(entry_log.getCompound("from"));

                    if (!LandEventsHandler.lastLeaveMessage.containsKey(evt.getEntity().getUUID()))
                        LandEventsHandler.lastLeaveMessage.put(evt.getEntity().getUUID(),
                                System.currentTimeMillis() - 1);
                    if (!LandEventsHandler.lastEnterMessage.containsKey(evt.getEntity().getUUID()))
                        LandEventsHandler.lastEnterMessage.put(evt.getEntity().getUUID(),
                                System.currentTimeMillis() - 1);

                    Component message = null;

                    final boolean owns = newClaimer != null && newClaimer.isMember(player);
                    if (!isNewOwned && !PermNodes.getBooleanPerm(player, LandEventsHandler.PERMENTERWILD))
                        message = CommandManager.makeFormattedComponent("msg.team.nowildperms.noenter");
                    else if (isNewOwned && owns && !PermNodes.getBooleanPerm(player, LandEventsHandler.PERMENTEROWN))
                        message = CommandManager.makeFormattedComponent("msg.team.owned.noenter");
                    else if (isNewOwned && !owns && !PermNodes.getBooleanPerm(player, LandEventsHandler.PERMENTEROTHER))
                        message = CommandManager.makeFormattedComponent("msg.team.other.noenter");
                    if (message != null)
                    {
                        final ChunkPos newChunk = new ChunkPos(here);
                        final ChunkPos oldChunk = new ChunkPos(old);
                        if (!newChunk.equals(oldChunk))
                        {
                            entry_point = old;
                            // entry_log.putString("last_name", last_name);
                            entry_log.put("from", NbtUtils.writeBlockPos(entry_point));
                            tag.put("last_entered_chunk", entry_log);
                        }
                        player.connection.teleport(entry_point.getX() + 0.5, entry_point.getY() + 0.5,
                                entry_point.getZ() + 0.5, player.getYRot(), player.getXRot());
                        ChatHelper.sendSystemMessage(player, message);
                        return;
                    }

                    // If we got to here, it means this is a valid location, so
                    // lets save it and current team.
                    // entry_log.putString("last_name", last_name);
                    entry_log.put("from", NbtUtils.writeBlockPos(here));
                    tag.put("last_entered_chunk", entry_log);

                    messages:
                    {
                        if (newClaimer != null && !isWild && !notLoaded)
                        {
                            if (newClaimer.equals(oldClaimer)) break messages;
                            if (oldClaimer != null)
                            {
                                final long last = LandEventsHandler.lastLeaveMessage.get(evt.getEntity().getUUID());
                                if (last < System.currentTimeMillis())
                                {
                                    LandEventsHandler.sendMessage(player, oldClaimer, LandEventsHandler.EXIT);
                                    LandEventsHandler.lastLeaveMessage.put(evt.getEntity().getUUID(),
                                            System.currentTimeMillis() + 100);
                                }
                            }
                            final long last = LandEventsHandler.lastEnterMessage.get(evt.getEntity().getUUID());
                            if (last < System.currentTimeMillis())
                            {
                                LandEventsHandler.sendMessage(player, newClaimer, LandEventsHandler.ENTER);
                                LandEventsHandler.lastLeaveMessage.put(evt.getEntity().getUUID(),
                                        System.currentTimeMillis() + 100);
                            }
                        }
                        else
                        {
                            final long last = LandEventsHandler.lastLeaveMessage.get(evt.getEntity().getUUID());
                            if (last < System.currentTimeMillis())
                            {
                                LandEventsHandler.sendMessage(player, oldClaimer, LandEventsHandler.EXIT);
                                LandEventsHandler.lastLeaveMessage.put(evt.getEntity().getUUID(),
                                        System.currentTimeMillis() + 100);
                            }
                        }
                    }
                }
            }
        }

        @SubscribeEvent(priority = EventPriority.HIGHEST)
        public void attack(final AttackEntityEvent evt)
        {
            if (evt.getEntity().getCommandSenderWorld().isClientSide) return;
            if (!Essentials.config.landEnabled) return;
            final Level world = evt.getEntity().getCommandSenderWorld();
            final KGobalPos b = CoordinateUtls.forMob(evt.getTarget());
            final LandTeam owner = LandManager.getInstance().getLandOwner(world, evt.getTarget().blockPosition());

            if (!this.canTakeDamage(evt.getTarget(), owner))
            {
                evt.setCanceled(true);
                return;
            }

            // TODO possible perms for attacking things in unclaimed land?
            if (LandManager.isWild(owner)) return;

            final Player attacker = evt.getPlayer();

            // Check if the team allows fakeplayers
            if (owner.fakePlayers && evt.getPlayer() instanceof FakePlayer) return;

            // Check if item frame
            if (evt.getTarget() instanceof ItemFrame && !owner.canBreakBlock(attacker.getUUID(), b))
            {
                evt.setCanceled(true);
                return;
            }

            // If mob is protected, do not allow the attack, even if by owner.
            if (owner.protected_mobs.contains(evt.getTarget().getUUID()))
            {
                evt.setCanceled(true);
                return;
            }
        }

        @SubscribeEvent(priority = EventPriority.HIGHEST)
        public void mobGriefing(final EntityMobGriefingEvent evt)
        {
            if (evt.getEntity() == null) return;
            if (evt.getEntity().getCommandSenderWorld().isClientSide) return;
            if (!Essentials.config.landEnabled) return;
            if (!Essentials.config.noMobGriefing) return;
            if (MobManager.isWhitelistedForGriefing(evt.getEntity())) return;
            final LandTeam owner = LandManager.getInstance().getLandOwner(evt.getEntity().getCommandSenderWorld(),
                    evt.getEntity().blockPosition());
            if (LandManager.isWild(owner)) return;
            // Check if the team allows fakeplayers
            if (owner.fakePlayers && evt.getEntity() instanceof FakePlayer) return;
            evt.setResult(Result.DENY);
        }

        @SubscribeEvent(priority = EventPriority.HIGHEST)
        public void attack(final LivingAttackEvent evt)
        {
            if (evt.getEntity().getCommandSenderWorld().isClientSide) return;
            if (!Essentials.config.landEnabled) return;
            final LandTeam owner = LandManager.getInstance().getLandOwner(evt.getEntity().getCommandSenderWorld(),
                    evt.getEntity().blockPosition());
            if (!this.canTakeDamage(evt.getEntity(), owner))
            {
                evt.setCanceled(true);
                return;
            }

            // TODO maybe add a perm for combat in non-claimed land?
            if (LandManager.isWild(owner)) return;

            if (evt.getEntity() instanceof Player)
            {
                final LandTeam players = LandManager.getTeam(evt.getEntity());
                // Check if player is protected via friendly fire settings.
                if (!players.friendlyFire)
                {
                    final Entity damageSource = evt.getSource().getEntity();
                    if (damageSource instanceof Player && LandEventsHandler.sameTeam(damageSource, evt.getEntity()))
                    {
                        evt.setCanceled(true);
                        return;
                    }
                }
            }

            // Check if the team allows fakeplayers
            if (owner.fakePlayers && evt.getSource().getEntity() instanceof FakePlayer) return;

            // check if entity is protected by team
            if (owner.protected_mobs.contains(evt.getEntity().getUUID()))
            {
                evt.setCanceled(true);
                return;
            }
        }

        @SubscribeEvent
        public void projectileImpact(final ProjectileImpactEvent evt)
        {
            if (evt.getEntity().getCommandSenderWorld().isClientSide) return;
            if (!Essentials.config.landEnabled) return;
            if (evt.getRayTraceResult().getType() == Type.MISS) return;
            if (!(evt.getRayTraceResult() instanceof EntityHitResult)) return;
            final EntityHitResult hit = (EntityHitResult) evt.getRayTraceResult();

            final Entity target = hit.getEntity();
            final LandTeam owner = LandManager.getInstance().getLandOwner(target.getCommandSenderWorld(),
                    target.blockPosition());

            if (!this.canTakeDamage(target, owner))
            {
                evt.setCanceled(true);
                return;
            }

            // TODO maybe add a perm for combat in non-claimed land?
            if (LandManager.isWild(owner)) return;

            // check if entity is protected by team
            if (owner.protected_mobs.contains(target.getUUID()))
            {
                evt.setCanceled(true);
                return;
            }

        }

        @SubscribeEvent(priority = EventPriority.HIGHEST)
        public void attack(final LivingHurtEvent evt)
        {
            if (evt.getEntity().getCommandSenderWorld().isClientSide) return;
            if (!Essentials.config.landEnabled) return;
            final LandTeam owner = LandManager.getInstance().getLandOwner(evt.getEntity().getCommandSenderWorld(),
                    evt.getEntity().blockPosition());
            if (!this.canTakeDamage(evt.getEntity(), owner))
            {
                evt.setCanceled(true);
                return;
            }

            // TODO maybe add a perm for combat in non-claimed land?
            if (LandManager.isWild(owner)) return;

            // check if entity is protected by team
            if (owner.protected_mobs.contains(evt.getEntity().getUUID()))
            {
                evt.setCanceled(true);
                return;
            }
        }

        @SubscribeEvent(priority = EventPriority.HIGHEST)
        public void spawn(final LivingSpawnEvent.SpecialSpawn evt)
        {
            if (!Essentials.config.landEnabled) return;
            if (evt.getEntity().getCommandSenderWorld().isClientSide) return;
            final LandTeam owner = LandManager.getInstance().getLandOwner(evt.getEntity().getCommandSenderWorld(),
                    evt.getEntity().blockPosition());
            if (LandManager.isWild(owner)) return;
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
            if (evt.getEntity().getCommandSenderWorld().isClientSide) return;
            final Level world = evt.getEntity().getCommandSenderWorld();
            final LandTeam owner = LandManager.getInstance().getLandOwner(world, evt.getEntity().blockPosition());
            if (LandManager.isWild(owner)) return;
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

            final ServerPlayer player = (ServerPlayer) evt.getPlayer();
            final Level world = player.getCommandSenderWorld();
            // Block coordinate
            final KGobalPos b = KGobalPos.getPosition(player.getCommandSenderWorld().dimension(), evt.getPos());
            final LandTeam owner = LandManager.getInstance().getLandOwner(world, evt.getPos());
            if (LandManager.isWild(owner))
            {
                if (PermNodes.getBooleanPerm(player, LandEventsHandler.PERMUSEBLOCKWILD)) return DenyReason.NONE;
                return DenyReason.WILD;
            }
            final boolean isFakePlayer = player instanceof FakePlayer;
            // Check if the team allows fakeplayers
            if (owner.fakePlayers && isFakePlayer) return DenyReason.NONE;
            else // Otherwise check normal behaviour
            {
                // Treat relation place perm as owning the land.
                final boolean owns = owner.canUseStuff(player.getUUID(), b);
                if (owns && !PermNodes.getBooleanPerm(player, LandEventsHandler.PERMUSEBLOCKOWN))
                    return DenyReason.OURS;
                if (!owns && !PermNodes.getBooleanPerm(player, LandEventsHandler.PERMUSEBLOCKOTHER))
                    return DenyReason.OTHER;
            }
            return DenyReason.NONE;
        }

        private DenyReason canUseItem(final PlayerInteractEvent evt)
        {
            // Check our global whitelist
            if (LandEventsHandler.itemUseWhitelist.contains(evt.getItemStack().getItem().getRegistryName()))
                return DenyReason.NONE;

            // See if is food and should be explicitly whitelisted
            if (Essentials.config.foodWhitelisted && evt.getItemStack().isEdible()) return DenyReason.NONE;

            // Check the tag for the item as well
            if (ItemList.is(LandEventsHandler.ITEMUSEWHTETAG, evt.getItemStack())) return DenyReason.NONE;

            // Check our specific event allowances
            if (MinecraftForge.EVENT_BUS
                    .post(new DenyItemUseEvent(evt.getEntity(), evt.getItemStack(), UseType.RIGHTCLICKBLOCK)))
                return DenyReason.NONE;

            final ServerPlayer player = (ServerPlayer) evt.getPlayer();
            final Level world = player.getCommandSenderWorld();
            // Block coordinate
            final KGobalPos b = KGobalPos.getPosition(player.getCommandSenderWorld().dimension(), evt.getPos());
            final LandTeam owner = LandManager.getInstance().getLandOwner(world, evt.getPos());
            if (LandManager.isWild(owner))
            {
                if (PermNodes.getBooleanPerm(player, LandEventsHandler.PERMUSEITEMWILD)) return DenyReason.NONE;
                return DenyReason.WILD;
            }
            final boolean isFakePlayer = player instanceof FakePlayer;
            // Check if the team allows fakeplayers
            if (owner.fakePlayers && isFakePlayer) return DenyReason.NONE;
            else // Otherwise check normal behaviour
            {
                // Treat relation place perm as owning the land.
                final boolean owns = owner.canUseStuff(player.getUUID(), b);
                if (owns && !PermNodes.getBooleanPerm(player, LandEventsHandler.PERMUSEITEMOWN)) return DenyReason.OURS;
                if (!owns && !PermNodes.getBooleanPerm(player, LandEventsHandler.PERMUSEITEMOTHER))
                    return DenyReason.OTHER;
            }
            return DenyReason.NONE;
        }

        private DenyReason canUseMob(final PlayerInteractEvent evt, final Entity mob)
        {
            // Check our global whitelist
            final ResourceLocation reg = mob.getType().getRegistryName();
            if (LandEventsHandler.mobUseWhitelist.contains(reg)) return DenyReason.NONE;
            final String[] perms = LandEventsHandler.customMobPerms.getOrDefault(reg,
                    LandEventsHandler.defaultMobPerms);

            // Check our specific event allowances
            if (MinecraftForge.EVENT_BUS
                    .post(new DenyItemUseEvent(evt.getEntity(), evt.getItemStack(), UseType.RIGHTCLICKBLOCK)))
                return DenyReason.NONE;

            final ServerPlayer player = (ServerPlayer) evt.getPlayer();
            final Level world = player.getCommandSenderWorld();
            // Block coordinate
            final KGobalPos b = KGobalPos.getPosition(player.getCommandSenderWorld().dimension(), evt.getPos());
            final LandTeam owner = LandManager.getInstance().getLandOwner(world, evt.getPos());
            if (LandManager.isWild(owner) || LandManager.getInstance().isPublicMob(evt.getEntity().getUUID()))
            {
                final String wildUse = perms[0];
                if (PermNodes.getBooleanPerm(player, wildUse)) return DenyReason.NONE;
                return DenyReason.WILD;
            }
            final boolean isFakePlayer = player instanceof FakePlayer;
            // Check if the team allows fakeplayers
            if (owner.fakePlayers && isFakePlayer) return DenyReason.NONE;
            else // Otherwise check normal behaviour
            {
                // Treat relation place perm as owning the land.
                final boolean owns = owner.canUseStuff(player.getUUID(), b);
                if (owns && !PermNodes.getBooleanPerm(player, perms[1])) return DenyReason.OURS;
                if (!owns && !PermNodes.getBooleanPerm(player, perms[2])) return DenyReason.OTHER;
            }
            return DenyReason.NONE;
        }

        @SubscribeEvent(priority = EventPriority.HIGHEST)
        public void interact(final PlayerInteractEvent.LeftClickBlock evt)
        {
            if (!(evt.getPlayer() instanceof ServerPlayer)) return;
            if (!Essentials.config.landEnabled) return;
            final DenyReason rsult = this.canUseBlock(evt);
            // First check if we do not have permission to act here.
            if (!rsult.test())
            {
                final Level world = evt.getWorld();
                // Block coordinate
                final KGobalPos b = KGobalPos.getPosition(evt.getPlayer().getCommandSenderWorld().dimension(),
                        evt.getPos());
                // Chunk Coordinate
                final KGobalPos c = CoordinateUtls.chunkPos(b);
                final LandTeam owner = LandManager.getInstance().getLandOwner(world, evt.getPos());

                final boolean isFakePlayer = evt.getPlayer() instanceof FakePlayer;
                if (!isFakePlayer)
                {
                    final ServerPlayer player = (ServerPlayer) evt.getPlayer();
                    switch (rsult)
                    {
                    case OTHER:
                    case OURS:
                        LandEventsHandler.sendMessage(player, owner, LandEventsHandler.DENY);
                        break;
                    case WILD:
                        ChatHelper.sendSystemMessage(player,
                                CommandManager.makeFormattedComponent("msg.team.nowildperms.useblock"));
                        break;
                    default:
                        break;
                    }
                    player.inventoryMenu.sendAllDataToRemote();
                }
                if (Essentials.config.log_interactions) InventoryLogger.log("Cancelled Left Click at {} for {} {}", c,
                        b.getPos(), evt.getPlayer().getUUID(), evt.getPlayer().getName().getString());
                evt.setCanceled(true);
                evt.setUseBlock(Result.DENY);
                evt.setUseItem(Result.DENY);
                return;
            }

        }

        @SubscribeEvent(priority = EventPriority.HIGHEST)
        public void interact(final PlayerInteractEvent.EntityInteractSpecific evt)
        {
            if (evt.getSide() == LogicalSide.CLIENT) return;
            if (!Essentials.config.landEnabled) return;
            final DenyReason rsult = this.canUseMob(evt, evt.getTarget());
            // First check if we do not have permission to act here.
            if (!rsult.test())
            {
                final Level world = evt.getWorld();
                // Block coordinate
                final KGobalPos b = KGobalPos.getPosition(evt.getPlayer().getCommandSenderWorld().dimension(),
                        evt.getPos());
                // Chunk Coordinate
                final KGobalPos c = CoordinateUtls.chunkPos(b);
                final LandTeam owner = LandManager.getInstance().getLandOwner(world, evt.getPos());

                final boolean isFakePlayer = evt.getPlayer() instanceof FakePlayer;
                if (!isFakePlayer)
                {
                    final ServerPlayer player = (ServerPlayer) evt.getPlayer();
                    switch (rsult)
                    {
                    case OTHER:
                    case OURS:
                        LandEventsHandler.sendMessage(player, owner, LandEventsHandler.DENY);
                        break;
                    case WILD:
                        ChatHelper.sendSystemMessage(player,
                                CommandManager.makeFormattedComponent("msg.team.nowildperms.usemob"));
                        break;
                    default:
                        break;
                    }
                    player.inventoryMenu.sendAllDataToRemote();
                }
                if (Essentials.config.log_interactions)
                    InventoryLogger.log("Cancelled Mob Interact at {} with {} for {} {}", c, b.getPos(),
                            evt.getTarget().getName().getString(), evt.getPlayer().getUUID(),
                            evt.getPlayer().getName().getString());
                evt.setCanceled(true);
                evt.setCancellationResult(InteractionResult.FAIL);
                return;
            }
        }

        @SubscribeEvent(priority = EventPriority.HIGHEST)
        public void interact(final PlayerInteractEvent.EntityInteract evt)
        {
            if (evt.getSide() == LogicalSide.CLIENT) return;
            if (!Essentials.config.landEnabled) return;
            final DenyReason rsult = this.canUseMob(evt, evt.getTarget());

            final Level world = evt.getWorld();
            // Block coordinate
            final KGobalPos b = KGobalPos.getPosition(evt.getPlayer().getCommandSenderWorld().dimension(),
                    evt.getPos());
            // Chunk Coordinate
            final KGobalPos c = CoordinateUtls.chunkPos(b);
            final LandTeam owner = LandManager.getInstance().getLandOwner(world, evt.getPos());
            // First check if we do not have permission to act here.
            if (!rsult.test())
            {
                final boolean isFakePlayer = evt.getPlayer() instanceof FakePlayer;
                if (!isFakePlayer)
                {
                    final ServerPlayer player = (ServerPlayer) evt.getPlayer();
                    switch (rsult)
                    {
                    case OTHER:
                    case OURS:
                        LandEventsHandler.sendMessage(player, owner, LandEventsHandler.DENY);
                        break;
                    case WILD:
                        ChatHelper.sendSystemMessage(player,
                                CommandManager.makeFormattedComponent("msg.team.nowildperms.usemob"));
                        break;
                    default:
                        break;
                    }
                    player.inventoryMenu.sendAllDataToRemote();
                }
                if (Essentials.config.log_interactions)
                    InventoryLogger.log("Cancelled Mob Interact at {} with {} for {} {}", c, b.getPos(),
                            evt.getTarget().getName().getString(), evt.getPlayer().getUUID(),
                            evt.getPlayer().getName().getString());
                evt.setCanceled(true);
                evt.setCancellationResult(InteractionResult.FAIL);
                return;
            }

            if (LandManager.isWild(owner)) return;

            // Not letting these manage this stuff.
            if (evt.getPlayer() instanceof FakePlayer) return;

            // If the player owns it, they can toggle whether the entity is
            // protected or not, Only team admins can do this.
            if (owner.isAdmin(evt.getPlayer()))
            {
                // No protecting players.
                if (evt.getTarget() instanceof Player) return;

                // check if player is holding a public toggle.
                if (!evt.getWorld().isClientSide && LandEventsHandler.isPublicToggle(evt.getItemStack())
                        && evt.getPlayer().isCrouching())
                {
                    // If so, toggle whether the entity is public.
                    final UUID id = evt.getTarget().getUUID();
                    final boolean isPublic = owner.public_mobs.contains(id);
                    LandManager.getInstance().toggleMobPublic(id, owner);
                    ChatHelper.sendSystemMessage(evt.getPlayer(), Essentials.config.getMessage("msg.team.setmob.public." + !isPublic));
                    evt.setCanceled(true);
                    if (Essentials.config.log_interactions)
                        InventoryLogger.log("Cancelled Mob Interact at {} with {} for {} {}", c, b.getPos(),
                                evt.getTarget().getName().getString(), evt.getPlayer().getUUID(),
                                evt.getPlayer().getName().getString());
                    return;
                }
                // check if player is holding a protect toggle.
                if (!evt.getWorld().isClientSide && LandEventsHandler.isProtectToggle(evt.getItemStack())
                        && evt.getPlayer().isCrouching()
                        && PermNodes.getBooleanPerm((ServerPlayer) evt.getPlayer(), LandEventsHandler.PERMPROTECTMOB))
                {
                    // If so, toggle whether the entity is protected.
                    final UUID id = evt.getTarget().getUUID();
                    final boolean isPublic = owner.protected_mobs.contains(id);
                    LandManager.getInstance().toggleMobProtect(id, owner);
                    ChatHelper.sendSystemMessage(evt.getPlayer(), Essentials.config.getMessage("msg.team.setmob.protect." + !isPublic));
                    evt.setCanceled(true);
                    if (Essentials.config.log_interactions)
                        InventoryLogger.log("Cancelled Mob Interact at {} with {} for {} {}", c, b.getPos(),
                                evt.getTarget().getName().getString(), evt.getPlayer().getUUID(),
                                evt.getPlayer().getName().getString());
                    return;
                }
            }
        }

        @SubscribeEvent(priority = EventPriority.HIGHEST)
        public void interact(final PlayerInteractEvent.RightClickItem evt)
        {
            if (!(evt.getPlayer() instanceof ServerPlayer)) return;
            final DenyReason rsult = this.canUseItem(evt);
            // First check if we do not have permission to act here.
            if (!rsult.test())
            {
                final Level world = evt.getWorld();
                // Block coordinate
                final KGobalPos b = KGobalPos.getPosition(evt.getPlayer().getCommandSenderWorld().dimension(),
                        evt.getPos());
                // Chunk Coordinate
                final KGobalPos c = CoordinateUtls.chunkPos(b);
                final LandTeam owner = LandManager.getInstance().getLandOwner(world, evt.getPos());
                final boolean isFakePlayer = evt.getPlayer() instanceof FakePlayer;
                if (!isFakePlayer)
                {
                    final ServerPlayer player = (ServerPlayer) evt.getPlayer();
                    switch (rsult)
                    {
                    case OTHER:
                    case OURS:
                        LandEventsHandler.sendMessage(player, owner, LandEventsHandler.DENY);
                        break;
                    case WILD:
                        ChatHelper.sendSystemMessage(player,
                                CommandManager.makeFormattedComponent("msg.team.nowildperms.useblock"));
                        break;
                    default:
                        break;
                    }
                    player.inventoryMenu.sendAllDataToRemote();
                }
                if (Essentials.config.log_interactions)
                    InventoryLogger.log("Cancelled Item Interact at {} with {} for {} {}", c, b.getPos(),
                            evt.getItemStack(), evt.getPlayer().getUUID(), evt.getPlayer().getName().getString());
                evt.setCancellationResult(InteractionResult.FAIL);
                evt.setCanceled(true);
                return;
            }
        }

        @SubscribeEvent(priority = EventPriority.HIGHEST)
        public void interact(final PlayerInteractEvent.RightClickBlock evt)
        {
            if (!(evt.getPlayer() instanceof ServerPlayer)) return;
            if (!Essentials.config.landEnabled) return;
            final Level world = evt.getWorld();
            // Block coordinate
            final KGobalPos b = KGobalPos.getPosition(evt.getPlayer().getCommandSenderWorld().dimension(),
                    evt.getPos());
            // Chunk Coordinate
            final KGobalPos c = CoordinateUtls.chunkPos(b);
            final LandTeam owner = LandManager.getInstance().getLandOwner(world, evt.getPos());
            final DenyReason rsult = this.canUseBlock(evt);
            // First check if we do not have permission to act here.
            if (!rsult.test())
            {
                final boolean isFakePlayer = evt.getPlayer() instanceof FakePlayer;
                if (!isFakePlayer)
                {
                    final ServerPlayer player = (ServerPlayer) evt.getPlayer();

                    switch (rsult)
                    {
                    case OTHER:
                    case OURS:
                        LandEventsHandler.sendMessage(player, owner, LandEventsHandler.DENY);
                        break;
                    case WILD:
                        ChatHelper.sendSystemMessage(player,
                                CommandManager.makeFormattedComponent("msg.team.nowildperms.useblock"));
                        break;
                    default:
                        break;
                    }
                    player.inventoryMenu.sendAllDataToRemote();
                }
                if (Essentials.config.log_interactions)
                    InventoryLogger.log("Cancelled Block Interact at {} with {} for {} {}", c, b.getPos(),
                            evt.getItemStack(), evt.getPlayer().getUUID(), evt.getPlayer().getName().getString());
                evt.setCanceled(true);
                evt.setUseBlock(Result.DENY);
                evt.setUseItem(Result.DENY);
                return;
            }
            // We don't care about anything else beyond here is unowned
            if (LandManager.isWild(owner)) return;

            final Player player = evt.getPlayer();

            // Don't allow fake players to act below
            if (evt.getPlayer() instanceof FakePlayer) return;
            // Check permission, Treat relation public perm as if we own this
            // for this check.
            final boolean owns = owner.canUseStuff(player.getUUID(), b) || owner.canPlaceBlock(player.getUUID(), b);
            // Check if the block is public.
            final KGobalPos blockLoc = b;
            // If we own this, we can return here, first check public toggle
            // though.
            if (owns)
            {
                // Do stuff for toggling public
                if (!evt.getWorld().isClientSide && LandEventsHandler.isPublicToggle(evt.getItemStack())
                        && evt.getPlayer().isCrouching() && !owner.allPublic
                        && LandManager.getInstance().isAdmin(evt.getPlayer().getUUID()))
                {
                    final boolean isPublic = LandManager.getInstance().isPublic(blockLoc, owner);
                    if (isPublic) LandManager.getInstance().unsetPublic(blockLoc, owner);
                    else LandManager.getInstance().setPublic(blockLoc, owner);
                    ChatHelper.sendSystemMessage(evt.getPlayer(), Essentials.config.getMessage("msg.team.setpublic.block." + !isPublic));
                    evt.setCanceled(true);
                    if (Essentials.config.log_interactions)
                        InventoryLogger.log("Cancelled Block Interact at {} with {} for {} {}", c, b.getPos(),
                                evt.getItemStack(), evt.getPlayer().getUUID(), evt.getPlayer().getName().getString());
                }
                // Do stuff for toggling break
                if (!evt.getWorld().isClientSide && LandEventsHandler.isBreakToggle(evt.getItemStack())
                        && evt.getPlayer().isCrouching()
                        && LandManager.getInstance().isAdmin(evt.getPlayer().getUUID()))
                {
                    final boolean isPublic = owner.public_break.contains(blockLoc);
                    if (owner.public_break.contains(blockLoc)) owner.public_break.remove(blockLoc);
                    else owner.public_break.add(blockLoc);
                    ChatHelper.sendSystemMessage(evt.getPlayer(), Essentials.config.getMessage("msg.team.setbreak.block." + !isPublic));
                    LandSaveHandler.saveTeam(owner.teamName);
                    evt.setCanceled(true);
                    if (Essentials.config.log_interactions)
                        InventoryLogger.log("Cancelled Block Interact at {} with {} for {} {}", c, b.getPos(),
                                evt.getItemStack(), evt.getPlayer().getUUID(), evt.getPlayer().getName().getString());
                }
                // Do stuff for toggling place
                if (!evt.getWorld().isClientSide && LandEventsHandler.isPlaceToggle(evt.getItemStack())
                        && evt.getPlayer().isCrouching()
                        && LandManager.getInstance().isAdmin(evt.getPlayer().getUUID()))
                {
                    final boolean isPublic = owner.public_place.contains(blockLoc);
                    if (owner.public_place.contains(blockLoc)) owner.public_place.remove(blockLoc);
                    else owner.public_place.add(blockLoc);
                    ChatHelper.sendSystemMessage(evt.getPlayer(), Essentials.config.getMessage("msg.team.setplace.block." + !isPublic));
                    LandSaveHandler.saveTeam(owner.teamName);
                    evt.setCanceled(true);
                    if (Essentials.config.log_interactions)
                        InventoryLogger.log("Cancelled Block Interact at {} with {} for {} {}", c, b.getPos(),
                                evt.getItemStack(), evt.getPlayer().getUUID(), evt.getPlayer().getName().getString());
                }
                return;
            }
        }
    }

    public static class ChunkLoadHandler
    {
        public static MinecraftServer server;

        public static Set<ServerLevel> worlds = Sets.newConcurrentHashSet();

        public static Set<KGobalPos> allLoaded = Sets.newConcurrentHashSet();

        @SubscribeEvent
        public static void ServerLoaded(final ServerStartedEvent event)
        {
            if (!Essentials.config.chunkLoading) return;
            ChunkLoadHandler.server.tell(new TickTask(1, () -> {
                LandManager.getInstance()._teamMap.forEach((s, t) -> {
                    for (final KGobalPos c : t.land.getLoaded()) ChunkLoadHandler.addChunks(c);
                });
            }));
        }

        public static boolean removeChunks(KGobalPos location)
        {
            if (!Essentials.config.chunkLoading) return false;
            final ServerLevel world = ChunkLoadHandler.server.getLevel(location.getDimension());
            if (world == null) return false;
            if (location.getPos().getY() != 0) location = KGobalPos.getPosition(location.getDimension(),
                    location.getPos().below(location.getPos().getY()));
            if (!ChunkLoadHandler.allLoaded.remove(location)) return false;
            world.getChunkSource().updateChunkForced(new ChunkPos(location.getPos().getX(), location.getPos().getZ()),
                    false);
            return true;
        }

        public static boolean addChunks(KGobalPos location)
        {
            if (!Essentials.config.chunkLoading) return false;
            final ServerLevel world = ChunkLoadHandler.server.getLevel(location.getDimension());
            if (world == null) return false;
            if (location.getPos().getY() != 0) location = KGobalPos.getPosition(location.getDimension(),
                    location.getPos().below(location.getPos().getY()));
            if (!ChunkLoadHandler.allLoaded.add(location)) return false;
            world.getChunkSource().updateChunkForced(new ChunkPos(location.getPos().getX(), location.getPos().getZ()),
                    true);
            return true;
        }
    }

    public static boolean sameTeam(final Entity a, final Entity b)
    {
        return LandManager.getTeam(a) == LandManager.getTeam(b);
    }

    public static final ResourceLocation ITEMUSEWHTETAG = new ResourceLocation(Essentials.MODID, "land_whitelist");

    public static final String PERMBREAKWILD = "land.break.unowned";
    public static final String PERMBREAKOWN = "land.break.owned.self";
    public static final String PERMBREAKOTHER = "land.break.owned.other";

    public static final String PERMPLACEWILD = "land.place.unowned";
    public static final String PERMPLACEOWN = "land.place.owned.self";
    public static final String PERMPLACEOTHER = "land.place.owned.other";

    public static final String PERMUSEITEMWILD = "land.useitem.unowned";
    public static final String PERMUSEITEMOWN = "land.useitem.owned.self";
    public static final String PERMUSEITEMOTHER = "land.useitem.owned.other";

    public static final String PERMUSEBLOCKWILD = "land.useblock.unowned";
    public static final String PERMUSEBLOCKOWN = "land.useblock.owned.self";
    public static final String PERMUSEBLOCKOTHER = "land.useblock.owned.other";

    public static final String PERMUSEMOBWILD = "land.usemob.unowned";
    public static final String PERMUSEMOBOWN = "land.usemob.owned.self";
    public static final String PERMUSEMOBOTHER = "land.usemob.owned.other";

    public static final String PERMENTERWILD = "land.enter.unowned";
    public static final String PERMENTEROWN = "land.enter.owned.self";
    public static final String PERMENTEROTHER = "land.enter.owned.other";

    public static final String PERMCREATETEAM = "teams.create";
    public static final String PERMJOINTEAMINVITED = "teams.join.invite";
    public static final String PERMJOINTEAMNOINVITE = "teams.join.force";

    public static final String PERMPROTECTMOB = "teams.protect.mob";

    public static final String PERMUNCLAIMOTHER = "land.unclaim.owned.other";

    static Map<UUID, Long> lastLeaveMessage = Maps.newHashMap();
    static Map<UUID, Long> lastEnterMessage = Maps.newHashMap();

    protected InteractEventHandler interact_handler = new InteractEventHandler();
    protected EntityEventHandler entity_handler = new EntityEventHandler();
    protected BlockEventHandler block_handler = new BlockEventHandler();

    public Set<UUID> checked = Sets.newHashSet();
    public List<GameProfile> toCheck = Lists.newArrayList();

    public LandEventsHandler()
    {}

    public void registerPerms()
    {
        PermNodes.registerNode(LandEventsHandler.PERMBREAKWILD, DefaultPermissionLevel.ALL,
                "Can the player break blocks in unowned land.");
        PermNodes.registerNode(LandEventsHandler.PERMBREAKOWN, DefaultPermissionLevel.ALL,
                "Can the player break blocks in their own land.");
        PermNodes.registerNode(LandEventsHandler.PERMBREAKOTHER, DefaultPermissionLevel.OP,
                "Can the player break blocks in other player's land.");

        PermNodes.registerNode(LandEventsHandler.PERMPLACEWILD, DefaultPermissionLevel.ALL,
                "Can the player place blocks in unowned land.");
        PermNodes.registerNode(LandEventsHandler.PERMPLACEOWN, DefaultPermissionLevel.ALL,
                "Can the player place blocks in their own land.");
        PermNodes.registerNode(LandEventsHandler.PERMPLACEOTHER, DefaultPermissionLevel.OP,
                "Can the player place blocks in other player's land.");

        PermNodes.registerNode(LandEventsHandler.PERMUSEITEMWILD, DefaultPermissionLevel.ALL,
                "Can the player use items in unowned land.");
        PermNodes.registerNode(LandEventsHandler.PERMUSEITEMOWN, DefaultPermissionLevel.ALL,
                "Can the player use items in their own land.");
        PermNodes.registerNode(LandEventsHandler.PERMUSEITEMOTHER, DefaultPermissionLevel.OP,
                "Can the player use items in other player's land.");

        PermNodes.registerNode(LandEventsHandler.PERMUSEBLOCKWILD, DefaultPermissionLevel.ALL,
                "Can the player use items in unowned land.");
        PermNodes.registerNode(LandEventsHandler.PERMUSEBLOCKOWN, DefaultPermissionLevel.ALL,
                "Can the player use items in their own land.");
        PermNodes.registerNode(LandEventsHandler.PERMUSEBLOCKOTHER, DefaultPermissionLevel.OP,
                "Can the player use items in other player's land.");

        PermNodes.registerNode(LandEventsHandler.PERMUSEMOBWILD, DefaultPermissionLevel.ALL,
                "Can the player interact with mobs in unowned land.");
        PermNodes.registerNode(LandEventsHandler.PERMUSEMOBOWN, DefaultPermissionLevel.ALL,
                "Can the player interact with mobs in their own land.");
        PermNodes.registerNode(LandEventsHandler.PERMUSEMOBOTHER, DefaultPermissionLevel.ALL,
                "Can the player interact with mobs in other player's land.");

        PermNodes.registerNode(LandEventsHandler.PERMENTERWILD, DefaultPermissionLevel.ALL,
                "Can the player enter unowned land.");
        PermNodes.registerNode(LandEventsHandler.PERMENTEROWN, DefaultPermissionLevel.ALL,
                "Can the player enter their own land.");
        PermNodes.registerNode(LandEventsHandler.PERMENTEROTHER, DefaultPermissionLevel.ALL,
                "Can the player enter other player's land.");

        PermNodes.registerNode(LandEventsHandler.PERMCREATETEAM, DefaultPermissionLevel.ALL,
                "Can the player create a team.");
        PermNodes.registerNode(LandEventsHandler.PERMJOINTEAMINVITED, DefaultPermissionLevel.ALL,
                "Can the player join a team with an invite.");
        PermNodes.registerNode(LandEventsHandler.PERMJOINTEAMNOINVITE, DefaultPermissionLevel.OP,
                "Can the player join a team without an invite.");

        PermNodes.registerNode(LandEventsHandler.PERMPROTECTMOB, DefaultPermissionLevel.ALL,
                "Can the player protect mobs in their team's land.");

        PermNodes.registerNode(LandEventsHandler.PERMUNCLAIMOTHER, DefaultPermissionLevel.OP,
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
        final MinecraftServer server = Essentials.server;
        if (server.getNextTickTime() % 200 != 0) return;
        GameProfile profile = this.toCheck.get(0);
        try
        {
            profile = server.getSessionService().fillProfileProperties(profile, true);
            if (profile.getName() == null || profile.getId() == null) return;
            server.getProfileCache().add(profile);
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
        final Player entityPlayer = evt.getPlayer();
        final LandTeam team = LandManager.getTeam(entityPlayer);
        final MinecraftServer server = Essentials.server;
        team.lastSeen = server.getNextTickTime();
    }

    @SubscribeEvent
    public void logout(final PlayerLoggedOutEvent evt)
    {
        final Player entityPlayer = evt.getPlayer();
        final LandTeam team = LandManager.getTeam(entityPlayer);
        final MinecraftServer server = Essentials.server;
        team.lastSeen = server.getNextTickTime();
    }

    @SubscribeEvent
    public void detonate(final ExplosionEvent.Detonate evt)
    {
        if (evt.getWorld().isClientSide) return;
        final List<BlockPos> toRemove = Lists.newArrayList();
        final boolean denyBlasts = Essentials.config.denyExplosions;
        if (Essentials.config.landEnabled) for (final BlockPos pos : evt.getAffectedBlocks())
        {
            final LandTeam owner = LandManager.getInstance().getLandOwner(evt.getWorld(), pos);
            boolean deny = denyBlasts;
            if (LandManager.isWild(owner)) continue;
            deny = deny || owner.noExplosions;
            if (!deny) continue;
            toRemove.add(pos);
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

    private static final byte DENY = 0;
    private static final byte ENTER = 1;
    private static final byte EXIT = 2;

    private static Map<UUID, Long> denyFloodControl = Maps.newHashMap();
    private static Map<UUID, Long> enterFloodControl = Maps.newHashMap();
    private static Map<UUID, Long> exitFloodControl = Maps.newHashMap();

    private static long getTime(final Entity player)
    {
        return player.getServer().getLevel(Level.OVERWORLD).getGameTime();
    }

    private static void sendMessage(final Player player, final LandTeam team, final byte index)
    {
        Component message = null;
        if (team == null) return;
        final long time = LandEventsHandler.getTime(player);
        final int delay = 10;
        switch (index)
        {
        case DENY:
            message = LandEventsHandler.getDenyMessage(team);
            if (LandEventsHandler.denyFloodControl.getOrDefault(player.getUUID(), (long) 0) > time) message = null;
            else LandEventsHandler.denyFloodControl.put(player.getUUID(), time + delay);
            break;
        case ENTER:
            message = LandEventsHandler.getEnterMessage(team);
            if (LandEventsHandler.enterFloodControl.getOrDefault(player.getUUID(), (long) 0) > time) message = null;
            else LandEventsHandler.enterFloodControl.put(player.getUUID(), time + delay);
            break;
        case EXIT:
            message = LandEventsHandler.getExitMessage(team);
            if (LandEventsHandler.exitFloodControl.getOrDefault(player.getUUID(), (long) 0) > time) message = null;
            else LandEventsHandler.exitFloodControl.put(player.getUUID(), time + delay);
            break;
        }
        if (message != null) player.displayClientMessage(message, true);
    }

    private static Component getDenyMessage(final LandTeam team)
    {
        if (team != null && !team.denyMessage.isEmpty()) return new TextComponent(team.denyMessage);
        if (!Essentials.config.defaultMessages) return null;
        return CommandManager.makeFormattedComponent("msg.team.deny", null, false, team.teamName);
    }

    private static Component getEnterMessage(final LandTeam team)
    {
        if (team != null && !team.enterMessage.isEmpty()) return new TextComponent(team.enterMessage);
        if (!Essentials.config.defaultMessages) return null;
        return CommandManager.makeFormattedComponent("msg.team.enterLand", null, false, team.teamName);
    }

    private static Component getExitMessage(final LandTeam team)
    {
        if (team != null && !team.exitMessage.isEmpty()) return new TextComponent(team.exitMessage);
        if (!Essentials.config.defaultMessages) return null;
        return CommandManager.makeFormattedComponent("msg.team.exitLand", null, false, team.teamName);
    }
}
