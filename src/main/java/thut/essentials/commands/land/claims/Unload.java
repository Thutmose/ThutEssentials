package thut.essentials.commands.land.claims;

import java.util.logging.Level;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.server.permission.DefaultPermissionLevel;
import net.minecraftforge.server.permission.PermissionAPI;
import thut.essentials.ThutEssentials;
import thut.essentials.land.LandEventsHandler.ChunkLoadHandler;
import thut.essentials.land.LandManager;
import thut.essentials.land.LandManager.LandTeam;
import thut.essentials.util.BaseCommand;
import thut.essentials.util.Coordinate;

public class Unload extends BaseCommand
{
    private static final String BYPASSLIMIT = "thutessentials.land.cload.nolimit";
    private static final String LOADCHUNKS  = "thutessentials.land.cload.load";

    public Unload()
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
        boolean all = false;
        if(args.length>0) all = args[0].equals("all");
        
        if(all)
        {
            for(Coordinate coord : team.land.land)
            {
                LandManager.getInstance().unLoadLand(coord, team);
            }
            sender.sendMessage(new TextComponentString("UnLoaded all land for Team" + team.teamName));
            return;
        }

        int x = MathHelper.floor(sender.getPosition().getX() / 16f);
        int y = MathHelper.floor(sender.getPosition().getY() / 16f);
        int z = MathHelper.floor(sender.getPosition().getZ() / 16f);
        int dim = sender.getEntityWorld().provider.getDimension();

        Coordinate chunk = new Coordinate(x, y, z, dim);
        LandTeam owner = LandManager.getInstance().getLandOwner(chunk);

        if (owner != team) throw new CommandException("You may only unload land owned by your team.");
        if (!ChunkLoadHandler.chunks.containsKey(chunk)) throw new CommandException("Not loaded.");

        LandManager.getInstance().unLoadLand(chunk, team);
        sender.sendMessage(new TextComponentString("UnLoaded subchunk for Team" + team.teamName));
        ThutEssentials.logger.log(Level.FINER, "unload: " + team.teamName + " " + chunk);
    }

}
