package thut.essentials.commands.kits;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.util.text.event.ClickEvent.Action;
import net.minecraftforge.server.permission.IPermissionHandler;
import net.minecraftforge.server.permission.PermissionAPI;
import net.minecraftforge.server.permission.context.PlayerContext;
import thut.essentials.util.BaseCommand;
import thut.essentials.util.KitManager;

public class Kits extends BaseCommand
{

    public Kits()
    {
        super("kits", 0);
        KitManager.init();
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException
    {
        EntityPlayer player = getCommandSenderAsPlayer(sender);
        IPermissionHandler manager = PermissionAPI.getPermissionHandler();
        PlayerContext context = new PlayerContext(player);
        player.sendMessage(new TextComponentString("================"));
        player.sendMessage(new TextComponentString("      Kits      "));
        player.sendMessage(new TextComponentString("================"));
        Style style = new Style();
        style.setClickEvent(new ClickEvent(Action.RUN_COMMAND, "/kit"));
        if (!KitManager.kit.isEmpty()
                && manager.hasPermission(player.getGameProfile(), "thutessentials.kit.default", context))
            player.sendMessage(new TextComponentString("Default").setStyle(style));
        for (String s : KitManager.kits.keySet())
        {
            if (!manager.hasPermission(player.getGameProfile(), "thutessentials.kit." + s, context)) continue;
            style = new Style();
            style.setClickEvent(new ClickEvent(Action.RUN_COMMAND, "/kit " + s));
            player.sendMessage(new TextComponentString(s).setStyle(style));
        }
        player.sendMessage(new TextComponentString("================"));
    }

}
