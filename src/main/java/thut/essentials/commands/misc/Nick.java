package thut.essentials.commands.misc;

import java.util.Collection;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.server.permission.DefaultPermissionLevel;
import net.minecraftforge.server.permission.PermissionAPI;
import thut.essentials.Essentials;
import thut.essentials.commands.CommandManager;
import thut.essentials.util.NameManager;
import thut.essentials.util.PlayerDataHandler;
import thut.essentials.util.RuleManager;

public class Nick
{
    public static void register(final CommandDispatcher<CommandSourceStack> commandDispatcher)
    {
        final String name = "nick";
        String perm;
        PermissionAPI.registerNode(perm = "command." + name, DefaultPermissionLevel.OP, "Can the player use /" + name);
        if (Essentials.config.commandBlacklist.contains(name)) return;

        LiteralArgumentBuilder<CommandSourceStack> command = Commands.literal(name).requires(cs -> CommandManager.hasPerm(cs,
                perm));

        command = command.then(Commands.argument("player", GameProfileArgument.gameProfile()).then(Commands.argument(
                "nick", StringArgumentType.greedyString()).executes(ctx -> Nick.set_nick(ctx.getSource(),
                        GameProfileArgument.getGameProfiles(ctx, "player"), StringArgumentType.getString(ctx,
                                "nick")))));

        command = command.then(Commands.argument("player", GameProfileArgument.gameProfile()).then(Commands.argument(
                "prefix", StringArgumentType.greedyString()).executes(ctx -> Nick.add_name_prefix(ctx.getSource(),
                        GameProfileArgument.getGameProfiles(ctx, "player"), StringArgumentType.getString(ctx,
                                "prefix")))));

        command = command.then(Commands.argument("player", GameProfileArgument.gameProfile()).then(Commands.argument(
                "suffix", StringArgumentType.greedyString()).executes(ctx -> Nick.add_name_suffix(ctx.getSource(),
                        GameProfileArgument.getGameProfiles(ctx, "player"), StringArgumentType.getString(ctx,
                                "suffix")))));
        // Actually register the command.
        commandDispatcher.register(command);
        NameManager.init();
    }

    private static int add_name_prefix(final CommandSourceStack source, final Collection<GameProfile> target, String prefix)
    {
        if (prefix.length() > 8) prefix = prefix.substring(0, 8);
        prefix = RuleManager.format(prefix);
        final MinecraftServer server = source.getServer();
        for (final GameProfile p : target)
        {
            final ServerPlayer player = server.getPlayerList().getPlayer(p.getId());
            if (player == null) continue;

            final CompoundTag tag = PlayerDataHandler.getCustomDataTag(player);
            tag.putString("nick_pref", prefix);
            PlayerDataHandler.saveCustomData(player);
            player.refreshDisplayName();
        }
        return 0;
    }

    private static int add_name_suffix(final CommandSourceStack source, final Collection<GameProfile> target, String prefix)
    {
        if (prefix.length() > 8) prefix = prefix.substring(0, 8);
        prefix = RuleManager.format(prefix);
        final MinecraftServer server = source.getServer();
        for (final GameProfile p : target)
        {
            final ServerPlayer player = server.getPlayerList().getPlayer(p.getId());
            if (player == null) continue;
            final CompoundTag tag = PlayerDataHandler.getCustomDataTag(player);
            tag.putString("nick_suff", prefix);
            PlayerDataHandler.saveCustomData(player);
            player.refreshDisplayName();
        }
        return 0;
    }

    private static int set_nick(final CommandSourceStack source, final Collection<GameProfile> target, String nick)
    {
        if (nick.length() > 16) nick = nick.substring(0, 16);
        nick = RuleManager.format(nick);
        final MinecraftServer server = source.getServer();
        for (final GameProfile p : target)
        {
            final ServerPlayer player = server.getPlayerList().getPlayer(p.getId());
            if (player == null) continue;
            final CompoundTag tag = PlayerDataHandler.getCustomDataTag(player);
            if ("_".equals(nick) && player != null) if (tag.contains("nick_orig")) nick = tag.getString("nick_orig");
            if (!tag.contains("nick_orig")) tag.putString("nick_orig", p.getName());
            tag.putString("nick", nick);
            PlayerDataHandler.saveCustomData(player);
            player.refreshDisplayName();
        }
        return 0;
    }

}
