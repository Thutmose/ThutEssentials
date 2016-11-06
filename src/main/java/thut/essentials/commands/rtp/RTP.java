package thut.essentials.commands.rtp;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import thut.essentials.commands.misc.Spawn.PlayerMover;
import net.minecraft.server.MinecraftServer;
import thut.essentials.util.BaseCommand;

import net.minecraft.util.math.BlockPos;
import net.minecraft.block.state.IBlockState;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.BlockAir;

import java.util.Random;

public class RTP extends BaseCommand {

	public RTP() {
		super("rtp", 0);
	}

	@Override
	public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
		 EntityPlayer player = getCommandSenderAsPlayer(sender);

		 BlockPos position;
		 while ((position = checkSpot(player)) == null);
		 
		 PlayerMover.setMove(player, player.dimension, position, null);
	}

	// Calculate the next position to check.
	private BlockPos calculatePos()
	{
		Random rand = new Random();
		// This variable 1000 can and should be set in the config.
		int x = rand.nextInt(1000);
		int z = rand.nextInt(1000);
		int y = 64;
		return new BlockPos(x, y, z);
	}
	
	private BlockPos checkSpot(EntityPlayer player)
	{
		BlockPos position = calculatePos();
		IBlockState standing = player.getEntityWorld().getBlockState(position);
		IBlockState mid = player.getEntityWorld().getBlockState(position);
		IBlockState breath = player.getEntityWorld().getBlockState(position);
		
		// If the block we're standing on is air or liquid or if the two blocks we take up are liquid then return null, check failed.
		if (standing.getBlock() instanceof BlockLiquid || standing.getBlock() instanceof BlockAir || mid.getBlock() instanceof BlockLiquid || standing.getBlock() instanceof BlockLiquid)
		{
			return null;
		}
		while (!(mid.getBlock() instanceof BlockAir || breath.getBlock() instanceof BlockAir))
		{
			// Make a new position of y+1
			position = new BlockPos(position.getX(), position.getY()+1, position.getZ());
			standing = player.getEntityWorld().getBlockState(position);
			mid = player.getEntityWorld().getBlockState(position);
			breath = player.getEntityWorld().getBlockState(position);
			// If the block we're standing on is air or liquid or if the two blocks we take up are liquid then return null, check failed.
			if (standing.getBlock() instanceof BlockLiquid || standing.getBlock() instanceof BlockAir || mid.getBlock() instanceof BlockLiquid || standing.getBlock() instanceof BlockLiquid)
			{
				return null;
			}
			// If we reached to top.... for some reason..........
			if (position.getY() > 256) return null;
		}
		return position;
		
	}
	
}
