package thut.essentials.commands.rtp;

import java.util.Random;

import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import thut.essentials.ThutEssentials;
import thut.essentials.commands.misc.Spawn;
import thut.essentials.commands.misc.Spawn.PlayerMover;
import thut.essentials.util.BaseCommand;
import thut.essentials.util.ConfigManager;

public class RTP extends BaseCommand
{

    public RTP()
    {
        super("rtp", 0);
    }

    /** Return whether the specified command parameter index is a username
     * parameter. */
    @Override
    public boolean isUsernameIndex(String[] args, int index)
    {
        return index == 0;
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException
    {
        EntityPlayer player;
        if (args.length == 1)
        {
            if (sender instanceof EntityPlayer)
                if (!ThutEssentials.perms.hasPermission((EntityPlayer) sender, "rtp.other"))
                    throw new CommandException("You do not have permission to RTP someone else.");
            player = getPlayer(server, sender, args[0]);
        }
        else
        {
            player = getCommandSenderAsPlayer(sender);
        }
        BlockPos position;
        int n = 100;
        while ((position = checkSpot(player)) == null && n-- > 0)
            ;
        if (position != null) PlayerMover.setMove(player, ThutEssentials.instance.config.rtpActivateDelay,
                player.dimension, position, null, Spawn.INTERUPTED);
        else sender.sendMessage(new TextComponentString("No spot found."));
    }

    private boolean isValid(IBlockState ground, IBlockState lower, IBlockState upper)
    {
        return ground.isBlockNormalCube() && validMaterial(lower.getMaterial()) && validMaterial(upper.getMaterial());
    }

    private boolean validMaterial(Material mat)
    {
        return !mat.isSolid() && !mat.isLiquid();
    }

    // Calculate the next position to check.
    private BlockPos calculatePos()
    {
        Random rand = new Random();
        int x = (int) (Math.signum(rand.nextGaussian()) * rand.nextInt(ConfigManager.INSTANCE.rtpdistance));
        int z = (int) (Math.signum(rand.nextGaussian()) * rand.nextInt(ConfigManager.INSTANCE.rtpdistance));
        int y = 252;
        return new BlockPos(x, y, z);
    }

    private BlockPos checkSpot(EntityPlayer player)
    {
        BlockPos position = calculatePos();
        IBlockState ground = player.getEntityWorld().getBlockState(position);
        IBlockState lower = player.getEntityWorld().getBlockState(position.up());
        IBlockState upper = player.getEntityWorld().getBlockState(position.up(2));
        // If the block we're standing on is air or liquid or if the two blocks
        // we take up are liquid then return null, check failed.
        if (isValid(ground, lower, upper)) { return position; }
        if (ground.getMaterial().isLiquid()) return null;
        while (!isValid(ground, lower, upper) && position.getY() > 0)
        {
            // Make a new position of y+1
            position = position.down();
            ground = player.getEntityWorld().getBlockState(position);
            lower = player.getEntityWorld().getBlockState(position.up());
            upper = player.getEntityWorld().getBlockState(position.up(2));
            if (ground.getMaterial().isLiquid()) return null;
        }
        return position.up(2);

    }

}
