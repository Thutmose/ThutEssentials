package thut.essentials.commands.rules;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSource;
import net.minecraft.server.MinecraftServer;
import thut.essentials.util.BaseCommand;
import thut.essentials.util.RuleManager;

public class DelRule extends BaseCommand
{

    public DelRule()
    {
        super("delrule", 2);
    }

    @Override
    public String getUsage(ICommandSource sender)
    {
        return "/" + getName() + " <rule index, first is 0>";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSource sender, String[] args) throws CommandException
    {
        try
        {
            RuleManager.delRule(sender, Integer.parseInt(args[0]));
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

}
