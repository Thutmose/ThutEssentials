package thut.essentials.util;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.UUID;

import com.mojang.authlib.GameProfile;

import io.netty.buffer.Unpooled;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
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
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.GameRules;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.storage.WorldInfo;
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

    public static void init(final MinecraftServer server)
    {
        if (!(server instanceof DedicatedServer)) return;
        server.setPlayerList(new NameManager((DedicatedServer) server));
        server.getPlayerList().func_212504_a(server.getWorld(DimensionType.OVERWORLD));
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
            if (!PlayerDataHandler.getCustomDataTag(player).contains("nick_orig"))
                PlayerDataHandler.getCustomDataTag(player).putString("nick_orig", name);
            PlayerDataHandler.saveCustomData(player);
            server.getPlayerList().sendPacketToAllPlayers(
                    new SPlayerListItemPacket(SPlayerListItemPacket.Action.UPDATE_DISPLAY_NAME, player));
        }
        else NameManager.setName(name, profile);
    }

    public NameManager(final DedicatedServer p_i1503_1_)
    {
        super(p_i1503_1_);
    }

    @Override
    public void initializeConnectionToPlayer(final NetworkManager netManager, final ServerPlayerEntity playerIn)
    {
        GameProfile gameprofile = playerIn.getGameProfile();
        PlayerProfileCache playerprofilecache = this.getServer().getPlayerProfileCache();

        NameManager.onLogin(playerIn, this.getServer());

        GameProfile gameprofile1 = playerprofilecache.getProfileByUUID(gameprofile.getId());
        String s = gameprofile1 == null ? gameprofile.getName() : gameprofile1.getName();
        playerprofilecache.addEntry(gameprofile);
        CompoundNBT compoundnbt = this.readPlayerDataFromFile(playerIn);
        ServerWorld serverworld = this.getServer().getWorld(playerIn.dimension);

        // Forge: Make sure the dimension hasn't been deleted, if so stick them
        // in the overworld.
        if (serverworld == null)
        {
            playerIn.dimension = DimensionType.OVERWORLD;
            serverworld = this.getServer().getWorld(playerIn.dimension);
            playerIn.setPosition(serverworld.getWorldInfo().getSpawnX(), serverworld.getWorldInfo().getSpawnY(),
                    serverworld.getWorldInfo().getSpawnZ());
        }

        playerIn.setWorld(serverworld);
        playerIn.interactionManager.setWorld((ServerWorld) playerIn.world);
        String s1 = "local";
        if (netManager.getRemoteAddress() != null)
        {
            s1 = netManager.getRemoteAddress().toString();
        }

        Essentials.LOGGER.info("{}[{}] logged in with entity id {} at ({}, {}, {})", playerIn.getName().getString(), s1,
                playerIn.getEntityId(), playerIn.getPosX(), playerIn.getPosY(), playerIn.getPosZ());
        WorldInfo worldinfo = serverworld.getWorldInfo();
        this.setPlayerGameTypeBasedOnOther(playerIn, (ServerPlayerEntity) null, serverworld);
        ServerPlayNetHandler serverplaynethandler = new ServerPlayNetHandler(this.getServer(), netManager, playerIn);
        net.minecraftforge.fml.network.NetworkHooks.sendMCRegistryPackets(netManager, "PLAY_TO_CLIENT");
        net.minecraftforge.fml.network.NetworkHooks.sendDimensionDataPacket(netManager, playerIn);
        GameRules gamerules = serverworld.getGameRules();
        boolean flag = gamerules.getBoolean(GameRules.DO_IMMEDIATE_RESPAWN);
        boolean flag1 = gamerules.getBoolean(GameRules.REDUCED_DEBUG_INFO);
        serverplaynethandler.sendPacket(new SJoinGamePacket(playerIn.getEntityId(),
                playerIn.interactionManager.getGameType(), WorldInfo.byHashing(worldinfo.getSeed()),
                worldinfo.isHardcore(), serverworld.dimension.getType(), this.getMaxPlayers(), worldinfo.getGenerator(),
                this.getViewDistance(), flag1, !flag));
        serverplaynethandler.sendPacket(new SCustomPayloadPlayPacket(SCustomPayloadPlayPacket.BRAND,
                (new PacketBuffer(Unpooled.buffer())).writeString(this.getServer().getServerModName())));
        serverplaynethandler
                .sendPacket(new SServerDifficultyPacket(worldinfo.getDifficulty(), worldinfo.isDifficultyLocked()));
        serverplaynethandler.sendPacket(new SPlayerAbilitiesPacket(playerIn.abilities));
        serverplaynethandler.sendPacket(new SHeldItemChangePacket(playerIn.inventory.currentItem));
        serverplaynethandler.sendPacket(new SUpdateRecipesPacket(this.getServer().getRecipeManager().getRecipes()));
        serverplaynethandler.sendPacket(new STagsListPacket(this.getServer().getNetworkTagManager()));
        this.updatePermissionLevel(playerIn);
        playerIn.getStats().markAllDirty();
        playerIn.getRecipeBook().init(playerIn);
        this.sendScoreboard(serverworld.getScoreboard(), playerIn);
        this.getServer().refreshStatusNextTick();
        ITextComponent itextcomponent;
        if (playerIn.getGameProfile().getName().equalsIgnoreCase(s))
        {
            itextcomponent = new TranslationTextComponent("multiplayer.player.joined", playerIn.getDisplayName());
        }
        else
        {
            itextcomponent = new TranslationTextComponent("multiplayer.player.joined.renamed",
                    playerIn.getDisplayName(), s);
        }

        this.sendMessage(itextcomponent.applyTextStyle(TextFormatting.YELLOW));
        serverplaynethandler.setPlayerLocation(playerIn.getPosX(), playerIn.getPosY(), playerIn.getPosZ(),
                playerIn.rotationYaw, playerIn.rotationPitch);
        this.addPlayer(playerIn);
        this.uuidToPlayerMap.put(playerIn.getUniqueID(), playerIn);
        this.sendPacketToAllPlayers(new SPlayerListItemPacket(SPlayerListItemPacket.Action.ADD_PLAYER, playerIn));

        for (int i = 0; i < this.getPlayers().size(); ++i)
        {
            playerIn.connection.sendPacket(
                    new SPlayerListItemPacket(SPlayerListItemPacket.Action.ADD_PLAYER, this.getPlayers().get(i)));
        }

        serverworld.addNewPlayer(playerIn);
        this.getServer().getCustomBossEvents().onPlayerLogin(playerIn);
        this.sendWorldInfo(playerIn, serverworld);
        if (!this.getServer().getResourcePackUrl().isEmpty())
        {
            playerIn.loadResourcePack(this.getServer().getResourcePackUrl(), this.getServer().getResourcePackHash());
        }

        for (EffectInstance effectinstance : playerIn.getActivePotionEffects())
        {
            serverplaynethandler.sendPacket(new SPlayEntityEffectPacket(playerIn.getEntityId(), effectinstance));
        }

        if (compoundnbt != null && compoundnbt.contains("RootVehicle", 10))
        {
            CompoundNBT compoundnbt1 = compoundnbt.getCompound("RootVehicle");
            final ServerWorld worldf = serverworld;
            Entity entity1 = EntityType.func_220335_a(compoundnbt1.getCompound("Entity"), serverworld, (p_217885_1_) ->
            {
                return !worldf.summonEntity(p_217885_1_) ? null : p_217885_1_;
            });
            if (entity1 != null)
            {
                UUID uuid = compoundnbt1.getUniqueId("Attach");
                if (entity1.getUniqueID().equals(uuid))
                {
                    playerIn.startRiding(entity1, true);
                }
                else
                {
                    for (Entity entity : entity1.getRecursivePassengers())
                    {
                        if (entity.getUniqueID().equals(uuid))
                        {
                            playerIn.startRiding(entity, true);
                            break;
                        }
                    }
                }

                if (!playerIn.isPassenger())
                {
                    Essentials.LOGGER.warn("Couldn't reattach entity to player");
                    serverworld.removeEntity(entity1);

                    for (Entity entity2 : entity1.getRecursivePassengers())
                    {
                        serverworld.removeEntity(entity2);
                    }
                }
            }
        }

        playerIn.addSelfToInternalCraftingInventory();
        net.minecraftforge.fml.hooks.BasicEventHooks.firePlayerLoggedIn(playerIn);
    }
}
