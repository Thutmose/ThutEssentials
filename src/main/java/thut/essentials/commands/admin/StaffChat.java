package thut.essentials.commands.admin;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.ISuggestionProvider;
import net.minecraft.command.arguments.GameProfileArgument;
import net.minecraft.command.arguments.MessageArgument;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Util;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.server.permission.DefaultPermissionLevel;
import net.minecraftforge.server.permission.PermissionAPI;
import thut.essentials.Essentials;
import thut.essentials.commands.CommandManager;

/**
 * StaffChat
 *
 * @author Hexeption admin@hexeption.co.uk
 * @since 10/03/2020 - 01:57 am
 */
public class StaffChat
{

    public static void register(final CommandDispatcher<CommandSource> commandDispatcher)
    {
        final String name = "staff";
        String perm;
        PermissionAPI.registerNode(perm = "command." + name, DefaultPermissionLevel.OP, "Can the player use /" + name);
        if (Essentials.config.commandBlacklist.contains(name)) return;

        StaffChat.createCommand(commandDispatcher, name, perm);
        StaffChat.createCommand(commandDispatcher, "sc", perm);
    }

    private static void createCommand(final CommandDispatcher<CommandSource> commandDispatcher, final String name,
            final String perm)
    {
        //@formatter:off
        commandDispatcher.register(Commands.literal(name).requires(cs -> CommandManager.hasPerm(cs, perm))
            .then(Commands.literal("add").then(Commands.argument("target", GameProfileArgument.gameProfile())
                .executes(context -> StaffChat.addStaff(context.getSource(), GameProfileArgument.getGameProfiles(context, "target")))))
            .then(Commands.literal("remove").then(Commands.argument("target", GameProfileArgument.gameProfile())
                .suggests((context, builder) -> ISuggestionProvider
                    .suggest(Essentials.config.staff.stream().map(UUID::fromString).map(id -> context.getSource().getServer().getPlayerList().getPlayer(id))
                        .map(player -> player != null ? player.getName().getString() : "Unknown").collect(Collectors.toList()), builder))
                .executes(context -> StaffChat.removeStaff(context.getSource(), GameProfileArgument.getGameProfiles(context, "target")))))
            .then(Commands.argument("message", MessageArgument.message())
                .executes(context -> StaffChat.execute(context.getSource(), MessageArgument.getMessage(context, "message")))));
        //@formatter:on
    }

    private static int removeStaff(final CommandSource source, final Collection<GameProfile> target)
            throws CommandSyntaxException
    {
        final PlayerEntity player = source.getPlayerOrException();
        final List<String> staffList = Lists.newArrayList(Essentials.config.staff);
        final GameProfile gameProfile = target.stream().findFirst().get();

        staffList.remove(gameProfile.getId().toString());

        try
        {
            Essentials.config.staff = staffList;
            Essentials.config.onUpdated();
            Essentials.config.write();
            player.sendMessage(CommandManager.makeFormattedComponent("Removed form Staff: " + gameProfile.getName()),
                    Util.NIL_UUID);
            return 1;
        }
        catch (final Exception e)
        {
            player.sendMessage(CommandManager.makeFormattedComponent("Error removing a Staff"), Util.NIL_UUID);
            e.printStackTrace();
            return 0;
        }
    }

    private static int addStaff(final CommandSource source, final Collection<GameProfile> target)
            throws CommandSyntaxException
    {
        final PlayerEntity player = source.getPlayerOrException();
        final List<String> staffList = Lists.newArrayList(Essentials.config.staff);

        final GameProfile gameProfile = target.stream().findFirst().get();

        staffList.add(gameProfile.getId().toString());

        try
        {
            Essentials.config.staff = staffList;
            Essentials.config.onUpdated();
            Essentials.config.write();
            player.sendMessage(CommandManager.makeFormattedComponent("Added to Staff: " + gameProfile.getName()),
                    Util.NIL_UUID);
            return 1;
        }
        catch (final Exception e)
        {
            player.sendMessage(CommandManager.makeFormattedComponent("Error adding to Staff"), Util.NIL_UUID);
            e.printStackTrace();
            return 0;
        }
    }

    private static int execute(final CommandSource source, final ITextComponent message) throws CommandSyntaxException
    {
        final PlayerEntity sender = source.getPlayerOrException();
        final ITextComponent textComponent = CommandManager.makeFormattedComponent("[Staff] <" + sender.getName()
                .getString() + "> " + message.getString(), TextFormatting.YELLOW, false);
        source.getServer().sendMessage(textComponent, Util.NIL_UUID);
        Essentials.config.staff.forEach(s ->
        {
            try
            {
                final UUID id = UUID.fromString(s);
                final PlayerEntity player = source.getServer().getPlayerList().getPlayer(id);
                if (player != null) player.sendMessage(textComponent, Util.NIL_UUID);
            }
            catch (final Exception e)
            {
                e.printStackTrace();
            }
        });
        return 1;
    }

}
