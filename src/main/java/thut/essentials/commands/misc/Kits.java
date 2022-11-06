package thut.essentials.commands.misc;

import java.util.List;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import thut.essentials.Essentials;
import thut.essentials.commands.CommandManager;
import thut.essentials.economy.EconomyManager;
import thut.essentials.util.KitManager;
import thut.essentials.util.KitManager.KitSet;
import thut.essentials.util.PermNodes;
import thut.essentials.util.PermNodes.DefaultPermissionLevel;
import thut.essentials.util.PlayerDataHandler;

public class Kits
{
    public static void register(final CommandDispatcher<CommandSourceStack> commandDispatcher)
    {
        String name = "kits";
        if (!Essentials.config.commandBlacklist.contains(name))
        {
            String perm;
            PermNodes.registerBooleanNode(perm = "command." + name, DefaultPermissionLevel.ALL,
                    "Can the player use /" + name);
            // Setup with name and permission
            LiteralArgumentBuilder<CommandSourceStack> command = Commands.literal(name)
                    .requires(cs -> CommandManager.hasPerm(cs, perm));
            // Register the execution.
            command = command.executes(ctx -> Kits.kits(ctx.getSource()));

            // Actually register the command.
            commandDispatcher.register(command);
        }
        name = "kit";
        if (!Essentials.config.commandBlacklist.contains(name))
        {
            String perm;
            PermNodes.registerBooleanNode(perm = "command." + name, DefaultPermissionLevel.ALL,
                    "Can the player use /" + name);
            // Setup with name and permission
            LiteralArgumentBuilder<CommandSourceStack> command = Commands.literal(name)
                    .requires(cs -> CommandManager.hasPerm(cs, perm));

            command = command.then(Commands.argument("kit_name", StringArgumentType.string())
                    .executes(ctx -> Kits.execute(ctx.getSource(), StringArgumentType.getString(ctx, "kit_name"))));
            commandDispatcher.register(command);

            // Default kit.
            command = command.executes(ctx -> Kits.execute(ctx.getSource(), null));
            commandDispatcher.register(command);
        }
        name = "reload_kits";
        if (!Essentials.config.commandBlacklist.contains(name))
        {
            String perm;
            PermNodes.registerBooleanNode(perm = "command." + name, DefaultPermissionLevel.OP,
                    "Can the player use /" + name);
            // Setup with name and permission
            LiteralArgumentBuilder<CommandSourceStack> command = Commands.literal(name)
                    .requires(cs -> CommandManager.hasPerm(cs, perm));
            command = command.executes(ctx -> Kits.reload(ctx.getSource()));
            commandDispatcher.register(command);
        }
    }

    private static int reload(final CommandSourceStack source) throws CommandSyntaxException
    {
        KitManager.init();
        Essentials.config.sendFeedback(source, "thutessentials.kits.reloaded", true);
        return 0;
    }

    private static int kits(final CommandSourceStack source) throws CommandSyntaxException
    {
        final ServerPlayer player = source.getPlayerOrException();
        KitManager.sendKitsList(player);
        return 0;
    }

    private static int execute(final CommandSourceStack source, final String warpName) throws CommandSyntaxException
    {
        final ServerPlayer player = source.getPlayerOrException();
        final MinecraftServer server = player.getServer();

        List<ItemStack> stacks;
        int delay = Essentials.config.kitReuseDelay;
        String kitTag = "kitTime";
        // Specific kit.
        if (warpName != null)
        {
            final KitSet kit = KitManager.kits.get(warpName);
            if (kit == null)
            {
                Essentials.config.sendError(source, "thutessentials.kits.no_kit");
                return 1;
            }
            if (kit.OP && !PermNodes.hasStringInList(player, KitManager.KIT, warpName))
            {
                Essentials.config.sendError(source, "thutessentials.kits.noperms");
                return 1;
            }
            if (PermNodes.hasStringInList(player, KitManager.NO_KIT, warpName))
            {
                Essentials.config.sendError(source, "thutessentials.kits.noperms");
                return 1;
            }

            kitTag = "kitTime_" + warpName;
            delay = kit.cooldown;
            stacks = kit.stacks;
        }
        else stacks = KitManager.kit;

        if (!PermNodes.getBooleanPerm(player, KitManager.DEFAULT_KIT))
        {
            Essentials.config.sendError(source, "thutessentials.kits.noperms");
            return 1;
        }

        final long kitTime = PlayerDataHandler.getCustomDataTag(player).getLong(kitTag);
        if (delay <= 0 && kitTime != 0 || server.getLevel(Level.OVERWORLD).getGameTime() < kitTime)
        {
            Essentials.config.sendError(source, "thutessentials.kits.too_soon");
            return 1;
        }
        for (final ItemStack stack : stacks)
        {
            EconomyManager.giveItem(player, stack.copy());
            PlayerDataHandler.getCustomDataTag(player).putLong(kitTag,
                    server.getLevel(Level.OVERWORLD).getGameTime() + delay);
            PlayerDataHandler.saveCustomData(player);
        }
        return 0;
    }
}
