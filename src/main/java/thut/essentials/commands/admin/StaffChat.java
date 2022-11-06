package thut.essentials.commands.admin;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.commands.arguments.MessageArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import thut.essentials.Essentials;
import thut.essentials.commands.CommandManager;
import thut.essentials.util.ChatHelper;
import thut.essentials.util.PermNodes;
import thut.essentials.util.PermNodes.DefaultPermissionLevel;

/**
 * StaffChat
 *
 * @author Hexeption admin@hexeption.co.uk
 * @since 10/03/2020 - 01:57 am
 */
public class StaffChat
{

    public static void register(final CommandDispatcher<CommandSourceStack> commandDispatcher)
    {
        final String name = "staff";
        String perm;
        PermNodes.registerBooleanNode(perm = "command." + name, DefaultPermissionLevel.OP, "Can the player use /" + name);
        if (Essentials.config.commandBlacklist.contains(name)) return;

        StaffChat.createCommand(commandDispatcher, name, perm);
        StaffChat.createCommand(commandDispatcher, "sc", perm);
    }

    private static void createCommand(final CommandDispatcher<CommandSourceStack> commandDispatcher, final String name,
            final String perm)
    {
        //@formatter:off
        commandDispatcher.register(Commands.literal(name).requires(cs -> CommandManager.hasPerm(cs, perm))
            .then(Commands.literal("add").then(Commands.argument("target", GameProfileArgument.gameProfile())
                .executes(context -> StaffChat.addStaff(context.getSource(), GameProfileArgument.getGameProfiles(context, "target")))))
            .then(Commands.literal("remove").then(Commands.argument("target", GameProfileArgument.gameProfile())
                .suggests((context, builder) -> SharedSuggestionProvider
                    .suggest(Essentials.config.staff.stream().map(UUID::fromString).map(id -> context.getSource().getServer().getPlayerList().getPlayer(id))
                        .map(player -> player != null ? player.getName().getString() : "Unknown").collect(Collectors.toList()), builder))
                .executes(context -> StaffChat.removeStaff(context.getSource(), GameProfileArgument.getGameProfiles(context, "target")))))
            .then(Commands.argument("message", MessageArgument.message())
                .executes(context -> StaffChat.execute(context.getSource(), MessageArgument.getMessage(context, "message")))));
        //@formatter:on
    }

    private static int removeStaff(final CommandSourceStack source, final Collection<GameProfile> target)
            throws CommandSyntaxException
    {
        final Player player = source.getPlayerOrException();
        final List<String> staffList = Lists.newArrayList(Essentials.config.staff);
        final GameProfile gameProfile = target.stream().findFirst().get();

        staffList.remove(gameProfile.getId().toString());

        try
        {
            Essentials.config.staff = staffList;
            Essentials.config.onUpdated();
            Essentials.config.write();
            ChatHelper.sendSystemMessage(player, CommandManager.makeFormattedComponent("Removed form Staff: " + gameProfile.getName()));
            return 1;
        }
        catch (final Exception e)
        {
            ChatHelper.sendSystemMessage(player, CommandManager.makeFormattedComponent("Error removing a Staff"));
            e.printStackTrace();
            return 0;
        }
    }

    private static int addStaff(final CommandSourceStack source, final Collection<GameProfile> target)
            throws CommandSyntaxException
    {
        final Player player = source.getPlayerOrException();
        final List<String> staffList = Lists.newArrayList(Essentials.config.staff);

        final GameProfile gameProfile = target.stream().findFirst().get();

        staffList.add(gameProfile.getId().toString());

        try
        {
            Essentials.config.staff = staffList;
            Essentials.config.onUpdated();
            Essentials.config.write();
            ChatHelper.sendSystemMessage(player, CommandManager.makeFormattedComponent("Added to Staff: " + gameProfile.getName()));
            return 1;
        }
        catch (final Exception e)
        {
            ChatHelper.sendSystemMessage(player, CommandManager.makeFormattedComponent("Error adding to Staff"));
            e.printStackTrace();
            return 0;
        }
    }

    private static int execute(final CommandSourceStack source, final Component message) throws CommandSyntaxException
    {
        final Player sender = source.getPlayerOrException();
        final Component textComponent = CommandManager.makeFormattedComponent("[Staff] <" + sender.getName()
                .getString() + "> " + message.getString(), ChatFormatting.YELLOW, false);
        source.getServer().sendSystemMessage(textComponent);
        Essentials.config.staff.forEach(s ->
        {
            try
            {
                final UUID id = UUID.fromString(s);
                final Player player = source.getServer().getPlayerList().getPlayer(id);
                if (player != null) ChatHelper.sendSystemMessage(player, textComponent);
            }
            catch (final Exception e)
            {
                e.printStackTrace();
            }
        });
        return 1;
    }

}
