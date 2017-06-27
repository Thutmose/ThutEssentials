package thut.essentials.commands.misc;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
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
        EntityPlayer entityPlayer = evt.player;
        String motd = ConfigManager.INSTANCE.motd;
        if (motd.isEmpty())
        {
            motd = entityPlayer.getServer().getMOTD();
        }
        else
        {
            motd = RuleManager.format(motd);
        }
        entityPlayer.sendMessage(new TextComponentString(motd));
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException
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
        sender.sendMessage(new TextComponentString(motd));
    }

}
