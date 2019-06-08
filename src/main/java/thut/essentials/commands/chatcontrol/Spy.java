package thut.essentials.commands.chatcontrol;

import java.util.Set;
import java.util.UUID;

import com.google.common.collect.Sets;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.CommandEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import thut.essentials.commands.CommandManager;
import thut.essentials.util.BaseCommand;

public class Spy extends BaseCommand
{
    UUID                serverID      = new UUID(0, 0);
    Set<UUID>           spies         = Sets.newHashSet();
    Set<ICommandSource> customSenders = Sets.newHashSet();

    public Spy()
    {
        super("spy", 2);
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = false)
    public void chat(CommandEvent event)
    {
        if (event.getCommand().getName().equals("tell") && event.getParameters().length > 1)
        {
            ITextComponent message;
            PlayerEntity target = null;
            PlayerEntity sayer;
            try
            {
                target = getPlayer(event.getSender().getServer(), event.getSender(), event.getParameters()[0]);
                sayer = getCommandSenderAsPlayer(event.getSender());
            }
            catch (CommandException e)
            {
                return;
            }
            ITextComponent name = target.getDisplayName();
            message = CommandManager.makeFormattedComponent("[Spy]", TextFormatting.GOLD, false);
            ITextComponent arrow = CommandManager.makeFormattedComponent(" -> ", TextFormatting.GOLD, false);
            ITextComponent sender = event.getSender().getDisplayName();
            String values = ": " + event.getParameters()[1];
            for (int i = 2; i < event.getParameters().length; i++)
            {
                values = values + " " + event.getParameters()[i];
            }
            message.appendSibling(sender).appendSibling(arrow).appendSibling(name)
                    .appendSibling(new StringTextComponent(values));
            for (UUID id : spies)
            {
                if (id == serverID)
                {
                    for (ICommandSource sender2 : customSenders)
                        sender2.sendMessage(message);
                }
                else if (!(id.equals(target.getUniqueID()) || id.equals(sayer.getUniqueID())))
                {
                    ServerPlayerEntity spy = event.getSender().getServer().getPlayerList().getPlayerByUUID(id);
                    spy.sendMessage(message);
                }
            }
        }
    }

    @Override
    public void execute(MinecraftServer server, ICommandSource sender, String[] args) throws CommandException
    {
        UUID id = null;
        if (!(sender instanceof ServerPlayerEntity))
        {
            id = serverID;
        }
        else
        {
            id = getPlayerBySender(sender).getUniqueID();
        }
        if (spies.remove(id))
        {
            customSenders.remove(sender);
            sender.sendMessage(new StringTextComponent("Spying turned off."));
        }
        else
        {
            spies.add(id);
            if (id == serverID) customSenders.add(sender);
            sender.sendMessage(new StringTextComponent("Spying turned on."));
        }
    }

}
