package thut.essentials.commands;

import java.util.Optional;
import java.util.UUID;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.ClickEvent.Action;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;
import thut.essentials.Essentials;
import thut.essentials.land.LandEventsHandler;
import thut.essentials.util.PermNodes;
import thut.essentials.util.PlayerMover;

public class CommandManager
{
    public static GameProfile getProfile(final MinecraftServer server, final UUID id)
    {
        Optional<GameProfile> profile = null;
        // First check profile cache.
        if (id != null) profile = server.getProfileCache().get(id);
        if (!profile.isPresent()) profile = Optional.of(new GameProfile(id, null));

        // Try to fill profile via secure method.
        LandEventsHandler.TEAMMANAGER.queueUpdate(profile.get());
        return profile.get();
    }

    public static GameProfile getProfile(final MinecraftServer server, final String arg)
    {
        UUID id = null;
        String name = null;

        // First check if arg is a UUID
        try
        {
            id = UUID.fromString(arg);
        }
        catch (final Exception e)
        {
            // If not a UUID, arg is the name.
            name = arg;
        }

        Optional<GameProfile> profile = null;

        // First check profile cache.
        if (id != null) profile = server.getProfileCache().get(id);
        if (!profile.isPresent()) profile = Optional.of(new GameProfile(id, name));

        // Try to fill profile via secure method.
        LandEventsHandler.TEAMMANAGER.queueUpdate(profile.get());

        // Temporarily update the UUID from server player list if possible
        if (profile.get().getId() == null)
        {
            final Player player = server.getPlayerList().getPlayerByName(profile.get().getName());
            profile = Optional.of(player.getGameProfile());
        }

        return profile.get();
    }

    public static boolean hasPerm(final CommandSourceStack source, final String permission)
    {
        try
        {
            final ServerPlayer player = source.getPlayerOrException();
            return CommandManager.hasPerm(player, permission);
        }
        catch (final CommandSyntaxException e)
        {
            // TODO decide what to actually do here?
            return true;
        }
    }

    public static boolean hasPerm(final ServerPlayer player, final String permission)
    {
        return PermNodes.getBooleanPerm(player, permission);
    }

    public static void register_commands(final CommandDispatcher<CommandSourceStack> commandDispatcher)
    {
        // We do this first, as commands might need it.
        MinecraftForge.EVENT_BUS.register(new PlayerMover());
        // Register commands.
        thut.essentials.commands.economy.Balance.register(commandDispatcher);
        thut.essentials.commands.economy.Pay.register(commandDispatcher);

        thut.essentials.commands.admin.StaffChat.register(commandDispatcher);
        thut.essentials.commands.admin.InitSpawn.register(commandDispatcher);

        thut.essentials.commands.homes.Homes.register(commandDispatcher);
        thut.essentials.commands.homes.Create.register(commandDispatcher);
        thut.essentials.commands.homes.Delete.register(commandDispatcher);

        thut.essentials.commands.warps.Warps.register(commandDispatcher);
        thut.essentials.commands.warps.Create.register(commandDispatcher);
        thut.essentials.commands.warps.Delete.register(commandDispatcher);

        thut.essentials.commands.tpa.Tpa.register(commandDispatcher);
        thut.essentials.commands.tpa.TpAccept.register(commandDispatcher);
        thut.essentials.commands.tpa.TpToggle.register(commandDispatcher);

        thut.essentials.commands.misc.Back.register(commandDispatcher);
        thut.essentials.commands.misc.RTP.register(commandDispatcher);
        thut.essentials.commands.misc.Bed.register(commandDispatcher);
        thut.essentials.commands.misc.Config.register(commandDispatcher);
        thut.essentials.commands.misc.Kits.register(commandDispatcher);
        thut.essentials.commands.misc.Spawn.register(commandDispatcher);
        thut.essentials.commands.misc.Nick.register(commandDispatcher);

        thut.essentials.commands.structures.Structuregen.register(commandDispatcher);

        thut.essentials.commands.land.util.Chat.register(commandDispatcher);
        thut.essentials.commands.land.util.Check.register(commandDispatcher);
        thut.essentials.commands.land.util.Home.register(commandDispatcher);
        thut.essentials.commands.land.util.Members.register(commandDispatcher);
        thut.essentials.commands.land.util.Reload.register(commandDispatcher);
        thut.essentials.commands.land.util.Teams.register(commandDispatcher);
        thut.essentials.commands.land.util.Show.register(commandDispatcher);

        thut.essentials.commands.util.Speed.register(commandDispatcher);
        thut.essentials.commands.util.Repair.register(commandDispatcher);
        thut.essentials.commands.util.Heal.register(commandDispatcher);
        thut.essentials.commands.util.RAM.register(commandDispatcher);

        thut.essentials.commands.land.management.Create.register(commandDispatcher);
        thut.essentials.commands.land.management.Rename.register(commandDispatcher);
        thut.essentials.commands.land.management.Invite.register(commandDispatcher);
        thut.essentials.commands.land.management.Join.register(commandDispatcher);
        thut.essentials.commands.land.management.Admins.register(commandDispatcher);
        thut.essentials.commands.land.management.Delete.register(commandDispatcher);
        thut.essentials.commands.land.management.Kick.register(commandDispatcher);
        thut.essentials.commands.land.management.Edit.register(commandDispatcher);
        thut.essentials.commands.land.management.Ranks.register(commandDispatcher);
        thut.essentials.commands.land.management.Relations.register(commandDispatcher);

        thut.essentials.commands.land.claims.Claim.register(commandDispatcher);
        thut.essentials.commands.land.claims.Owner.register(commandDispatcher);
        thut.essentials.commands.land.claims.Unclaim.register(commandDispatcher);
        thut.essentials.commands.land.claims.Deed.register(commandDispatcher);
        thut.essentials.commands.land.claims.Load.register(commandDispatcher);
        thut.essentials.commands.land.claims.Unload.register(commandDispatcher);

        thut.essentials.commands.util.Fly.register(commandDispatcher);
    }

    public static MutableComponent makeFormattedCommandLink(final String text, final String command,
            final Object... args)
    {
        final MutableComponent message = Essentials.config.getMessage(text, args);
        return message.setStyle(message.getStyle().withClickEvent(new ClickEvent(Action.RUN_COMMAND, command)));
    }

    public static MutableComponent makeFormattedComponent(final String text, final ChatFormatting colour,
            final boolean bold, final Object... args)
    {
        final MutableComponent message = Essentials.config.getMessage(text, args);
        Style style = message.getStyle();
        if (colour != null) style = style.withColor(TextColor.fromLegacyFormat(colour));
        if (bold) style = style.withBold(bold);
        return message.setStyle(style);
    }

    public static MutableComponent makeFormattedComponent(final String text, final ChatFormatting colour,
            final boolean bold)
    {
        return CommandManager.makeFormattedComponent(text, colour, bold, new Object[0]);
    }

    public static MutableComponent makeFormattedComponent(final String text)
    {
        return CommandManager.makeFormattedComponent(text, null, false, new Object[0]);
    }

    public static MutableComponent makeFormattedCommandLink(final String text, final String command)
    {
        return CommandManager.makeFormattedCommandLink(text, command, new Object[0]);
    }

}
