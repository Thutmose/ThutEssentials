package thut.essentials;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import thut.essentials.commands.CommandManager;
import thut.essentials.land.LandEventsHandler;

@Mod(Reference.MODID)
@EventBusSubscriber
public class Essentials
{
    public static final Config config = new Config();
    // Directly reference a log4j logger.
    public static final Logger LOGGER = LogManager.getLogger();

    public Essentials()
    {
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void serverStarting(final FMLServerStartingEvent event)
    {
        LandEventsHandler.TEAMMANAGER.registerPerms();
        CommandManager.register_commands(event.getCommandDispatcher());
    }
}
