package thut.essentials.commands.rules;

import java.util.List;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSource;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.StringTextComponent;
import thut.essentials.util.BaseCommand;
import thut.essentials.util.ConfigManager;
import thut.essentials.util.RuleManager;

public class Rules extends BaseCommand
{
    public Rules()
    {
        super("rules", 0);
    }

    @Override
    public void execute(MinecraftServer server, ICommandSource sender, String[] args) throws CommandException
    {
        List<String> rules = RuleManager.getRules();
        sender.sendMessage(new StringTextComponent(RuleManager.format(ConfigManager.INSTANCE.ruleHeader)));
        for (String s : rules)
        {
            sender.sendMessage(new StringTextComponent(RuleManager.format(s)));
        }
    }

}
