package thut.essentials.commands.land.claims;

import java.util.logging.Level;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.server.permission.DefaultPermissionLevel;
import net.minecraftforge.server.permission.PermissionAPI;
import thut.essentials.ThutEssentials;
import thut.essentials.events.LoadLandEvent;
import thut.essentials.land.LandEventsHandler.ChunkLoadHandler;
import thut.essentials.land.LandManager;
import thut.essentials.land.LandManager.LandTeam;
import thut.essentials.util.BaseCommand;
import thut.essentials.util.ConfigManager;
import thut.essentials.util.Coordinate;

public class Load extends BaseCommand
{
    private static final String BYPASSLIMIT = "thutessentials.land.cload.nolimit";
    private static final String LOADCHUNKS  = "thutessentials.land.cload.load";

    public Load()
    {
        super("cload", 0);
        PermissionAPI.registerNode(BYPASSLIMIT, DefaultPermissionLevel.OP,
                "Permission to bypass the chunk loaded land per player limit for a team.");
        PermissionAPI.registerNode(LOADCHUNKS, DefaultPermissionLevel.ALL, "Permission to load chunks.");
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException
    {
        EntityPlayer player = getPlayerBySender(sender);
        LandTeam team = LandManager.getTeam(player);
        if (team == LandManager.getDefaultTeam())
            throw new CommandException("You are not in a team that can claim land.");

        if (!team.hasRankPerm(player.getUniqueID(), LandTeam.LOADPERM))
            throw new CommandException("You are not allowed to do that.");

        int x = MathHelper.floor(sender.getPosition().getX() / 16f);
        int y = MathHelper.floor(sender.getPosition().getY() / 16f);
        int z = MathHelper.floor(sender.getPosition().getZ() / 16f);
        int dim = sender.getEntityWorld().provider.getDimension();

        Coordinate chunk = new Coordinate(x, y, z, dim);
        LandTeam owner = LandManager.getInstance().getLandOwner(chunk);

        if (owner != team) throw new CommandException("You may only load land owned by your team.");
        int maxLoaded = team.maxLoaded < 0 ? ConfigManager.INSTANCE.loadedChunksPerTeam : team.maxLoaded;
        if (maxLoaded >= team.land.loaded)
            throw new CommandException("You have too much land loaded, please unload some first.");
        if (ChunkLoadHandler.chunks.containsKey(chunk)) throw new CommandException("Already loaded.");

        LoadLandEvent event = new LoadLandEvent(new BlockPos(x, y, z), dim, player, team.teamName);
        MinecraftForge.EVENT_BUS.post(event);
        if (!event.isCanceled())
        {
            LandManager.getInstance().loadLand(player.getEntityWorld(), chunk, team);
            sender.sendMessage(new TextComponentString("Loaded subchunk for Team" + team.teamName));
            ThutEssentials.logger.log(Level.FINER, "load: " + team.teamName + " " + chunk);
        }
    }

}
