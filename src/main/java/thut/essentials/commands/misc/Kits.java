package thut.essentials.commands.misc;

import java.util.List;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.dimension.DimensionType;
import net.minecraftforge.server.permission.DefaultPermissionLevel;
import net.minecraftforge.server.permission.IPermissionHandler;
import net.minecraftforge.server.permission.PermissionAPI;
import net.minecraftforge.server.permission.context.PlayerContext;
import thut.essentials.Essentials;
import thut.essentials.commands.CommandManager;
import thut.essentials.economy.EconomyManager;
import thut.essentials.util.KitManager;
import thut.essentials.util.KitManager.KitSet;
import thut.essentials.util.PlayerDataHandler;

public class Kits
{
    public static void register(final CommandDispatcher<CommandSource> commandDispatcher)
    {
        String name = "kits";
        if (!Essentials.config.commandBlacklist.contains(name))
        {
            String perm;
            PermissionAPI.registerNode(perm = "command." + name, DefaultPermissionLevel.ALL, "Can the player use /"
                    + name);
            // Setup with name and permission
            LiteralArgumentBuilder<CommandSource> command = Commands.literal(name).requires(cs -> CommandManager
                    .hasPerm(cs, perm));
            // Register the execution.
            command = command.executes(ctx -> Kits.kits(ctx.getSource()));

            // Actually register the command.
            commandDispatcher.register(command);
        }
        name = "kit";
        if (!Essentials.config.commandBlacklist.contains(name))
        {
            String perm;
            PermissionAPI.registerNode(perm = "command." + name, DefaultPermissionLevel.ALL, "Can the player use /"
                    + name);
            // Setup with name and permission
            LiteralArgumentBuilder<CommandSource> command = Commands.literal(name).requires(cs -> CommandManager
                    .hasPerm(cs, perm));

            command = command.then(Commands.argument("kit_name", StringArgumentType.string()).executes(ctx -> Kits
                    .execute(ctx.getSource(), StringArgumentType.getString(ctx, "kit_name"))));
            commandDispatcher.register(command);

            // Default kit.
            command = command.executes(ctx -> Kits.execute(ctx.getSource(), null));
            commandDispatcher.register(command);
        }
        name = "reload_kits";
        if (!Essentials.config.commandBlacklist.contains(name))
        {
            String perm;
            PermissionAPI.registerNode(perm = "command." + name, DefaultPermissionLevel.OP, "Can the player use /"
                    + name);
            // Setup with name and permission
            LiteralArgumentBuilder<CommandSource> command = Commands.literal(name).requires(cs -> CommandManager
                    .hasPerm(cs, perm));
            command = command.executes(ctx -> Kits.reload(ctx.getSource()));
            commandDispatcher.register(command);
        }
    }

    private static int reload(final CommandSource source) throws CommandSyntaxException
    {
        KitManager.init();
        Essentials.config.sendFeedback(source, "thutessentials.kits.reloaded", true);
        return 0;
    }

    private static int kits(final CommandSource source) throws CommandSyntaxException
    {
        final ServerPlayerEntity player = source.asPlayer();
        KitManager.sendKitsList(player);
        return 0;
    }

    private static int execute(final CommandSource source, final String warpName) throws CommandSyntaxException
    {
        final ServerPlayerEntity player = source.asPlayer();
        final MinecraftServer server = player.getServer();

        List<ItemStack> stacks;
        int delay = Essentials.config.kitReuseDelay;
        String kitTag = "kitTime";
        String perm = "thutessentials.kit.default";
        // Specific kit.
        if (warpName != null)
        {
            final KitSet kit = KitManager.kits.get(warpName);
            if (kit == null)
            {
                Essentials.config.sendError(source, "thutessentials.kits.no_kit");
                return 1;
            }
            perm = "thutessentials.kit." + warpName;
            kitTag = "kitTime_" + warpName;
            delay = kit.cooldown;
            stacks = kit.stacks;
        }
        else stacks = KitManager.kit;

        final IPermissionHandler manager = PermissionAPI.getPermissionHandler();
        final PlayerContext context = new PlayerContext(player);
        if (!manager.hasPermission(player.getGameProfile(), perm, context))
        {
            Essentials.config.sendError(source, "thutessentials.kits.noperms");
            return 1;
        }

        final long kitTime = PlayerDataHandler.getCustomDataTag(player).getLong(kitTag);
        if (delay <= 0 && kitTime != 0 || server.getWorld(DimensionType.OVERWORLD).getGameTime() < kitTime)
        {
            Essentials.config.sendError(source, "thutessentials.kits.too_soon");
            return 1;
        }
        for (final ItemStack stack : stacks)
        {
            EconomyManager.giveItem(player, stack.copy());
            PlayerDataHandler.getCustomDataTag(player).putLong(kitTag, server.getWorld(DimensionType.OVERWORLD)
                    .getGameTime() + delay);
        }
        return 0;
    }
}
