package thut.essentials.commands.warps;

import java.util.List;

import com.google.common.collect.Lists;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;

import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.server.permission.DefaultPermissionLevel;
import net.minecraftforge.server.permission.PermissionAPI;
import thut.essentials.Essentials;
import thut.essentials.commands.CommandManager;
import thut.essentials.util.WarpManager;

public class Delete
{
    private static SuggestionProvider<CommandSource> SUGGEST_NAMES = (ctx, sb) ->
    {
        final List<String> opts = Lists.newArrayList();
        opts.addAll(WarpManager.warpLocs.keySet());
        opts.replaceAll(s -> s.contains(" ") ? "\"" + s + "\"" : s);
        return net.minecraft.command.ISuggestionProvider.suggest(opts, sb);
    };

    public static void register(final CommandDispatcher<CommandSource> commandDispatcher)
    {
        final String name = "del_warp";
        if (!Essentials.config.commandBlacklist.contains(name))
        {
            String perm;
            PermissionAPI.registerNode(perm = "command." + name, DefaultPermissionLevel.OP, "Can the player use /"
                    + name);
            // Setup with name and permission
            LiteralArgumentBuilder<CommandSource> command = Commands.literal(name).requires(cs -> CommandManager
                    .hasPerm(cs, perm));

            // Home name argument version.
            command = command.then(Commands.argument("warp", StringArgumentType.string()).suggests(Delete.SUGGEST_NAMES)
                    .executes(ctx -> Delete.execute(ctx.getSource(), StringArgumentType.getString(ctx, "warp"))));
            commandDispatcher.register(command);
        }
    }

    private static int execute(final CommandSource source, final String homeName) throws CommandSyntaxException
    {
        final int ret = WarpManager.delWarp(homeName);
        ITextComponent message;
        switch (ret)
        {
        case 0:
            message = CommandManager.makeFormattedComponent("thutessentials.warps.removed", null, false, homeName);
            source.sendFeedback(message, true);
            break;
        case 1:
            message = CommandManager.makeFormattedComponent("thutessentials.warps.noexists_use", null, false, homeName);
            source.sendFeedback(message, true);
            break;
        }
        return ret;
    }
}
