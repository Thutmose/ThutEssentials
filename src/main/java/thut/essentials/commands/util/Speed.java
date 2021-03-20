package thut.essentials.commands.util;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.arguments.EntityArgument;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraftforge.server.permission.DefaultPermissionLevel;
import net.minecraftforge.server.permission.PermissionAPI;
import thut.essentials.Essentials;
import thut.essentials.commands.CommandManager;
import thut.essentials.util.PlayerDataHandler;

public class Speed
{
    public static void register(final CommandDispatcher<CommandSource> commandDispatcher)
    {
        final String name = "speed";
        if (Essentials.config.commandBlacklist.contains(name)) return;
        String perm;
        PermissionAPI.registerNode(perm = "command." + name, DefaultPermissionLevel.OP, "Can the player use /" + name);
        // Setup with name and permission
        final LiteralArgumentBuilder<CommandSource> command = Commands.literal(name).requires(cs -> CommandManager
                .hasPerm(cs, perm));
        command.then(Commands.argument("speed", FloatArgumentType.floatArg(0, (float) Essentials.config.maxSpeed))
                .executes(ctx -> Speed.execute(ctx.getSource(), FloatArgumentType.getFloat(ctx, "speed"))).then(Commands
                        .argument("player", EntityArgument.player()).executes(ctx -> Speed.execute(ctx.getSource(),
                                EntityArgument.getPlayer(ctx, "player"), FloatArgumentType.getFloat(ctx, "speed")))));
        // Actually register the command.
        commandDispatcher.register(command);
    }

    private static int execute(final CommandSource source, final float value) throws CommandSyntaxException
    {
        return Speed.execute(source, source.getPlayerOrException(), value);
    }

    private static int execute(final CommandSource source, final ServerPlayerEntity player, final float value)
    {
        final CompoundNBT tag = PlayerDataHandler.getCustomDataTag(player);
        final CompoundNBT speed = tag.getCompound("speed");
        if (!speed.contains("defaultWalk"))
        {
            speed.putDouble("defaultWalk", player.abilities.getWalkingSpeed());
            speed.putDouble("defaultFly", player.abilities.getFlyingSpeed());
        }
        final CompoundNBT cap = new CompoundNBT();
        player.abilities.addSaveData(cap);
        final CompoundNBT ab = cap.getCompound("abilities");
        ab.putFloat("flySpeed", (float) (speed.getDouble("defaultFly") * value));
        ab.putFloat("walkSpeed", (float) (speed.getDouble("defaultWalk") * value));
        cap.put("abilities", ab);
        player.abilities.loadSaveData(cap);
        player.onUpdateAbilities();
        tag.put("speed", speed);
        PlayerDataHandler.saveCustomData(player);
        return 0;
    }

}
