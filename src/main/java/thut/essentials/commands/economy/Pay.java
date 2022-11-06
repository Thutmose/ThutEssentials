package thut.essentials.commands.economy;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;
import thut.essentials.Essentials;
import thut.essentials.commands.CommandManager;
import thut.essentials.economy.EconomyManager;
import thut.essentials.util.ChatHelper;
import thut.essentials.util.PermNodes;
import thut.essentials.util.PermNodes.DefaultPermissionLevel;

public class Pay
{
    public static void register(final CommandDispatcher<CommandSourceStack> commandDispatcher)
    {
        // TODO configurable this.
        final String name = "pay";
        if (Essentials.config.commandBlacklist.contains(name)) return;
        String perm;
        PermNodes.registerBooleanNode(perm = "command." + name, DefaultPermissionLevel.ALL, "Can the player use /" + name);

        // Setup with name and permission
        LiteralArgumentBuilder<CommandSourceStack> command = Commands.literal(name)
                .requires(cs -> CommandManager.hasPerm(cs, perm));
        // Register the execution.
        command = command.then(Commands.argument("target_player", EntityArgument.player())
                .then(Commands.argument("amount", IntegerArgumentType.integer())
                        .executes(ctx -> Pay.execute(ctx.getSource(), EntityArgument.getPlayer(ctx, "target_player"),
                                IntegerArgumentType.getInteger(ctx, "amount")))));

        // Actually register the command.
        commandDispatcher.register(command);
    }

    private static int execute(final CommandSourceStack source, final ServerPlayer payee, final int toSend)
            throws CommandSyntaxException
    {
        ServerPlayer player = null;
        int senderBalance = Integer.MAX_VALUE;
        try
        {
            player = source.getPlayerOrException();
            senderBalance = EconomyManager.getBalance(player);
        }
        catch (final Exception e)
        {

        }
        if (toSend <= 0)
        {
            ChatHelper.sendSystemMessage(player, Essentials.config.getMessage("thutessentials.econ.pay.positive"));
            return 1;
        }
        if (toSend <= senderBalance)
        {
            EconomyManager.addBalance(payee, toSend);
            ChatHelper.sendSystemMessage(payee,
                    Essentials.config.getMessage("thutessentials.econ.pay.got", toSend, source.getDisplayName()));
            if (player != null) EconomyManager.addBalance(player, -toSend);
            Essentials.config.sendFeedback(source, "thutessentials.econ.pay.send", false, toSend,
                    payee.getDisplayName());
            return 0;
        }
        if (player != null)
            ChatHelper.sendSystemMessage(player, Essentials.config.getMessage("thutessentials.econ.pay.notenough"));
        return 1;
    }
}
