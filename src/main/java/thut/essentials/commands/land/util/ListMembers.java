package thut.essentials.commands.land.util;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import com.google.common.collect.Lists;
import com.mojang.authlib.GameProfile;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSource;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import thut.essentials.land.LandManager;
import thut.essentials.land.LandManager.LandTeam;
import thut.essentials.util.BaseCommand;

public class ListMembers extends BaseCommand
{

    public ListMembers()
    {
        super("listmembers", 0);
    }

    @Override
    public void execute(MinecraftServer server, ICommandSource sender, String[] args) throws CommandException
    {
        LandTeam team = LandManager.getTeam(getPlayerBySender(sender));
        if (args.length == 1) team = LandManager.getInstance().getTeam(args[0], false);
        if (team == null) throw new CommandException("No team found by name " + args[0]);
        String teamName = team.teamName;
        sender.sendMessage(new StringTextComponent("Members of Team " + teamName + ":"));
        sender.sendMessage(getMembers(server, team, true));
    }

    public static ITextComponent getMembers(MinecraftServer server, LandTeam team, boolean tabbed)
    {
        StringTextComponent mess = new StringTextComponent("");
        Collection<UUID> c = team.member;
        List<UUID> ids = Lists.newArrayList(c);
        for (int i = 0; i < ids.size(); i++)
        {
            UUID o = ids.get(i);
            if (o == null) continue;
            GameProfile profile = getProfile(server, o);
            if (tabbed) mess.appendText("    ");
            if (profile.getName() != null) mess.appendText(profile.getName());
            else mess.appendText("<unknown> " + o);
            if (i < ids.size() - 1) mess.appendText("\n");
        }
        System.out.println(mess);
        return mess;
    }
}
