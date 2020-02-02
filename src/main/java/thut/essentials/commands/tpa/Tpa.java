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
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.server.permission.DefaultPermissionLevel;
import net.minecraftforge.server.permission.PermissionAPI;
import thut.essentials.Essentials;
import thut.essentials.commands.CommandManager;
import thut.essentials.util.PlayerDataHandler;

public class Tpa
{
    public static void register(final CommandDispatcher<CommandSource> commandDispatcher)
    {
        // TODO configurable this.
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
        final PlayerEntity player = source.asPlayer();
        final CompoundNBT tag = PlayerDataHandler.getCustomDataTag(target);
        final CompoundNBT tpaTag = tag.getCompound("tpa");
        if (tpaTag.getBoolean("ignore")) return 1;

        final ITextComponent header = player.getDisplayName().appendSibling(CommandManager.makeFormattedComponent(
                "thutessentials.tpa.requested", TextFormatting.YELLOW, true));
        target.sendMessage(header);

        ITextComponent tpMessage;
        final String tpaccept = "tpaccept";// TODO config this.
        final ITextComponent accept = CommandManager.makeFormattedCommandLink("thutessentials.tpa.accept", "/"
                + tpaccept + " " + player.getCachedUniqueIdString() + " accept", TextFormatting.GREEN, true);
        final ITextComponent deny = CommandManager.makeFormattedCommandLink("thutessentials.tpa.deny", "/" + tpaccept
                + " " + player.getCachedUniqueIdString() + " deny", TextFormatting.RED, true);
        tpMessage = accept.appendSibling(new StringTextComponent("      /      ")).appendSibling(deny);
        target.sendMessage(tpMessage);
        tpaTag.putString("R", player.getCachedUniqueIdString());
        tag.put("tpa", tpaTag);
        PlayerDataHandler.saveCustomData(target);
        player.sendMessage(CommandManager.makeFormattedComponent(target.getDisplayName().getFormattedText()
                + "thutessentials.tpa.requestsent", TextFormatting.DARK_GREEN, true));
        return 0;
    }
}
