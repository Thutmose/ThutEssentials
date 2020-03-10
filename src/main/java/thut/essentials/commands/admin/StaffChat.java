package thut.essentials.commands.admin;

import com.google.common.collect.Lists;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.sun.javafx.runtime.eula.Eula;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.ISuggestionProvider;
import net.minecraft.command.arguments.GameProfileArgument;
import net.minecraft.command.arguments.MessageArgument;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.server.permission.DefaultPermissionLevel;
import net.minecraftforge.server.permission.PermissionAPI;
import thut.essentials.Config;
import thut.essentials.Essentials;
import thut.essentials.commands.CommandManager;

/**
 * StaffChat
 *
 * @author Hexeption admin@hexeption.co.uk
 * @since 10/03/2020 - 01:57 am
 */
public class StaffChat {

    public static void register(final CommandDispatcher<CommandSource> commandDispatcher) {
        String name = "staff";
        String perm;
        PermissionAPI.registerNode(perm = "command." + name, DefaultPermissionLevel.OP, "Can the player use /"
            + name);
        if (Essentials.config.commandBlacklist.contains(name)) {
            return;
        }

        createCommand(commandDispatcher, name, perm);
        createCommand(commandDispatcher, "sc", perm);
    }

    private static void createCommand(CommandDispatcher<CommandSource> commandDispatcher, String name, String perm) {
        commandDispatcher.register(Commands.literal(name).requires(cs -> CommandManager.hasPerm(cs, perm))
            .then(Commands.literal("add").then(Commands.argument("target", GameProfileArgument.gameProfile())
                .executes(context -> addStaff(context.getSource(), GameProfileArgument.getGameProfiles(context, "target")))))
            .then(Commands.literal("remove").then(Commands.argument("target", GameProfileArgument.gameProfile())
                .suggests((context, builder) -> ISuggestionProvider
                    .suggest(Essentials.config.staff.stream().map(UUID::fromString).map(id -> context.getSource().getServer().getPlayerList().getPlayerByUUID(id))
                        .map(player -> player != null ? player.getName().getFormattedText() : "Unknown").collect(Collectors.toList()), builder))
                .executes(context -> removeStaff(context.getSource(), GameProfileArgument.getGameProfiles(context, "target")))))
            .then(Commands.argument("message", MessageArgument.message())
                .executes(context -> execute(context.getSource(), MessageArgument.getMessage(context, "message")))));
    }

    private static int removeStaff(CommandSource source, Collection<GameProfile> target) throws CommandSyntaxException {
        final PlayerEntity player = source.asPlayer();

        List<String> staffList = Lists.newArrayList(Essentials.config.staff);

        GameProfile gameProfile = target.stream().findFirst().get();

        staffList.remove(gameProfile.getId().toString());

        try {
            Essentials.config.staff = staffList;
            Essentials.config.onUpdated();
            Essentials.config.write();
            player.sendMessage(CommandManager.makeFormattedComponent("Removed form Staff: " + gameProfile.getName()));
            return 1;
        } catch (Exception e) {
            player.sendMessage(CommandManager.makeFormattedComponent("Error removing a Staff"));
            e.printStackTrace();
            return 0;
        }
    }

    private static int addStaff(CommandSource source, Collection<GameProfile> target) throws CommandSyntaxException {
        final PlayerEntity player = source.asPlayer();
        List<String> staffList = Lists.newArrayList(Essentials.config.staff);

        GameProfile gameProfile = target.stream().findFirst().get();

        staffList.add(gameProfile.getId().toString());

        try {
            Essentials.config.staff = staffList;
            Essentials.config.onUpdated();
            Essentials.config.write();
            player.sendMessage(CommandManager.makeFormattedComponent("Added to Staff: " + gameProfile.getName()));
            return 1;
        } catch (Exception e) {
            player.sendMessage(CommandManager.makeFormattedComponent("Error adding to Staff"));
            e.printStackTrace();
            return 0;
        }
    }

    private static int execute(final CommandSource source, ITextComponent message) throws CommandSyntaxException {
        final PlayerEntity sender = source.asPlayer();
        ITextComponent textComponent = CommandManager.makeFormattedComponent("[Staff] <" + sender.getName().getFormattedText() + "> " + message.getFormattedText(), TextFormatting.YELLOW, false);
        source.getServer().sendMessage(textComponent);
        Essentials.config.staff.forEach(s -> {
            try {
                UUID id = UUID.fromString(s);
                PlayerEntity player = source.getServer().getPlayerList().getPlayerByUUID(id);
                if (player != null) {
                    player.sendMessage(textComponent);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        return 1;
    }

}
