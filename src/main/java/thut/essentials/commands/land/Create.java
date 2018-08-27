package thut.essentials.commands.land;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.server.permission.IPermissionHandler;
import net.minecraftforge.server.permission.PermissionAPI;
import net.minecraftforge.server.permission.context.PlayerContext;
import thut.essentials.land.LandEventsHandler;
import thut.essentials.land.LandManager;
import thut.essentials.util.BaseCommand;

public class Create extends BaseCommand
{
    public Create()
    {
        super("createteam", 0);
    }

    @Override
    public String getUsage(ICommandSender sender)
    {
        return "/" + getName() + " <team name>";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException
    {
        if (args.length != 1) throw new CommandException(getUsage(sender));
        String teamname = args[0];
        EntityPlayer player = getPlayerBySender(sender);
        IPermissionHandler manager = PermissionAPI.getPermissionHandler();
        PlayerContext context = new PlayerContext(player);
        if (!manager.hasPermission(player.getGameProfile(), LandEventsHandler.PERMCREATETEAM, context))
            throw new CommandException("You are not allowed to create a team");
        LandManager.getInstance().createTeam(player.getUniqueID(), teamname);
        player.sendMessage(new TextComponentString("You created Team " + teamname));
    }
}
