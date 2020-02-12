package thut.essentials.commands.economy;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.arguments.EntityArgument;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.server.permission.DefaultPermissionLevel;
import net.minecraftforge.server.permission.PermissionAPI;
import thut.essentials.Essentials;
import thut.essentials.commands.CommandManager;
import thut.essentials.economy.EconomyManager;

public class Pay
{
    public static void register(final CommandDispatcher<CommandSource> commandDispatcher)
    {
        // TODO configurable this.
        final String name = "pay";
        if (Essentials.config.commandBlacklist.contains(name)) return;
        String perm;
        PermissionAPI.registerNode(perm = "command." + name, DefaultPermissionLevel.ALL, "Can the player use /" + name);

        // Setup with name and permission
        LiteralArgumentBuilder<CommandSource> command = Commands.literal(name).requires(cs -> CommandManager.hasPerm(cs,
                perm));
        // Register the execution.
        command = command.then(Commands.argument("target_player", EntityArgument.player()).then(Commands.argument(
                "amount", IntegerArgumentType.integer()).executes(ctx -> Pay.execute(ctx.getSource(), EntityArgument
                        .getPlayer(ctx, "target_player"), IntegerArgumentType.getInteger(ctx, "amount")))));

        // Actually register the command.
        commandDispatcher.register(command);
    }

    private static int execute(final CommandSource source, final ServerPlayerEntity payee, final int toSend)
            throws CommandSyntaxException
    {
        ServerPlayerEntity player = null;
        int senderBalance = Integer.MAX_VALUE;
        try
        {
            player = source.asPlayer();
            senderBalance = EconomyManager.getBalance(player);
        }
        catch (final Exception e)
        {

        }
        if (toSend <= 0)
        {
            player.sendMessage(new TranslationTextComponent("thutessentials.econ.pay.positive"));
            return 1;
        }
        if (toSend <= senderBalance)
        {
            EconomyManager.addBalance(payee, toSend);
            player.sendMessage(new TranslationTextComponent("thutessentials.econ.pay.got", toSend, source
                    .getDisplayName()));
            if (player != null) EconomyManager.addBalance(player, -toSend);
            Essentials.config.sendFeedback(source, "thutessentials.econ.pay.send", true, toSend, payee
                    .getDisplayName());
            return 0;
        }
        player.sendMessage(new TranslationTextComponent("thutessentials.econ.pay.notenough"));
        return 1;
    }
}
