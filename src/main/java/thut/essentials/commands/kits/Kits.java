package thut.essentials.commands.kits;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.Style;
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
    public void execute(MinecraftServer server, ICommandSource sender, String[] args) throws CommandException
    {
        PlayerEntity player = getCommandSenderAsPlayer(sender);
        IPermissionHandler manager = PermissionAPI.getPermissionHandler();
        PlayerContext context = new PlayerContext(player);
        player.sendMessage(new StringTextComponent("================"));
        player.sendMessage(new StringTextComponent("      Kits      "));
        player.sendMessage(new StringTextComponent("================"));
        Style style = new Style();
        style.setClickEvent(new ClickEvent(Action.RUN_COMMAND, "/kit"));
        if (!KitManager.kit.isEmpty()
                && manager.hasPermission(player.getGameProfile(), "thutessentials.kit.default", context))
            player.sendMessage(new StringTextComponent("Default").setStyle(style));
        for (String s : KitManager.kits.keySet())
        {
            if (!manager.hasPermission(player.getGameProfile(), "thutessentials.kit." + s, context)) continue;
            style = new Style();
            style.setClickEvent(new ClickEvent(Action.RUN_COMMAND, "/kit " + s));
            player.sendMessage(new StringTextComponent(s).setStyle(style));
        }
        player.sendMessage(new StringTextComponent("================"));
    }

}
