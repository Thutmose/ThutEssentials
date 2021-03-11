package thut.essentials.commands.tpa;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.arguments.EntityArgument;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.Util;
import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.server.permission.DefaultPermissionLevel;
import net.minecraftforge.server.permission.PermissionAPI;
import thut.essentials.Essentials;
import thut.essentials.commands.CommandManager;
import thut.essentials.util.PlayerDataHandler;

public class Tpa
{
    public static void register(final CommandDispatcher<CommandSource> commandDispatcher)
    {
        final String name = "tpa";
        if (Essentials.config.commandBlacklist.contains(name)) return;
        String perm;
        PermissionAPI.registerNode(perm = "command." + name, DefaultPermissionLevel.ALL, "Can the player use /" + name);

        // Setup with name and permission
        LiteralArgumentBuilder<CommandSource> command = Commands.literal(name).requires(cs -> CommandManager.hasPerm(cs,
                perm));
        // Register the execution.
        command = command.then(Commands.argument("target_player", EntityArgument.player()).executes(ctx -> Tpa.execute(
                ctx.getSource(), EntityArgument.getPlayer(ctx, "target_player"))));

        // Actually register the command.
        commandDispatcher.register(command);
    }

    private static int execute(final CommandSource source, final ServerPlayerEntity target)
            throws CommandSyntaxException
    {
        final PlayerEntity player = source.getPlayerOrException();
        if (!Essentials.config.tpaCrossDim && target.getCommandSenderWorld() != player.getCommandSenderWorld())
        {
            player.sendMessage(Essentials.config.getMessage("thutessentials.tp.wrongdim"), Util.NIL_UUID);
            return 1;
        }
        CompoundNBT tag = PlayerDataHandler.getCustomDataTag(player);
        CompoundNBT tpaTag = tag.getCompound("tpa");

        final long last = tag.getLong("tpaDelay");
        final long time = player.getServer().getLevel(World.OVERWORLD).getGameTime();
        if (last > time && Essentials.config.tpaReUseDelay > 0)
        {
            player.sendMessage(Essentials.config.getMessage("thutessentials.tp.tosoon"), Util.NIL_UUID);
            return 1;
        }
        tpaTag.putLong("tpaDelay", time + Essentials.config.tpaReUseDelay);
        tag.put("tpa", tpaTag);
        PlayerDataHandler.saveCustomData(player);

        tag = PlayerDataHandler.getCustomDataTag(target);
        tpaTag = tag.getCompound("tpa");
        if (tpaTag.getBoolean("ignore")) return 1;

        final IFormattableTextComponent header = ((IFormattableTextComponent) player.getDisplayName()).append(
                CommandManager.makeFormattedComponent("thutessentials.tpa.requested"));
        target.sendMessage(header, Util.NIL_UUID);

        IFormattableTextComponent tpMessage;
        final String tpaccept = "tpaccept";
        final IFormattableTextComponent accept = CommandManager.makeFormattedCommandLink("thutessentials.tpa.accept",
                "/" + tpaccept + " " + player.getStringUUID() + " accept");
        final IFormattableTextComponent deny = CommandManager.makeFormattedCommandLink("thutessentials.tpa.deny", "/"
                + tpaccept + " " + player.getStringUUID() + " deny");
        tpMessage = accept.append(new StringTextComponent("      /      ")).append(deny);
        target.sendMessage(tpMessage, Util.NIL_UUID);
        tpaTag.putString("R", player.getStringUUID());
        tag.put("tpa", tpaTag);
        PlayerDataHandler.saveCustomData(target);
        player.sendMessage(CommandManager.makeFormattedComponent("thutessentials.tpa.requestsent",
                TextFormatting.DARK_GREEN, true, target.getDisplayName()), Util.NIL_UUID);
        return 0;
    }
}
