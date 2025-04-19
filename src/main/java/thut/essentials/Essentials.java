package thut.essentials;

import net.minecraft.server.MinecraftServer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.appender.FileAppender;
import thut.essentials.commands.CommandManager;
import thut.essentials.defuzz.SpawnDefuzzer;
import thut.essentials.economy.EconomyManager;
import thut.essentials.land.ClaimedCapability;
import thut.essentials.land.LandEventsHandler;
import thut.essentials.land.LandEventsHandler.ChunkLoadHandler;
import thut.essentials.util.CmdScheduler;
import thut.essentials.util.MobManager;
import thut.essentials.util.PlayerDataHandler;
import thut.essentials.util.world.DimVersionManager;
import thut.essentials.util.world.TickScheduler;
import thut.essentials.util.world.WorldStructures;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Mod(Essentials.MODID)
public class Essentials
{
    public static final String MODID = "thutessentials";
    public static final Config config = new Config();
    // Directly reference a log4j logger.
    public static final Logger LOGGER = LogManager.getLogger(Essentials.MODID);

    public static MinecraftServer server = null;

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS = DeferredRegister.create(
            NeoForgeRegistries.Keys.ATTACHMENT_TYPES, MODID);

    public Essentials(IEventBus bus, ModContainer container)
    {
        NeoForge.EVENT_BUS.register(this);
        thut.essentials.config.Config.setupConfigs(container, Essentials.config, Essentials.MODID, Essentials.MODID);
        final String log = Essentials.MODID;
        final File logfile = FMLPaths.GAMEDIR.get().resolve("logs").resolve(log + ".log").toFile();
        if (logfile.exists())
        {
            FMLPaths.GAMEDIR.get().resolve("logs").resolve("old").toFile().mkdirs();
            try
            {
                final DateTimeFormatter dtf = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
                Files.move(FMLPaths.GAMEDIR.get().resolve("logs").resolve(log + ".log"),
                        FMLPaths.GAMEDIR.get().resolve("logs").resolve("old").resolve(
                                String.format("%s_%s%s", log, LocalDateTime.now().format(dtf).replace(":", "-"),
                                        ".log")));
            }
            catch (final IOException e)
            {
                e.printStackTrace();
            }
        }
        final org.apache.logging.log4j.core.Logger logger = (org.apache.logging.log4j.core.Logger) Essentials.LOGGER;
        final FileAppender appender = FileAppender.newBuilder().withFileName(logfile.getAbsolutePath())
                .setName(Essentials.MODID).build();
        logger.addAppender(appender);
        appender.start();

        // This won't actually do anything unless config is enabled.
        NeoForge.EVENT_BUS.register(ChunkLoadHandler.class);

        // Register the mob grief preventer
        NeoForge.EVENT_BUS.register(MobManager.class);

        if (Essentials.config.defuzz)
        {
            Essentials.LOGGER.info("Registering Defuzzer!");
            NeoForge.EVENT_BUS.register(SpawnDefuzzer.class);
        }
        ATTACHMENTS.register(bus);
        ClaimedCapability.setup(ATTACHMENTS);

        bus.addListener(this::setup);
    }

    public void setup(final FMLCommonSetupEvent event)
    {
        // Initialize the world structure tracker
        WorldStructures.setup();
        DimVersionManager.init();
        NeoForge.EVENT_BUS.addListener(TickScheduler::onWorldTickPost);
        NeoForge.EVENT_BUS.addListener(TickScheduler::onWorldTickPre);
        NeoForge.EVENT_BUS.addListener(CmdScheduler::onTick);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void serverAboutStart(final ServerAboutToStartEvent event)
    {
        server = event.getServer();
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void serverStarting(final ServerStartingEvent event)
    {
        if (Essentials.config.landEnabled) NeoForge.EVENT_BUS.register(LandEventsHandler.TEAMMANAGER);
        if (Essentials.config.shopsEnabled) EconomyManager.getInstance();
        LandEventsHandler.ChunkLoadHandler.server = event.getServer();
        Essentials.LOGGER.info("Server Started");
    }

    @SubscribeEvent
    public void serverStarted(final ServerStartedEvent event)
    {
        if (Essentials.config.landEnabled) NeoForge.EVENT_BUS.register(LandEventsHandler.TEAMMANAGER);
        if (Essentials.config.shopsEnabled) EconomyManager.getInstance();
        LandEventsHandler.TEAMMANAGER.onServerStarted();
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void registerServerCommands(final RegisterCommandsEvent event)
    {
        CommandManager.register_commands(event.getDispatcher());
    }

    @SubscribeEvent
    public void serverUnload(final ServerStoppingEvent evt)
    {
        if (Essentials.config.landEnabled) NeoForge.EVENT_BUS.unregister(LandEventsHandler.TEAMMANAGER);
        if (Essentials.config.shopsEnabled) EconomyManager.clearInstance();
        LandEventsHandler.TEAMMANAGER.onServerStopped();
        PlayerDataHandler.saveAll();
        PlayerDataHandler.clear();
        Essentials.LOGGER.info("Server Stopped");
    }
}
