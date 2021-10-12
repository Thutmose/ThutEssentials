package thut.essentials.commands.util;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.server.permission.DefaultPermissionLevel;
import net.minecraftforge.server.permission.PermissionAPI;
import thut.essentials.Essentials;
import thut.essentials.commands.CommandManager;
import thut.essentials.util.PlayerDataHandler;

public class Speed
{
    public static void register(final CommandDispatcher<CommandSourceStack> commandDispatcher)
    {
        final String name = "speed";
        if (Essentials.config.commandBlacklist.contains(name)) return;
        String perm;
        PermissionAPI.registerNode(perm = "command." + name, DefaultPermissionLevel.OP, "Can the player use /" + name);
        // Setup with name and permission
        final LiteralArgumentBuilder<CommandSourceStack> command = Commands.literal(name).requires(cs -> CommandManager
                .hasPerm(cs, perm));
        command.then(Commands.argument("speed", FloatArgumentType.floatArg(0, (float) Essentials.config.maxSpeed))
                .executes(ctx -> Speed.execute(ctx.getSource(), FloatArgumentType.getFloat(ctx, "speed"))).then(Commands
                        .argument("player", EntityArgument.player()).executes(ctx -> Speed.execute(ctx.getSource(),
                                EntityArgument.getPlayer(ctx, "player"), FloatArgumentType.getFloat(ctx, "speed")))));
        // Actually register the command.
        commandDispatcher.register(command);
    }

    private static int execute(final CommandSourceStack source, final float value) throws CommandSyntaxException
    {
        return Speed.execute(source, source.getPlayerOrException(), value);
    }

    private static int execute(final CommandSourceStack source, final ServerPlayer player, final float value)
    {
        final CompoundTag tag = PlayerDataHandler.getCustomDataTag(player);
        final CompoundTag speed = tag.getCompound("speed");
        if (!speed.contains("defaultWalk"))
        {
            speed.putDouble("defaultWalk", player.getAbilities().getWalkingSpeed());
            speed.putDouble("defaultFly", player.getAbilities().getFlyingSpeed());
        }
        final CompoundTag cap = new CompoundTag();
        player.getAbilities().addSaveData(cap);
        final CompoundTag ab = cap.getCompound("abilities");
        ab.putFloat("flySpeed", (float) (speed.getDouble("defaultFly") * value));
        ab.putFloat("walkSpeed", (float) (speed.getDouble("defaultWalk") * value));
        cap.put("abilities", ab);
        player.getAbilities().loadSaveData(cap);
        player.onUpdateAbilities();
        tag.put("speed", speed);
        PlayerDataHandler.saveCustomData(player);
        return 0;
    }

}
