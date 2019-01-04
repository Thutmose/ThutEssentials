package thut.essentials.commands.land.management;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;
import thut.essentials.land.LandManager;
import thut.essentials.land.LandManager.LandTeam;
import thut.essentials.util.BaseCommand;

public class AddToTeam extends BaseCommand
{

    public AddToTeam()
    {
        super("addtoteam", 2);
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException
    {
        EntityPlayer player = getPlayer(server, sender, args[0]);
        String teamname = args[1];
        LandTeam teamtojoin = LandManager.getInstance().getTeam(teamname, false);
        LandTeam oldTeam = LandManager.getTeam(player);
        if (oldTeam == teamtojoin) throw new CommandException(player.getName()+" is already in that team!");
        if (teamtojoin != null)
        {
            LandManager.getInstance().addToTeam(player.getUniqueID(), teamname);
            LandManager.getInstance().addAdmin(player.getUniqueID(), teamname);
            player.sendMessage(new TextComponentString("You joined Team " + teamname));
            return;
        }
        else throw new CommandException("No team found by name " + teamname);
    }

}
