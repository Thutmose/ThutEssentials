package thut.essentials.commands.misc;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSource;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.server.MinecraftServer;
import thut.essentials.util.BaseCommand;
import thut.essentials.util.ConfigManager;
import thut.essentials.util.PlayerDataHandler;

public class Speed extends BaseCommand
{
    public Speed()
    {
        super("speed", 2);
    }

    /** Return whether the specified command parameter index is a username
     * parameter. */
    @Override
    public boolean isUsernameIndex(String[] args, int index)
    {
        return index == 0;
    }

    @Override
    public void execute(MinecraftServer server, ICommandSource sender, String[] args) throws CommandException
    {
        ServerPlayerEntity player = args.length == 1 ? getPlayerBySender(sender)
                : getPlayer(server, sender, args[0]);
        double value = args.length == 1 ? Double.parseDouble(args[0]) : Double.parseDouble(args[1]);
        value = Math.min(ConfigManager.INSTANCE.speedCap, value);
        CompoundNBT tag = PlayerDataHandler.getCustomDataTag(player);
        CompoundNBT speed = tag.getCompound("speed");
        if (!speed.hasKey("defaultWalk"))
        {
            speed.setDouble("defaultWalk", player.capabilities.getWalkSpeed());
            speed.setDouble("defaultFly", player.capabilities.getFlySpeed());
        }
        CompoundNBT cap = new CompoundNBT();
        player.capabilities.writeCapabilitiesToNBT(cap);
        CompoundNBT ab = cap.getCompound("abilities");
        ab.putFloat("flySpeed", (float) (speed.getDouble("defaultFly") * value));
        ab.putFloat("walkSpeed", (float) (speed.getDouble("defaultWalk") * value));
        cap.setTag("abilities", ab);
        player.capabilities.readCapabilitiesFromNBT(cap);
        player.sendPlayerAbilities();
        tag.setTag("speed", speed);
        PlayerDataHandler.saveCustomData(player);
    }
}
