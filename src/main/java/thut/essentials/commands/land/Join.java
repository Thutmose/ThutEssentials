package thut.essentials.commands.land;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;
import thut.essentials.commands.CommandManager;
import thut.essentials.land.LandManager;
import thut.essentials.land.LandManager.LandTeam;
import thut.essentials.util.BaseCommand;
import thut.essentials.util.ConfigManager;

public class Join extends BaseCommand
{

    public Join()
    {
        super("joinTeam", 0);
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException
    {
        boolean isOp = CommandManager.isOp(sender);
        EntityPlayer player = getCommandSenderAsPlayer(sender);
        String teamname = args[0];
        LandTeam teamtojoin = LandManager.getInstance().getTeam(teamname, false);
        if (teamtojoin != null)
        {
            boolean empty = teamtojoin.teamName.equalsIgnoreCase(ConfigManager.INSTANCE.defaultTeamName);
            if (empty)
            {
                empty = teamtojoin.member.size() == 0;
                empty = empty && !LandManager.getInstance().getTeam(teamname, false).reserved;
            }
            if (empty || isOp)
            {
                LandManager.getInstance().addToTeam(player.getUniqueID(), teamname);
                LandManager.getInstance().addAdmin(player.getUniqueID(), teamname);
                player.addChatMessage(new TextComponentString("You joined Team " + teamname));
                return;
            }
        }
        if (LandManager.getInstance().hasInvite(player.getUniqueID(), teamname) || isOp)
            LandManager.getInstance().addToTeam(player.getUniqueID(), teamname);
        else sender.addChatMessage(new TextComponentString("You do not have an invite for Team " + teamname));
    }

}
