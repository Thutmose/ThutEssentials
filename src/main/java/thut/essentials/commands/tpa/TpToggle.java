package thut.essentials.commands.tpa;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.server.permission.DefaultPermissionLevel;
import net.minecraftforge.server.permission.PermissionAPI;
import thut.essentials.Essentials;
import thut.essentials.commands.CommandManager;
import thut.essentials.util.PlayerDataHandler;

public class TpToggle
{
    public static void register(final CommandDispatcher<CommandSource> commandDispatcher)
    {
        final String name = "tptoggle";
        if (Essentials.config.commandBlacklist.contains(name)) return;
        String perm;
        PermissionAPI.registerNode(perm = "command." + name, DefaultPermissionLevel.ALL, "Can the player use /" + name);

        // Setup with name and permission
        LiteralArgumentBuilder<CommandSource> command = Commands.literal(name).requires(cs -> CommandManager.hasPerm(cs,
                perm));
        // Register the execution.
        command = command.executes(ctx -> TpToggle.execute(ctx.getSource()));

        // Actually register the command.
        commandDispatcher.register(command);
    }

    private static int execute(final CommandSource source) throws CommandSyntaxException
    {
        final PlayerEntity player = source.asPlayer();
        final CompoundNBT tag = PlayerDataHandler.getCustomDataTag(player);
        final CompoundNBT tpaTag = tag.getCompound("tpa");
        final boolean ignore = !tpaTag.getBoolean("ignore");
        tpaTag.putBoolean("ignore", ignore);
        tag.put("tpa", tpaTag);
        PlayerDataHandler.saveCustomData(player);
        player.sendMessage(CommandManager.makeFormattedComponent("thutessentials.tpa.ignoreset" + ignore,
                TextFormatting.DARK_GREEN, true));
        return 0;
    }
}
