package thut.essentials.commands.land;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.common.MinecraftForge;
import thut.essentials.commands.CommandManager;
import thut.essentials.events.ClaimLandEvent;
import thut.essentials.land.LandManager;
import thut.essentials.land.LandManager.LandTeam;
import thut.essentials.util.BaseCommand;
import thut.essentials.util.ConfigManager;
import thut.essentials.util.Coordinate;

public class Claim extends BaseCommand
{

    public Claim()
    {
        super("claim", 0);
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException
    {
        EntityPlayer player = getCommandSenderAsPlayer(sender);
        LandTeam team = LandManager.getTeam(player);
        if (team == LandManager.getDefaultTeam())
            throw new CommandException("You are not in a team that can claim land.");
        boolean isOp = CommandManager.isOp(sender);
        int teamCount = team.member.size();
        int count = LandManager.getInstance().countLand(team.teamName);
        boolean up = false;
        boolean all = false;
        int num = 1;
        int radius = 0;

        if (!team.hasPerm(player.getUniqueID(), LandTeam.CLAIMPERM))
            throw new CommandException("You are not allowed to do that.");

        if (args.length > 1)
        {
            try
            {
                if (args[1].equalsIgnoreCase("up") || args[1].equalsIgnoreCase("down"))
                {
                    num = Integer.parseInt(args[0]);
                    up = args[1].equalsIgnoreCase("up");
                }
                if (args[1].equalsIgnoreCase("all") || args[0].equalsIgnoreCase("all"))
                {
                    all = true;
                    up = true;
                    num = 16;
                }
                if (args.length > 3 || all)
                {
                    radius = all ? Integer.parseInt(args[1]) : Integer.parseInt(args[2]);
                }
            }
            catch (NumberFormatException e)
            {
                throw new CommandException("Error in formating number of chunks");
            }
        }
        else if (args.length > 0)
        {
            if (args[0].equalsIgnoreCase("all"))
            {
                all = true;
                up = true;
                num = 16;
            }
        }
        int x = MathHelper.floor_double(sender.getPosition().getX() / 16f);
        int z = MathHelper.floor_double(sender.getPosition().getZ() / 16f);
        int n = 0;
        for (int dx = -radius; dx <= radius; dx++)
            for (int dz = -radius; dz <= radius; dz++)
                for (int i = 0; i < num; i++)
                {
                    if (count < teamCount * ConfigManager.INSTANCE.teamLandPerPlayer || isOp)
                    {
                        int dir = up ? 1 : -1;
                        teamCount = team.member.size();
                        count = LandManager.getInstance().countLand(team.teamName);
                        int y = MathHelper.floor_double(sender.getPosition().getY() / 16f) + i * dir;
                        if (all) y = i * dir;
                        int dim = sender.getEntityWorld().provider.getDimension();
                        if (y < 0 || y > 15) continue;
                        Coordinate chunk = new Coordinate(x + dx, y, z + dz, dim);
                        LandTeam owner = LandManager.getInstance().getLandOwner(chunk);
                        ClaimLandEvent event = new ClaimLandEvent(new BlockPos(x + dx, y, z + dz), dim, player,
                                team.teamName);
                        MinecraftForge.EVENT_BUS.post(event);
                        if (event.isCanceled()) continue;
                        if (owner != null)
                        {
                            if (owner.equals(team)) continue;
                            sender.addChatMessage(new TextComponentString("This land is already claimed by " + owner));
                            continue;
                        }
                        n++;
                        LandManager.getInstance().addTeamLand(team.teamName, chunk, true);
                    }
                    else
                    {
                        sender.addChatMessage(
                                new TextComponentString("Claimed " + n + " subchunks for Team" + team.teamName));
                        return;
                    }
                }
        sender.addChatMessage(new TextComponentString("Claimed " + n + " subchunks for Team" + team.teamName));
        return;
    }

}
