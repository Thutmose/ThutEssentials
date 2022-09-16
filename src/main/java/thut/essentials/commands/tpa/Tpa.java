package thut.essentials.commands.tpa;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import thut.essentials.Essentials;
import thut.essentials.commands.CommandManager;
import thut.essentials.util.ChatHelper;
import thut.essentials.util.PermNodes;
import thut.essentials.util.PermNodes.DefaultPermissionLevel;
import thut.essentials.util.PlayerDataHandler;

public class Tpa
{
    public static void register(final CommandDispatcher<CommandSourceStack> commandDispatcher)
    {
        final String name = "tpa";
        if (Essentials.config.commandBlacklist.contains(name)) return;
        String perm;
        PermNodes.registerNode(perm = "command." + name, DefaultPermissionLevel.ALL, "Can the player use /" + name);

        // Setup with name and permission
        LiteralArgumentBuilder<CommandSourceStack> command = Commands.literal(name)
                .requires(cs -> CommandManager.hasPerm(cs, perm));
        // Register the execution.
        command = command.then(Commands.argument("target_player", EntityArgument.player())
                .executes(ctx -> Tpa.execute(ctx.getSource(), EntityArgument.getPlayer(ctx, "target_player"))));

        // Actually register the command.
        commandDispatcher.register(command);
    }

    private static int execute(final CommandSourceStack source, final ServerPlayer target) throws CommandSyntaxException
    {
        final Player player = source.getPlayerOrException();
        if (!Essentials.config.tpaCrossDim && target.getCommandSenderWorld() != player.getCommandSenderWorld())
        {
            ChatHelper.sendSystemMessage(player, Essentials.config.getMessage("thutessentials.tp.wrongdim"));
            return 1;
        }
        CompoundTag tag = PlayerDataHandler.getCustomDataTag(player);
        CompoundTag tpaTag = tag.getCompound("tpa");

        final long last = tag.getLong("tpaDelay");
        final long time = player.getServer().getLevel(Level.OVERWORLD).getGameTime();
        if (last > time && Essentials.config.tpaReUseDelay > 0)
        {
            ChatHelper.sendSystemMessage(player, Essentials.config.getMessage("thutessentials.tp.tosoon"));
            return 1;
        }
        tpaTag.putLong("tpaDelay", time + Essentials.config.tpaReUseDelay);
        tag.put("tpa", tpaTag);
        PlayerDataHandler.saveCustomData(player);

        tag = PlayerDataHandler.getCustomDataTag(target);
        tpaTag = tag.getCompound("tpa");
        if (tpaTag.getBoolean("ignore")) return 1;

        final MutableComponent header = ((MutableComponent) player.getDisplayName())
                .append(CommandManager.makeFormattedComponent("thutessentials.tpa.requested"));
        ChatHelper.sendSystemMessage(target, header);

        MutableComponent tpMessage;
        final String tpaccept = "tpaccept";
        final MutableComponent accept = CommandManager.makeFormattedCommandLink("thutessentials.tpa.accept",
                "/" + tpaccept + " " + player.getStringUUID() + " accept");
        final MutableComponent deny = CommandManager.makeFormattedCommandLink("thutessentials.tpa.deny",
                "/" + tpaccept + " " + player.getStringUUID() + " deny");
        tpMessage = accept.append(new TextComponent("      /      ")).append(deny);
        ChatHelper.sendSystemMessage(target, tpMessage);
        tpaTag.putString("R", player.getStringUUID());
        tag.put("tpa", tpaTag);
        PlayerDataHandler.saveCustomData(target);
        ChatHelper.sendSystemMessage(player, CommandManager.makeFormattedComponent("thutessentials.tpa.requestsent",
                ChatFormatting.DARK_GREEN, true, target.getDisplayName()));
        return 0;
    }
}
