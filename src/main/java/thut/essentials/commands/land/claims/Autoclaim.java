package thut.essentials.commands.land.claims;

import java.util.Map;

import com.google.common.collect.Maps;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import thut.essentials.land.LandManager;
import thut.essentials.land.LandManager.LandTeam;
import thut.essentials.util.BaseCommand;
import thut.essentials.util.Coordinate;

public class Autoclaim extends BaseCommand
{
    private Map<PlayerEntity, Boolean> claimers = Maps.newHashMap();

    public Autoclaim()
    {
        super("autoclaim", 4);
        MinecraftForge.EVENT_BUS.register(this);
    }

    @Override
    public void execute(MinecraftServer server, ICommandSource sender, String[] args) throws CommandException
    {
        boolean all = false;
        if (args.length > 0)
        {
            all = args[0].equalsIgnoreCase("all");
        }
        if (claimers.containsKey(sender))
        {
            claimers.remove(sender);
            sender.sendMessage(new StringTextComponent("Set Autoclaiming off"));
        }
        else
        {
            claimers.put((PlayerEntity) sender, all);
            sender.sendMessage(new StringTextComponent("Set Autoclaiming on"));
        }
    }

    @SubscribeEvent
    public void livingUpdate(LivingUpdateEvent evt)
    {
        if (evt.getEntity().getEntityWorld().isRemote || evt.getEntity().isDead || claimers.isEmpty()) return;

        if (evt.getMobEntity() instanceof PlayerEntity && claimers.containsKey(evt.getMobEntity()))
        {
            boolean all = claimers.get(evt.getMobEntity());
            LandTeam team = LandManager.getTeam(evt.getMobEntity());
            if (team == null)
            {
                claimers.remove(evt.getMobEntity());
                return;
            }
            int num = all ? 16 : 1;
            int n = 0;
            for (int i = 0; i < num; i++)
            {
                int x = MathHelper.floor(evt.getMobEntity().getPosition().getX() / 16f);
                int y = MathHelper.floor(evt.getMobEntity().getPosition().getY() / 16f) + i;
                if (all) y = i;
                int z = MathHelper.floor(evt.getMobEntity().getPosition().getZ() / 16f);
                int dim = evt.getMobEntity().getEntityWorld().dimension.getDimension();
                if (y < 0 || y > 15) continue;
                if (LandManager.getInstance().getLandOwner(new Coordinate(x, y, z, dim)) != null)
                {
                    continue;
                }
                n++;
                LandManager.getInstance().addTeamLand(team.teamName, new Coordinate(x, y, z, dim), true);
            }
            if (n > 0)
            {
                evt.getMobEntity()
                        .sendMessage(new StringTextComponent("Claimed This land for Team" + team.teamName));
            }
        }
    }

}
