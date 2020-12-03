package thut.essentials.util;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.UUID;

import com.mojang.authlib.GameProfile;
import com.mojang.serialization.Dynamic;

import io.netty.buffer.Unpooled;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.NBTDynamicOps;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.ServerPlayNetHandler;
import net.minecraft.network.play.server.SCustomPayloadPlayPacket;
import net.minecraft.network.play.server.SHeldItemChangePacket;
import net.minecraft.network.play.server.SJoinGamePacket;
import net.minecraft.network.play.server.SPlayEntityEffectPacket;
import net.minecraft.network.play.server.SPlayerAbilitiesPacket;
import net.minecraft.network.play.server.SPlayerListItemPacket;
import net.minecraft.network.play.server.SServerDifficultyPacket;
import net.minecraft.network.play.server.STagsListPacket;
import net.minecraft.network.play.server.SUpdateRecipesPacket;
import net.minecraft.potion.EffectInstance;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.dedicated.DedicatedPlayerList;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.management.PlayerProfileCache;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.Util;
import net.minecraft.util.registry.DynamicRegistries;
import net.minecraft.util.registry.DynamicRegistries.Impl;
import net.minecraft.util.text.ChatType;
import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.DimensionType;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeManager;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.storage.IWorldInfo;
import net.minecraft.world.storage.PlayerData;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import thut.essentials.Essentials;

public class NameManager extends DedicatedPlayerList
{

    private static Field NAME = null;

    static
    {
        try
        {
            NameManager.NAME = GameProfile.class.getDeclaredFields()[1];
            NameManager.NAME.setAccessible(true);
            final Field modifiersField = Field.class.getDeclaredField("modifiers");
            modifiersField.setAccessible(true);
            modifiersField.setInt(NameManager.NAME, NameManager.NAME.getModifiers() & ~Modifier.FINAL);
        }
        catch (SecurityException | NoSuchFieldException | IllegalArgumentException | IllegalAccessException e)
        {
            NameManager.NAME = null;
        }
    }

    private static void setName(final String name, final GameProfile profile)
    {
        try
        {
            NameManager.NAME.set(profile, name);
        }
        catch (IllegalArgumentException | IllegalAccessException e)
        {
            Essentials.LOGGER.warn("Error setting name", e);
        }
    }

    public static void serverStarting(final FMLServerStartingEvent event)
    {
        final MinecraftServer server = event.getServer();
        if (!(server instanceof DedicatedServer)) return;
        final DedicatedServer ded = (DedicatedServer) server;
        server.setPlayerList(new NameManager(ded, (Impl) ded.func_244267_aX(), ded.playerDataManager));
        server.getPlayerList().func_212504_a(server.getWorld(World.OVERWORLD));
    }

    public static void init()
    {
        MinecraftForge.EVENT_BUS.addListener(NameManager::serverStarting);
    }

    public static void onLogin(final ServerPlayerEntity player, final MinecraftServer server)
    {
        final String name = PlayerDataHandler.getCustomDataTag(player).getString("nick");
        if (!name.isEmpty()) NameManager.setName(name, player.getGameProfile(), server);
    }

    public static void setName(String name, final GameProfile profile, final MinecraftServer server)
    {
        if (NameManager.NAME == null)
        {
            Essentials.LOGGER.warn("Setting custom name currently disabled.");
            return;
        }
        name = RuleManager.format(name);
        final ServerPlayerEntity player = server.getPlayerList().getPlayerByUUID(profile.getId());
        if ("_".equals(name) && player != null) if (PlayerDataHandler.getCustomDataTag(player).contains("nick_orig"))
            name = PlayerDataHandler.getCustomDataTag(player).getString("nick_orig");
        final GameProfile newProfile = new GameProfile(profile.getId(), name);
        server.getPlayerProfileCache().addEntry(newProfile);
        // This is null on login
        if (player != null)
        {
            NameManager.setName(name, player.getGameProfile());
            PlayerDataHandler.getCustomDataTag(player).putString("nick", name);
            if (!PlayerDataHandler.getCustomDataTag(player).contains("nick_orig")) PlayerDataHandler.getCustomDataTag(
                    player).putString("nick_orig", name);
            PlayerDataHandler.saveCustomData(player);
            server.getPlayerList().sendPacketToAllPlayers(new SPlayerListItemPacket(
                    SPlayerListItemPacket.Action.UPDATE_DISPLAY_NAME, player));
        }
        else NameManager.setName(name, profile);
    }

    public NameManager(final DedicatedServer server, final DynamicRegistries.Impl reg, final PlayerData data)
    {
        super(server, reg, data);
    }

    @Override
    public void initializeConnectionToPlayer(final NetworkManager netManager, final ServerPlayerEntity playerIn)
    {
        final GameProfile gameprofile = playerIn.getGameProfile();
        final PlayerProfileCache playerprofilecache = this.getServer().getPlayerProfileCache();

        NameManager.onLogin(playerIn, this.getServer());

        final GameProfile gameprofile1 = playerprofilecache.getProfileByUUID(gameprofile.getId());
        final String s = gameprofile1 == null ? gameprofile.getName() : gameprofile1.getName();
        playerprofilecache.addEntry(gameprofile);
        final CompoundNBT compoundnbt = this.readPlayerDataFromFile(playerIn);
        @SuppressWarnings("deprecation")
        final RegistryKey<World> registrykey = compoundnbt != null ? DimensionType.decodeWorldKey(new Dynamic<>(
                NBTDynamicOps.INSTANCE, compoundnbt.get("Dimension"))).resultOrPartial(
                        DedicatedPlayerList.LOGGER::error).orElse(World.OVERWORLD) : World.OVERWORLD;
        final ServerWorld serverworld = this.server.getWorld(registrykey);
        ServerWorld serverworld1;
        if (serverworld == null)
        {
            DedicatedPlayerList.LOGGER.warn("Unknown respawn dimension {}, defaulting to overworld", registrykey);
            serverworld1 = this.server.func_241755_D_();
        }
        else serverworld1 = serverworld;

        playerIn.setWorld(serverworld1);
        playerIn.interactionManager.setWorld((ServerWorld) playerIn.world);
        String s1 = "local";
        if (netManager.getRemoteAddress() != null) s1 = netManager.getRemoteAddress().toString();

        DedicatedPlayerList.LOGGER.info("{}[{}] logged in with entity id {} at ({}, {}, {})", playerIn.getName()
                .getString(), s1, playerIn.getEntityId(), playerIn.getPosX(), playerIn.getPosY(), playerIn.getPosZ());
        final IWorldInfo iworldinfo = serverworld1.getWorldInfo();
        this.setPlayerGameTypeBasedOnOther(playerIn, (ServerPlayerEntity) null, serverworld1);
        final ServerPlayNetHandler serverplaynethandler = new ServerPlayNetHandler(this.server, netManager, playerIn);
        net.minecraftforge.fml.network.NetworkHooks.sendMCRegistryPackets(netManager, "PLAY_TO_CLIENT");
        final GameRules gamerules = serverworld1.getGameRules();
        final boolean flag = gamerules.getBoolean(GameRules.DO_IMMEDIATE_RESPAWN);
        final boolean flag1 = gamerules.getBoolean(GameRules.REDUCED_DEBUG_INFO);
        serverplaynethandler.sendPacket(new SJoinGamePacket(playerIn.getEntityId(), playerIn.interactionManager
                .getGameType(), playerIn.interactionManager.func_241815_c_(), BiomeManager.getHashedSeed(serverworld1
                        .getSeed()), iworldinfo.isHardcore(), this.server.func_240770_D_(), this.field_232639_s_,
                serverworld1.getDimensionType(), serverworld1.getDimensionKey(), this.getMaxPlayers(), this
                        .getViewDistance(), flag1, !flag, serverworld1.isDebug(), serverworld1.func_241109_A_()));
        serverplaynethandler.sendPacket(new SCustomPayloadPlayPacket(SCustomPayloadPlayPacket.BRAND, new PacketBuffer(
                Unpooled.buffer()).writeString(this.getServer().getServerModName())));
        serverplaynethandler.sendPacket(new SServerDifficultyPacket(iworldinfo.getDifficulty(), iworldinfo
                .isDifficultyLocked()));
        serverplaynethandler.sendPacket(new SPlayerAbilitiesPacket(playerIn.abilities));
        serverplaynethandler.sendPacket(new SHeldItemChangePacket(playerIn.inventory.currentItem));
        serverplaynethandler.sendPacket(new SUpdateRecipesPacket(this.server.getRecipeManager().getRecipes()));
        serverplaynethandler.sendPacket(new STagsListPacket(this.server.func_244266_aF()));
        net.minecraftforge.fml.network.NetworkHooks.syncCustomTagTypes(playerIn, this.server.func_244266_aF());
        this.updatePermissionLevel(playerIn);
        playerIn.getStats().markAllDirty();
        playerIn.getRecipeBook().init(playerIn);
        this.sendScoreboard(serverworld1.getScoreboard(), playerIn);
        this.server.refreshStatusNextTick();
        IFormattableTextComponent iformattabletextcomponent;
        if (playerIn.getGameProfile().getName().equalsIgnoreCase(s))
            iformattabletextcomponent = new TranslationTextComponent("multiplayer.player.joined", playerIn
                    .getDisplayName());
        else iformattabletextcomponent = new TranslationTextComponent("multiplayer.player.joined.renamed", playerIn
                .getDisplayName(), s);

        this.func_232641_a_(iformattabletextcomponent.mergeStyle(TextFormatting.YELLOW), ChatType.SYSTEM,
                Util.DUMMY_UUID);
        serverplaynethandler.setPlayerLocation(playerIn.getPosX(), playerIn.getPosY(), playerIn.getPosZ(),
                playerIn.rotationYaw, playerIn.rotationPitch);
        this.addPlayer(playerIn);
        this.uuidToPlayerMap.put(playerIn.getUniqueID(), playerIn);
        this.sendPacketToAllPlayers(new SPlayerListItemPacket(SPlayerListItemPacket.Action.ADD_PLAYER, playerIn));

        for (final ServerPlayerEntity element : this.players)
            playerIn.connection.sendPacket(new SPlayerListItemPacket(SPlayerListItemPacket.Action.ADD_PLAYER, element));

        serverworld1.addNewPlayer(playerIn);
        this.server.getCustomBossEvents().onPlayerLogin(playerIn);
        this.sendWorldInfo(playerIn, serverworld1);
        if (!this.server.getResourcePackUrl().isEmpty()) playerIn.loadResourcePack(this.server.getResourcePackUrl(),
                this.server.getResourcePackHash());

        for (final EffectInstance effectinstance : playerIn.getActivePotionEffects())
            serverplaynethandler.sendPacket(new SPlayEntityEffectPacket(playerIn.getEntityId(), effectinstance));

        if (compoundnbt != null && compoundnbt.contains("RootVehicle", 10))
        {
            final CompoundNBT compoundnbt1 = compoundnbt.getCompound("RootVehicle");
            final Entity entity1 = EntityType.loadEntityAndExecute(compoundnbt1.getCompound("Entity"), serverworld1, (
                    p_217885_1_) ->
            {
                return !serverworld1.summonEntity(p_217885_1_) ? null : p_217885_1_;
            });
            if (entity1 != null)
            {
                UUID uuid;
                if (compoundnbt1.hasUniqueId("Attach")) uuid = compoundnbt1.getUniqueId("Attach");
                else uuid = null;

                if (entity1.getUniqueID().equals(uuid)) playerIn.startRiding(entity1, true);
                else for (final Entity entity : entity1.getRecursivePassengers())
                    if (entity.getUniqueID().equals(uuid))
                    {
                        playerIn.startRiding(entity, true);
                        break;
                    }

                if (!playerIn.isPassenger())
                {
                    DedicatedPlayerList.LOGGER.warn("Couldn't reattach entity to player");
                    serverworld1.removeEntity(entity1);

                    for (final Entity entity2 : entity1.getRecursivePassengers())
                        serverworld1.removeEntity(entity2);
                }
            }
        }

        playerIn.addSelfToInternalCraftingInventory();
        net.minecraftforge.fml.hooks.BasicEventHooks.firePlayerLoggedIn(playerIn);
    }
}
