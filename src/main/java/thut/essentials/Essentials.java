package thut.essentials;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import net.minecraftforge.fml.event.server.FMLServerStoppingEvent;
import thut.essentials.commands.CommandManager;
import thut.essentials.defuzz.SpawnDefuzzer;
import thut.essentials.economy.EconomyManager;
import thut.essentials.land.LandEventsHandler;
import thut.essentials.land.LandManager;
import thut.essentials.util.PlayerDataHandler;

@Mod(Reference.MODID)
public class Essentials
{
    public static final Config config = new Config();
    // Directly reference a log4j logger.
    public static final Logger LOGGER = LogManager.getLogger();

    public Essentials()
    {
        MinecraftForge.EVENT_BUS.register(this);
        thut.essentials.config.Config.setupConfigs(Essentials.config, Reference.MODID, Reference.MODID);
        if (Essentials.config.defuzz)
        {
            Essentials.LOGGER.info("Registering Defuzzer!");
            MinecraftForge.EVENT_BUS.register(SpawnDefuzzer.class);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void serverStarting(final FMLServerStartingEvent event)
    {
        if (Essentials.config.landEnabled) MinecraftForge.EVENT_BUS.register(LandEventsHandler.TEAMMANAGER);
        if (Essentials.config.shopsEnabled) EconomyManager.getInstance();
        LandEventsHandler.TEAMMANAGER.registerPerms();
        CommandManager.register_commands(event.getCommandDispatcher());
    }

    @SubscribeEvent
    public void serverUnload(final FMLServerStoppingEvent evt)
    {
        if (Essentials.config.landEnabled) MinecraftForge.EVENT_BUS.unregister(LandEventsHandler.TEAMMANAGER);
        if (Essentials.config.shopsEnabled) EconomyManager.clearInstance();
        LandManager.clearInstance();
        PlayerDataHandler.saveAll();
        PlayerDataHandler.clear();
    }
}
