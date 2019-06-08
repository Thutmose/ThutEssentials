package thut.essentials.commands.land.management;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.StringTextComponent;
import thut.essentials.land.LandManager;
import thut.essentials.land.LandManager.LandTeam;
import thut.essentials.util.BaseCommand;
import thut.essentials.util.ConfigManager;

public class Rename extends BaseCommand
{
    public Rename()
    {
        super("renameteam", 0);
    }

    @Override
    public void execute(MinecraftServer server, ICommandSource sender, String[] args) throws CommandException
    {
        PlayerEntity player = getPlayerBySender(sender);
        LandTeam team = LandManager.getTeam(player);
        if (team == null) throw new CommandException("You are not in a team.");
        if (!LandManager.getInstance().isAdmin(player.getUniqueID())
                || team.teamName.equalsIgnoreCase(ConfigManager.INSTANCE.defaultTeamName))
        {
            sender.sendMessage(new StringTextComponent("You are not Authorized to rename your team"));
            return;
        }
        String newName = args[0];
        String oldName = team.teamName;
        LandManager.getInstance().renameTeam(oldName, newName);
        return;
    }
}
