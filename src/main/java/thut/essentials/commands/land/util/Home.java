package thut.essentials.commands.land.util;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSource;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import thut.essentials.ThutEssentials;
import thut.essentials.commands.CommandManager;
import thut.essentials.commands.misc.Spawn;
import thut.essentials.commands.misc.Spawn.PlayerMover;
import thut.essentials.land.LandManager;
import thut.essentials.land.LandManager.LandTeam;
import thut.essentials.util.BaseCommand;
import thut.essentials.util.ConfigManager;
import thut.essentials.util.PlayerDataHandler;

public class Home extends BaseCommand
{

    public Home()
    {
        super("thome", 0);
    }

    @Override
    public void execute(MinecraftServer server, ICommandSource sender, String[] args) throws CommandException
    {
        ServerPlayerEntity player = getPlayerBySender(sender);
        LandTeam team = LandManager.getTeam(player);

        if (team.home == null)
            throw new CommandException("No Home is set for your team, use /editteam home to set one.");

        int[] home = { team.home.x, team.home.y, team.home.z, team.home.dim };

        CompoundNBT tag = PlayerDataHandler.getCustomDataTag(player);
        CompoundNBT tptag = tag.getCompound("tp");
        long last = tptag.getLong("homeDelay");
        long time = player.getServer().getWorld(0).getGameTime();
        if (last > time)
        {
            player.sendMessage(
                    CommandManager.makeFormattedComponent("Too Soon between Warp attempt", TextFormatting.RED, false));
            return;
        }

        ITextComponent teleMess = CommandManager.makeFormattedComponent("Warping to your Team's Home",
                TextFormatting.GREEN);
        tptag.putLong("homeDelay", time + ConfigManager.INSTANCE.homeReUseDelay);
        tag.setTag("tp", tptag);
        PlayerDataHandler.saveCustomData(player);
        PlayerMover.setMove(player, ThutEssentials.instance.config.homeActivateDelay, home[3],
                new BlockPos(home[0], home[1], home[2]), teleMess, Spawn.INTERUPTED);
    }

}
