package thut.essentials.commands.misc;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSource;
import net.minecraft.command.PlayerNotFoundException;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import thut.essentials.util.BaseCommand;

public class God extends BaseCommand
{
    public God()
    {
        super("god", 4);
    }

    @Override
    public void execute(MinecraftServer server, ICommandSource sender, String[] args) throws CommandException
    {
        PlayerEntity player;
        try
        {
            player = getPlayerBySender(sender);
        }
        catch (PlayerNotFoundException e)
        {
            if (args.length != 1) throw new CommandException("Invalid Arguments, /god <target>");
            player = getPlayer(server, sender, args[0]);
        }
        player.capabilities.disableDamage = !player.capabilities.disableDamage;
        if (!player.capabilities.disableDamage)
        {
            player.capabilities.disableDamage = false;
        }
        player.sendPlayerAbilities();
        player.sendMessage(new StringTextComponent(
                TextFormatting.GREEN + "God set to: " + TextFormatting.GOLD + player.capabilities.disableDamage));
    }
}
