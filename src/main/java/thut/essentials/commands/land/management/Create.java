package thut.essentials.commands.land.management;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.StringTextComponent;
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
    public String getUsage(ICommandSource sender)
    {
        return "/" + getName() + " <team name>";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSource sender, String[] args) throws CommandException
    {
        if (args.length != 1) throw new CommandException(getUsage(sender));
        String teamname = args[0];
        PlayerEntity player = getPlayerBySender(sender);
        IPermissionHandler manager = PermissionAPI.getPermissionHandler();
        PlayerContext context = new PlayerContext(player);
        if (!manager.hasPermission(player.getGameProfile(), LandEventsHandler.PERMCREATETEAM, context))
            throw new CommandException("You are not allowed to create a team");
        LandManager.getInstance().createTeam(player.getUniqueID(), teamname);
        player.sendMessage(new StringTextComponent("You created Team " + teamname));
    }
}
