package thut.essentials.commands.misc;

import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

import com.google.common.collect.Maps;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import thut.essentials.ThutEssentials;
import thut.essentials.commands.CommandManager;
import thut.essentials.events.MoveEvent;
import thut.essentials.util.BaseCommand;
import thut.essentials.util.ConfigManager;
import thut.essentials.util.PlayerDataHandler;
import thut.essentials.util.Transporter;
import thut.essentials.util.Transporter.Vector3;

public class Spawn extends BaseCommand
{
    public static final ITextComponent INTERUPTED = new TextComponentString(
            TextFormatting.RED + "" + TextFormatting.ITALIC + "You must remain still to do that");

    public static class PlayerMover
    {
        private static Vector3 offset = new Vector3(0.5, 0.5, 0.5);

        private static class Mover
        {
            final long              moveTime;
            final EntityPlayer      player;
            final int               dimension;
            final BlockPos          moveTo;
            final Vector3           start;
            final ITextComponent    message;
            final ITextComponent    failMess;
            final boolean           event;
            final Predicate<Entity> callback;

            public Mover(EntityPlayer player, long moveTime, int dimension, BlockPos moveTo, ITextComponent message,
                    ITextComponent failMess, Predicate<Entity> callback, boolean event)
            {
                this.player = player;
                this.dimension = dimension;
                this.moveTime = moveTime;
                this.moveTo = moveTo;
                this.message = message;
                this.failMess = failMess;
                this.event = event;
                this.callback = callback;
                start = new Vector3(player.posX, player.posY, player.posZ);
            }

            private void move()
            {
                if (event) MinecraftForge.EVENT_BUS.post(new MoveEvent(player));
                Vector3 dest = new Vector3(moveTo);
                dest.add(offset);
                Entity player1 = Transporter.teleportEntity(player, dest, dimension, false);
                if (callback != null) callback.test(player1);
                if (message != null) player1.addChatMessage(message);
            }
        }

        public static void setMove(final EntityPlayer player, final int moveTime, final int dimension,
                final BlockPos moveTo, final ITextComponent message, final ITextComponent failMess)
        {
            setMove(player, moveTime, dimension, moveTo, message, failMess, true);
        }

        public static void setMove(final EntityPlayer player, final int moveTime, final int dimension,
                final BlockPos moveTo, final ITextComponent message, final ITextComponent failMess, final boolean event)
        {
            setMove(player, moveTime, dimension, moveTo, message, failMess, null, event);
        }

        public static void setMove(final EntityPlayer player, final int moveTime, final int dimension,
                final BlockPos moveTo, final ITextComponent message, final ITextComponent failMess,
                final Predicate<Entity> callback, final boolean event)
        {
            player.getServer().addScheduledTask(new Runnable()
            {
                @Override
                public void run()
                {
                    if (!toMove.containsKey(player.getUniqueID()))
                    {
                        if (moveTime > 0) player.addChatMessage(new TextComponentString(
                                TextFormatting.GREEN + "Initiating Teleport, please remain still."));
                        toMove.put(player.getUniqueID(),
                                new Mover(player, moveTime + player.getEntityWorld().getTotalWorldTime(), dimension,
                                        moveTo, message, failMess, callback, event));
                    }
                }
            });
        }

        static Map<UUID, Mover> toMove = Maps.newHashMap();

        static
        {
            MinecraftForge.EVENT_BUS.register(new PlayerMover());
        }

        @SubscribeEvent
        public void playerTick(LivingUpdateEvent tick)
        {
            if (toMove.containsKey(tick.getEntity().getUniqueID()))
            {
                Mover mover = toMove.get(tick.getEntity().getUniqueID());
                Vector3 loc = new Vector3(mover.player.posX, mover.player.posY, mover.player.posZ);
                Vector3 diff = new Vector3(mover.start.x, mover.start.y, mover.start.z);
                diff.sub(loc);
                if (diff.lengthSquared() > 0.0)
                {
                    if (mover.failMess != null)
                    {
                        tick.getEntity().addChatMessage(mover.failMess);
                    }
                    toMove.remove(tick.getEntity().getUniqueID());
                    return;
                }
                if (tick.getEntity().getEntityWorld().getTotalWorldTime() > mover.moveTime)
                {
                    mover.move();
                    toMove.remove(tick.getEntity().getUniqueID());
                }
            }
        }

    }

    public Spawn()
    {
        super("spawn", 0);
    }

    @Override
    public String getCommandUsage(ICommandSender sender)
    {
        return "/" + getCommandName();
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException
    {
        EntityPlayer player = getCommandSenderAsPlayer(sender);
        NBTTagCompound tag = PlayerDataHandler.getCustomDataTag(player);
        NBTTagCompound tptag = tag.getCompoundTag("tp");
        long last = tptag.getLong("spawnDelay");
        long time = player.getServer().worldServerForDimension(0).getTotalWorldTime();
        if (last > time)
        {
            player.addChatMessage(
                    CommandManager.makeFormattedComponent("Too Soon between Warp attempt", TextFormatting.RED, false));
            return;
        }
        if (args.length == 0)
        {
            BlockPos spawn = server.worldServerForDimension(ThutEssentials.instance.config.spawnDimension)
                    .getSpawnPoint();
            ITextComponent teleMess = CommandManager.makeFormattedComponent("Warped to Spawn", TextFormatting.GREEN);
            PlayerMover.setMove(player, ThutEssentials.instance.config.spawnActivateDelay,
                    ThutEssentials.instance.config.spawnDimension, spawn, teleMess, Spawn.INTERUPTED);
            tptag.setLong("spawnDelay", time + ConfigManager.INSTANCE.spawnReUseDelay);
            tag.setTag("tp", tptag);
            PlayerDataHandler.saveCustomData(player);
        }
        else if (args[0].equalsIgnoreCase("me"))
        {
            BlockPos spawn = player.getBedLocation();
            if (spawn != null)
            {
                ITextComponent teleMess = CommandManager.makeFormattedComponent("Warped to Bed location",
                        TextFormatting.GREEN);
                PlayerMover.setMove(player, ThutEssentials.instance.config.spawnActivateDelay, player.dimension, spawn,
                        teleMess, Spawn.INTERUPTED);
                tptag.setLong("spawnDelay", time + ConfigManager.INSTANCE.spawnReUseDelay);
                tag.setTag("tp", tptag);
            }
            else
            {
                throw new CommandException("no bed found");
            }
        }
    }

}
