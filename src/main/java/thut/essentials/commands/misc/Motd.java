package thut.essentials.commands.misc;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent;
import thut.essentials.util.BaseCommand;
import thut.essentials.util.ConfigManager;
import thut.essentials.util.RuleManager;

public class Motd extends BaseCommand
{

    public Motd()
    {
        super("motd", 0);
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void PlayerLoggin(PlayerLoggedInEvent evt)
    {
        PlayerEntity PlayerEntity = evt.player;
        String motd = ConfigManager.INSTANCE.motd;
        if (motd.isEmpty())
        {
            motd = PlayerEntity.getServer().getMOTD();
        }
        else
        {
            motd = RuleManager.format(motd);
        }
        PlayerEntity.sendMessage(new StringTextComponent(motd));
    }

    @Override
    public void execute(MinecraftServer server, ICommandSource sender, String[] args) throws CommandException
    {
        String motd = ConfigManager.INSTANCE.motd;
        if (motd.isEmpty())
        {
            motd = server.getMOTD();
        }
        else
        {
            motd = RuleManager.format(motd);
        }
        sender.sendMessage(new StringTextComponent(motd));
    }

}
