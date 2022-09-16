package thut.essentials.commands.tpa;

import java.util.UUID;

import com.google.common.collect.Lists;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;
import thut.essentials.Essentials;
import thut.essentials.commands.CommandManager;
import thut.essentials.util.ChatHelper;
import thut.essentials.util.CoordinateUtls;
import thut.essentials.util.PermNodes;
import thut.essentials.util.PermNodes.DefaultPermissionLevel;
import thut.essentials.util.PlayerDataHandler;
import thut.essentials.util.PlayerMover;

public class TpAccept
{
    private static SuggestionProvider<CommandSourceStack> SUGGEST = (ctx,
            sb) -> net.minecraft.commands.SharedSuggestionProvider.suggest(Lists.newArrayList("accept", "deny"), sb);

    public static void register(final CommandDispatcher<CommandSourceStack> commandDispatcher)
    {
        // TODO configurable this.
        final String name = "tpaccept";
        if (Essentials.config.commandBlacklist.contains(name)) return;
        String perm;
        PermNodes.registerNode(perm = "command." + name, DefaultPermissionLevel.ALL, "Can the player use /" + name);

        // Setup with name and permission
        LiteralArgumentBuilder<CommandSourceStack> command = Commands.literal(name)
                .requires(cs -> CommandManager.hasPerm(cs, perm));

        command = command.then(Commands.argument("uuid", StringArgumentType.string())
                .then(Commands.argument("accept/deny", StringArgumentType.string()).suggests(TpAccept.SUGGEST)
                        .executes(ctx -> TpAccept.execute(ctx.getSource(), StringArgumentType.getString(ctx, "uuid"),
                                StringArgumentType.getString(ctx, "accept/deny")))));

        // Actually register the command.
        commandDispatcher.register(command);
    }

    private static int execute(final CommandSourceStack source, final String uuid, final String option)
            throws CommandSyntaxException
    {

        final Player player = source.getPlayerOrException();
        final CompoundTag tag = PlayerDataHandler.getCustomDataTag(player);
        final CompoundTag tpaTag = tag.getCompound("tpa");
        final String requestor = tpaTag.getString("R");
        if (!requestor.equals(uuid)) return 1;

        tpaTag.remove("R");
        tag.put("tpa", tpaTag);
        final MinecraftServer server = player.getServer();
        PlayerDataHandler.saveCustomData(player);
        final Player target = server.getPlayerList().getPlayer(UUID.fromString(uuid));
        if (option.equals("accept"))
        {
            ChatHelper.sendSystemMessage(target, CommandManager
                    .makeFormattedComponent("thutessentials.tpa.accepted_user", ChatFormatting.GREEN, true));
            ChatHelper.sendSystemMessage(player, CommandManager
                    .makeFormattedComponent("thutessentials.tpa.accepted_target", ChatFormatting.GREEN, true));
            PlayerMover.setMove(target, Essentials.config.tpaActivateDelay, CoordinateUtls.forMob(player), null,
                    PlayerMover.INTERUPTED);
        }
        else if (option.equals("deny"))
        {
            ChatHelper.sendSystemMessage(target,
                    CommandManager.makeFormattedComponent("thutessentials.tpa.denied_user", ChatFormatting.RED, true));
            ChatHelper.sendSystemMessage(player, CommandManager
                    .makeFormattedComponent("thutessentials.tpa.denied_target", ChatFormatting.RED, true));
        }
        return 0;
    }
}
