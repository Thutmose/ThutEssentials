package thut.essentials.commands.misc;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSource;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import thut.essentials.util.BaseCommand;

public class Heal extends BaseCommand
{
    public Heal()
    {
        super("heal", 2);
    }

    @Override
    public void execute(MinecraftServer server, ICommandSource sender, String[] args) throws CommandException
    {
        LivingEntity toHeal;
        if(args.length==0)
        {
            toHeal = getPlayerBySender(sender);
        }
        else
        {
            toHeal = (LivingEntity) getEntity(server, sender, args[0]);
        }
        toHeal.setHealth(toHeal.getMaxHealth());
        if(toHeal instanceof PlayerEntity)
        {
            PlayerEntity player = (PlayerEntity) toHeal;
            player.getFoodStats().setFoodLevel(20);
        }
    }

}
