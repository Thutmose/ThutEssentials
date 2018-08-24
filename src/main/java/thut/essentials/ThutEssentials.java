package thut.essentials;

import java.io.File;
import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.minecraft.command.CommandHandler;
import net.minecraft.command.ICommand;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.Mod.Instance;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartedEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.event.FMLServerStoppingEvent;
import thut.essentials.commands.CommandManager;
import thut.essentials.defuzz.SpawnDefuzzer;
import thut.essentials.economy.EconomyManager;
import thut.essentials.itemcontrol.ItemControl;
import thut.essentials.land.LandEventsHandler;
import thut.essentials.land.LandManager;
import thut.essentials.util.ChatHandler;
import thut.essentials.util.ConfigManager;
import thut.essentials.util.HomeManager;
import thut.essentials.util.LogFormatter;
import thut.essentials.util.PlayerDataHandler;
import thut.essentials.world.WorldManager;

@Mod(modid = ThutEssentials.MODID, name = "Thut Essentials", version = ThutEssentials.VERSION, updateJSON = ThutEssentials.UPDATEURL, acceptableRemoteVersions = "*")
public class ThutEssentials
{
    public static final String     MODID     = Reference.MODID;
    public static final String     VERSION   = Reference.VERSION;
    public static final String     UPDATEURL = "";

    @Instance(MODID)
    public static ThutEssentials   instance;

    public static Logger           logger    = Logger.getLogger(MODID);

    public ConfigManager           config;
    private CommandManager         manager;
    public SpawnDefuzzer           defuz     = new SpawnDefuzzer();
    public ItemControl             items     = new ItemControl();
    public final LandEventsHandler teams     = new LandEventsHandler();
    public boolean                 loaded    = false;

    public ThutEssentials()
    {
        initLogger();
    }

    private void initLogger()
    {
        FileHandler logHandler = null;
        logger.setLevel(Level.ALL);
        try
        {
            File logs = new File("." + File.separator + "logs");
            logs.mkdirs();
            File logfile = new File(logs, MODID + ".log");
            if ((logfile.exists() || logfile.createNewFile()) && logfile.canWrite() && logHandler == null)
            {
                logHandler = new FileHandler(logfile.getPath());
                logHandler.setFormatter(new LogFormatter());
                logger.addHandler(logHandler);
            }
        }
        catch (SecurityException | IOException e)
        {
            e.printStackTrace();
        }
    }

    @EventHandler
    public void preInit(FMLPreInitializationEvent e)
    {
        config = new ConfigManager(e.getSuggestedConfigurationFile());
        manager = new CommandManager();
    }

    @EventHandler
    public void serverLoad(FMLServerStartingEvent event)
    {
        if (config.chatTweaks)
        {
            MinecraftForge.EVENT_BUS.register(new ChatHandler());
        }
        loaded = true;
        teams.registerPerms();
        HomeManager.registerPerms();
        manager.registerCommands(event);
        if (config.landEnabled) LandManager.getInstance();
        if (config.economyEnabled) EconomyManager.getInstance();
        try
        {
            WorldManager.onServerStart(event);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
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
    }
}
