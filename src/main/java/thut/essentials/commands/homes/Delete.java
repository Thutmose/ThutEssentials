package thut.essentials.commands.homes;

import java.util.List;

import com.google.common.collect.Lists;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import thut.essentials.Essentials;
import thut.essentials.commands.CommandManager;
import thut.essentials.util.ChatHelper;
import thut.essentials.util.HomeManager;
import thut.essentials.util.PermNodes;
import thut.essentials.util.PermNodes.DefaultPermissionLevel;
import thut.essentials.util.PlayerDataHandler;

public class Delete
{
    private static SuggestionProvider<CommandSourceStack> SUGGEST_NAMES = (ctx, sb) ->
    {
        final ServerPlayer player = ctx.getSource().getPlayerOrException();
        final List<String> opts = Lists.newArrayList();
        final CompoundTag tag = PlayerDataHandler.getCustomDataTag(player);
        final CompoundTag homes = tag.getCompound("homes");
        opts.addAll(homes.getAllKeys());
        opts.replaceAll(s -> s.contains(" ") ? "\"" + s + "\"" : s);
        return net.minecraft.commands.SharedSuggestionProvider.suggest(opts, sb);
    };

    public static void register(final CommandDispatcher<CommandSourceStack> commandDispatcher)
    {
        final String name = "del_home";
        if (!Essentials.config.commandBlacklist.contains(name))
        {
            String perm;
            PermNodes.registerBooleanNode(perm = "command." + name, DefaultPermissionLevel.ALL, "Can the player use /"
                    + name);
            // Setup with name and permission
            LiteralArgumentBuilder<CommandSourceStack> command = Commands.literal(name).requires(cs -> CommandManager
                    .hasPerm(cs, perm));

            // Home name argument version.
            command = command.then(Commands.argument("home_name", StringArgumentType.string()).suggests(
                    Delete.SUGGEST_NAMES).executes(ctx -> Delete.execute(ctx.getSource(), StringArgumentType.getString(
                            ctx, "home_name"))));
            commandDispatcher.register(command);

            // No argument version.
            command = Commands.literal(name).requires(cs -> CommandManager.hasPerm(cs, perm));
            command = command.executes(ctx -> Delete.execute(ctx.getSource(), null));
            commandDispatcher.register(command);
        }
    }

    private static int execute(final CommandSourceStack source, String homeName) throws CommandSyntaxException
    {
        if (homeName == null) homeName = "Home";
        final ServerPlayer player = source.getPlayerOrException();
        final int ret = HomeManager.removeHome(player, homeName);
        Component message;
        switch (ret)
        {
        case 0:
            message = CommandManager.makeFormattedComponent("thutessentials.homes.removed", null, false, homeName);
            ChatHelper.sendSystemMessage(player, message);
            break;
        case 1:
            message = CommandManager.makeFormattedComponent("thutessentials.homes.noexists", null, false, homeName);
            ChatHelper.sendSystemMessage(player, message);
            break;
        }
        return ret;
    }
}
