package thut.essentials.commands.misc;

import java.util.Collection;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.arguments.GameProfileArgument;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.server.permission.DefaultPermissionLevel;
import net.minecraftforge.server.permission.PermissionAPI;
import thut.essentials.Essentials;
import thut.essentials.commands.CommandManager;
import thut.essentials.util.NameManager;
import thut.essentials.util.PlayerDataHandler;

public class Nick
{
    public static void register(final CommandDispatcher<CommandSource> commandDispatcher)
    {
        final String name = "nick";
        String perm;
        PermissionAPI.registerNode(perm = "command." + name, DefaultPermissionLevel.OP, "Can the player use /" + name);
        if (Essentials.config.commandBlacklist.contains(name)) return;

        LiteralArgumentBuilder<CommandSource> command = Commands.literal(name).requires(cs -> CommandManager.hasPerm(cs,
                perm));

        command = command.then(Commands.argument("player", GameProfileArgument.gameProfile()).then(Commands.argument(
                "nick", StringArgumentType.greedyString()).executes(ctx -> Nick.execute(ctx.getSource(),
                        GameProfileArgument.getGameProfiles(ctx, "player"), StringArgumentType.getString(ctx,
                                "nick")))));
        // Actually register the command.
        commandDispatcher.register(command);
        NameManager.init();
    }

    private static int execute(final CommandSource source, final Collection<GameProfile> target, String nick)
    {
        if (nick.length() > 16) nick = nick.substring(0, 16);
        final MinecraftServer server = source.getServer();
        for (final GameProfile p : target)
        {
            final ServerPlayerEntity player = server.getPlayerList().getPlayerByUUID(p.getId());
            if (player == null) continue;
            final CompoundNBT tag = PlayerDataHandler.getCustomDataTag(player);
            if ("_".equals(nick) && player != null) if (tag.contains("nick_orig")) nick = tag.getString("nick_orig");
            if (!tag.contains("nick_orig")) tag.putString("nick_orig", p.getName());
            tag.putString("nick", nick);
            PlayerDataHandler.saveCustomData(player);
            player.refreshDisplayName();
        }
        return 0;
    }

}
