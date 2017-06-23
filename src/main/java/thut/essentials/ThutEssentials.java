package thut.essentials;

import net.minecraft.command.CommandHandler;
import net.minecraft.command.ICommand;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.CommandEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.Mod.Instance;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartedEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.event.FMLServerStoppingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent;
import thut.essentials.commands.CommandManager;
import thut.essentials.defuzz.SpawnDefuzzer;
import thut.essentials.economy.EconomyManager;
import thut.essentials.economy.EconomySaveHandler;
import thut.essentials.itemcontrol.ItemControl;
import thut.essentials.land.LandEventsHandler;
import thut.essentials.land.LandManager;
import thut.essentials.land.LandSaveHandler;
import thut.essentials.util.ConfigManager;
import thut.essentials.util.DefaultPermissions;
import thut.essentials.util.IPermissionHandler;
import thut.essentials.util.PlayerDataHandler;

@Mod(modid = ThutEssentials.MODID, name = "Thut Essentials", version = ThutEssentials.VERSION, dependencies = "", updateJSON = ThutEssentials.UPDATEURL, acceptableRemoteVersions = "*")
public class ThutEssentials
{
    public static final String       MODID     = Reference.MODID;
    public static final String       VERSION   = Reference.VERSION;
    public static final String       UPDATEURL = "";

    @Instance(MODID)
    public static ThutEssentials     instance;

    public static IPermissionHandler perms     = new DefaultPermissions();

    public ConfigManager             config;
    private CommandManager           manager;
    private SpawnDefuzzer            defuz     = new SpawnDefuzzer();

    @EventHandler
    public void preInit(FMLPreInitializationEvent e)
    {
        config = new ConfigManager(e.getSuggestedConfigurationFile());
        LandEventsHandler teams = new LandEventsHandler();
        MinecraftForge.EVENT_BUS.register(teams);
        new ItemControl();
    }

    @EventHandler
    public void serverLoad(FMLServerStartingEvent event)
    {
        manager = new CommandManager(event);
        MinecraftForge.EVENT_BUS.register(this);
        LandSaveHandler.loadGlobalData();
        EconomySaveHandler.loadGlobalData();
        if (config.spawnDefuzz && FMLCommonHandler.instance().getMinecraftServerInstance().isDedicatedServer())
            MinecraftForge.EVENT_BUS.register(defuz);
    }

    @EventHandler
    public void serverStarted(FMLServerStartedEvent evt)
    {
        CommandHandler ch = (CommandHandler) FMLCommonHandler.instance().getMinecraftServerInstance()
                .getCommandManager();
        for (String s : ConfigManager.INSTANCE.alternateCommands)
        {
            String[] args = s.split(":");
            ICommand command = ch.getCommands().get(args[0]);
            if (command == null)
            {
                System.err.println("No Command found for " + args[0]);
                continue;
            }
            for (int i = 1; i < args.length; i++)
            {
                ch.getCommands().put(args[i], command);
            }
        }
    }

    @EventHandler
    public void serverUnload(FMLServerStoppingEvent evt)
    {
        PlayerDataHandler.saveAll();
        PlayerDataHandler.clear();
        LandManager.clearInstance();
        EconomyManager.clearInstance();
        manager.clear();
        if (config.spawnDefuzz && FMLCommonHandler.instance().getMinecraftServerInstance().isDedicatedServer())
            MinecraftForge.EVENT_BUS.unregister(defuz);
    }

    @SubscribeEvent
    void commandUseEvent(CommandEvent event)
    {

    }

    @SubscribeEvent
    public void PlayerLoggin(PlayerLoggedInEvent evt)
    {

    }
}
