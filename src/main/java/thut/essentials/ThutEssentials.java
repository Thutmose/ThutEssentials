package thut.essentials;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.common.collect.ListMultimap;
import com.google.common.io.Files;

import net.minecraft.command.CommandHandler;
import net.minecraft.command.ICommand;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeChunkManager;
import net.minecraftforge.common.ForgeChunkManager.PlayerOrderedLoadingCallback;
import net.minecraftforge.common.ForgeChunkManager.Ticket;
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
            // Backup the log
            if (logfile.exists())
            {
                DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
                Date date = new Date();
                logs = new File(logs, MODID);
                logs.mkdirs();
                try
                {
                    File newFile = new File(logs, dateFormat.format(date) + ".log");
                    Files.move(logfile, newFile);
                }
                catch (IOException e)
                {
                    //Probably another minecraft instance running.
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
            if (logfile.createNewFile() && logfile.canWrite() && logHandler == null)
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

        // Initialize the chunk loading stuff
        ForgeChunkManager.setForcedChunkLoadingCallback(this, new PlayerOrderedLoadingCallback()
        {
            @Override
            public void ticketsLoaded(List<Ticket> tickets, World world)
            {
                Iterator<Ticket> next = tickets.iterator();
                while (next.hasNext())
                {
                    Ticket ticket = next.next();
                    if (!ticket.getModId().equals(ThutEssentials.MODID)) continue;
                    if (!ticket.isPlayerTicket() || !ConfigManager.INSTANCE.chunkLoading)
                    {
                        ForgeChunkManager.releaseTicket(ticket);
                        continue;
                    }
                    int[] pos = ticket.getModData().getIntArray("pos");
                    if (pos.length != 2)
                    {
                        logger.log(Level.FINER, "invalid ticket for " + pos);
                        ForgeChunkManager.releaseTicket(ticket);
                    }
                    else
                    {
                        ChunkPos location = new ChunkPos(pos[0], pos[1]);
                        logger.log(Level.FINER, "Forcing Chunk at " + location);
                        ForgeChunkManager.forceChunk(ticket, location);
                    }
                }
            }

            @Override
            public ListMultimap<String, Ticket> playerTicketsLoaded(ListMultimap<String, Ticket> tickets, World world)
            {
                return tickets;
            }
        });
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
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        CommandHandler ch = (CommandHandler) server.getCommandManager();
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
        // Run these commands as server first starts.
        for (String s : ConfigManager.INSTANCE.serverInitCommands)
            ch.executeCommand(server, s);
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
