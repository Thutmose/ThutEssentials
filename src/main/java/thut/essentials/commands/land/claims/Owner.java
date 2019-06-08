package thut.essentials.commands.land.claims;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSource;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.StringTextComponent;
import thut.essentials.land.LandEventsHandler.ChunkLoadHandler;
import thut.essentials.land.LandManager;
import thut.essentials.land.LandManager.LandTeam;
import thut.essentials.util.BaseCommand;
import thut.essentials.util.Coordinate;

public class Owner extends BaseCommand
{

    public Owner()
    {
        super("landowner", 0);
    }

    @Override
    public void execute(MinecraftServer server, ICommandSource sender, String[] args) throws CommandException
    {
        int x = MathHelper.floor(sender.getPosition().getX() / 16f);
        int y = MathHelper.floor(sender.getPosition().getY() / 16f);
        int z = MathHelper.floor(sender.getPosition().getZ() / 16f);
        int dim = sender.getEntityWorld().dimension.getDimension();
        Coordinate c = new Coordinate(x, y, z, dim);
        LandTeam owner = LandManager.getInstance().getLandOwner(c);
        if (owner == null) sender.sendMessage(new StringTextComponent("This Land is not owned"));
        else
        {
            sender.sendMessage(new StringTextComponent("This Land is owned by Team " + owner.teamName));
            if (ChunkLoadHandler.chunks.containsKey(c))
                sender.sendMessage(new StringTextComponent("This Land is Chunk Loaded."));
        }
    }

}
