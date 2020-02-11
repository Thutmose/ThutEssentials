package thut.perms.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.server.permission.DefaultPermissionLevel;
import net.minecraftforge.server.permission.PermissionAPI;
import thut.essentials.Essentials;
import thut.essentials.commands.CommandManager;
import thut.perms.Perms;
import thut.perms.management.Group;

public class AddGroup
{

    public static void register(final CommandDispatcher<CommandSource> commandDispatcher)
    {
        // TODO configurable this.
        final String name = "add_group";
        if (Essentials.config.commandBlacklist.contains(name)) return;
        String perm;
        PermissionAPI.registerNode(perm = "command." + name, DefaultPermissionLevel.OP,
                "Can the player create a permissions group.");

        // Setup with name and permission
        LiteralArgumentBuilder<CommandSource> command = Commands.literal(name).requires(cs -> CommandManager.hasPerm(cs,
                perm));

        // Set up the command's arguments
        command = command.then(Commands.argument("group_name", StringArgumentType.string()).executes(ctx -> AddGroup
                .execute(ctx.getSource(), StringArgumentType.getString(ctx, "group_name"))));

        // Actually register the command.
        commandDispatcher.register(command);
    }

    private static int execute(final CommandSource source, final String groupName)
    {
        Group g = Perms.getGroup(groupName);
        if (g != null)
        {
            source.sendErrorMessage(new TranslationTextComponent("thutperms.group.exists"));
            return 1;
        }
        try
        {
            g = Perms.addGroup(groupName);
            g.setAll(false);
            g._init = false;
            for (final String node : Perms.manager.getRegisteredNodes())
                if (Perms.manager.getDefaultPermissionLevel(node) == DefaultPermissionLevel.ALL) g.getAllowedCommands()
                        .add(node);
            Perms.savePerms();
        }
        catch (final IllegalArgumentException e)
        {
            source.sendErrorMessage(new TranslationTextComponent(e.getMessage()));
            return 1;
        }
        source.sendFeedback(new TranslationTextComponent("thutperms.group.created", groupName), true);
        return 0;
    }
}
