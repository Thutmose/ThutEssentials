package thut.essentials.commands.names;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSource;
import net.minecraft.command.PlayerNotFoundException;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import thut.essentials.util.BaseCommand;

public class RealName extends BaseCommand
{
    public RealName()
    {
        super("whois", 0, "realname", "realName");
    }

    /** Return whether the specified command parameter index is a username
     * parameter. */
    @Override
    public boolean isUsernameIndex(String[] args, int index)
    {
        return index == 0;
    }

    @Override
    public void execute(MinecraftServer server, ICommandSource sender, String[] args) throws CommandException
    {
        ServerPlayerEntity player = null;
        String name = args[0];
        for (int i = 1; i < args.length; i++)
        {
            name = name + " " + args[i];
        }
        for (ServerPlayerEntity test : server.getPlayerList().getPlayers())
        {
            String temp = test.getDisplayName().getUnformattedText();
            temp = TextFormatting.getTextWithoutFormattingCodes(temp);
            if (name.equals(temp))
            {
                player = test;
                break;
            }
        }
        if (player == null) throw new PlayerNotFoundException("Cannot Find player of name: " + name);
        sender.sendMessage(new StringTextComponent(
                "The real name of " + player.getDisplayNameString() + " is " + player.getGameProfile().getName()));
    }
}
