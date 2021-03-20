package thut.essentials.commands.homes;

import java.util.List;

import com.google.common.collect.Lists;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;

import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.Util;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.server.permission.DefaultPermissionLevel;
import net.minecraftforge.server.permission.PermissionAPI;
import thut.essentials.Essentials;
import thut.essentials.commands.CommandManager;
import thut.essentials.util.HomeManager;
import thut.essentials.util.PlayerDataHandler;

public class Delete
{
    private static SuggestionProvider<CommandSource> SUGGEST_NAMES = (ctx, sb) ->
    {
        final ServerPlayerEntity player = ctx.getSource().getPlayerOrException();
        final List<String> opts = Lists.newArrayList();
        final CompoundNBT tag = PlayerDataHandler.getCustomDataTag(player);
        final CompoundNBT homes = tag.getCompound("homes");
        opts.addAll(homes.getAllKeys());
        opts.replaceAll(s -> s.contains(" ") ? "\"" + s + "\"" : s);
        return net.minecraft.command.ISuggestionProvider.suggest(opts, sb);
    };

    public static void register(final CommandDispatcher<CommandSource> commandDispatcher)
    {
        final String name = "del_home";
        if (!Essentials.config.commandBlacklist.contains(name))
        {
            String perm;
            PermissionAPI.registerNode(perm = "command." + name, DefaultPermissionLevel.ALL, "Can the player use /"
                    + name);
            // Setup with name and permission
            LiteralArgumentBuilder<CommandSource> command = Commands.literal(name).requires(cs -> CommandManager
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

    private static int execute(final CommandSource source, String homeName) throws CommandSyntaxException
    {
        if (homeName == null) homeName = "Home";
        final ServerPlayerEntity player = source.getPlayerOrException();
        final int ret = HomeManager.removeHome(player, homeName);
        ITextComponent message;
        switch (ret)
        {
        case 0:
            message = CommandManager.makeFormattedComponent("thutessentials.homes.removed", null, false, homeName);
            player.sendMessage(message, Util.NIL_UUID);
            break;
        case 1:
            message = CommandManager.makeFormattedComponent("thutessentials.homes.noexists", null, false, homeName);
            player.sendMessage(message, Util.NIL_UUID);
            break;
        }
        return ret;
    }
}
