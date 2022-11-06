package thut.essentials.commands.land.claims;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingEvent.LivingTickEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import thut.essentials.Essentials;
import thut.essentials.commands.CommandManager;
import thut.essentials.events.ClaimLandEvent;
import thut.essentials.land.LandManager;
import thut.essentials.land.LandManager.KGobalPos;
import thut.essentials.land.LandManager.LandTeam;
import thut.essentials.land.LandSaveHandler;
import thut.essentials.util.ChatHelper;
import thut.essentials.util.CoordinateUtls;
import thut.essentials.util.PermNodes;
import thut.essentials.util.PermNodes.DefaultPermissionLevel;

public class Claim
{
    private static final String BYPASSLIMIT = "thutessentials.land.claim.nolimit";
    private static final String AUTOCLAIM = "thutessentials.land.claim.autoclaim";
    private static final String BULKCLAIM = "thutessentials.land.claim.bulkclaim";

    private static final Set<UUID> autoclaimers = Sets.newHashSet();

    private static final Map<UUID, KGobalPos> claimstarts = Maps.newHashMap();

    public static void register(final CommandDispatcher<CommandSourceStack> commandDispatcher)
    {
        final String name = "claim";
        if (Essentials.config.commandBlacklist.contains(name)) return;
        MinecraftForge.EVENT_BUS.register(Claim.class);
        String perm;
        PermNodes.registerBooleanNode(perm = "command." + name, DefaultPermissionLevel.ALL, "Can the player use /" + name);
        PermNodes.registerBooleanNode(Claim.BYPASSLIMIT, DefaultPermissionLevel.OP,
                "Permission to bypass the land per player limit for a team.");
        PermNodes.registerBooleanNode(Claim.AUTOCLAIM, DefaultPermissionLevel.OP,
                "Permission to use autoclaim to claim land as they walk around.");
        PermNodes.registerBooleanNode(Claim.BULKCLAIM, DefaultPermissionLevel.OP,
                "Permission to use /claim start and /claim end to bulk claim chunks.");

        // Setup with name and permission
        LiteralArgumentBuilder<CommandSourceStack> command = Commands.literal(name)
                .requires(cs -> CommandManager.hasPerm(cs, perm));

        // Entire chunk
        command = command.executes(ctx -> Claim.execute(ctx.getSource(), true, true, false));
        commandDispatcher.register(command);

        command = Commands.literal(name).requires(cs -> CommandManager.hasPerm(cs, perm));
        command = command
                .then(Commands.literal("up").executes(ctx -> Claim.execute(ctx.getSource(), true, false, false)));
        commandDispatcher.register(command);

        command = Commands.literal(name).requires(cs -> CommandManager.hasPerm(cs, perm));
        command = command
                .then(Commands.literal("down").executes(ctx -> Claim.execute(ctx.getSource(), false, true, false)));
        commandDispatcher.register(command);

        command = Commands.literal(name).requires(cs -> CommandManager.hasPerm(cs, perm));
        command = command
                .then(Commands.literal("here").executes(ctx -> Claim.execute(ctx.getSource(), false, false, true)));
        commandDispatcher.register(command);

        command = Commands.literal(name).requires(cs -> CommandManager.hasPerm(cs, perm));
        command = command.then(Commands.literal("auto").requires(cs -> CommandManager.hasPerm(cs, Claim.AUTOCLAIM))
                .executes(ctx -> Claim.executeAuto(ctx.getSource())));
        commandDispatcher.register(command);

        command = Commands.literal(name).requires(cs -> CommandManager.hasPerm(cs, perm));
        command = command.then(Commands.literal("check").executes(ctx -> Claim.executeCheck(ctx.getSource())));
        commandDispatcher.register(command);

        command = Commands.literal(name);
        command = command.then(Commands.literal("start").requires(cs -> CommandManager.hasPerm(cs, Claim.BULKCLAIM))
                .executes(ctx -> Claim.executeStart(ctx.getSource())));
        commandDispatcher.register(command);

        command = Commands.literal(name);
        command = command.then(Commands.literal("end").requires(cs -> CommandManager.hasPerm(cs, Claim.BULKCLAIM))
                .executes(ctx -> Claim.executeEnd(ctx.getSource())));
        commandDispatcher.register(command);
    }

    @SubscribeEvent
    public static void livingUpdate(final LivingTickEvent evt)
    {
        if (!evt.getEntity().isAlive() || !Claim.autoclaimers.contains(evt.getEntity().getUUID())
                || !(evt.getEntity() instanceof ServerPlayer))
            return;
        final ServerPlayer player = (ServerPlayer) evt.getEntity();
        final LandTeam team = LandManager.getTeam(player);

        BlockPos here;
        BlockPos old;
        here = new BlockPos(player.xCloak, player.yCloak, player.zCloak);
        old = new BlockPos(player.xCloakO, player.yCloakO, player.zCloakO);
        final KGobalPos newChunk = CoordinateUtls
                .chunkPos(KGobalPos.getPosition(player.getCommandSenderWorld().dimension(), here));
        final KGobalPos oldChunk = CoordinateUtls
                .chunkPos(KGobalPos.getPosition(player.getCommandSenderWorld().dimension(), old));
        if (newChunk.equals(oldChunk)) return;
        final Level dim = player.getCommandSenderWorld();

        final int x = Mth.floor(player.blockPosition().getX() >> 4);
        final int z = Mth.floor(player.blockPosition().getZ() >> 4);
        final boolean noLimit = PermNodes.getBooleanPerm(player, Claim.BYPASSLIMIT);
        for (int i = dim.getMinSection(); i < dim.getMaxSection(); i++)
            Claim.claim(x, i, z, dim, player, team, false, noLimit);
    }

    @SubscribeEvent
    public static void serverUnload(final ServerStoppingEvent evt)
    {
        Claim.autoclaimers.clear();
        Claim.claimstarts.clear();
        MinecraftForge.EVENT_BUS.unregister(Claim.class);
    }

    private static int executeCheck(final CommandSourceStack source) throws CommandSyntaxException
    {
        final Player player = source.getPlayerOrException();
        final LandTeam team = LandManager.getTeam(player);
        final int count = LandManager.getInstance().countLand(team.teamName);
        final int teamCount = team.member.size();
        final int maxLand = team.maxLand < 0 ? teamCount * Essentials.config.teamLandPerPlayer : team.maxLand;
        ChatHelper.sendSystemMessage(player,
                Essentials.config.getMessage("thutessentials.claim.claimed.count", count, maxLand));
        return 0;
    }

    private static int executeStart(final CommandSourceStack source) throws CommandSyntaxException
    {
        final Player player = source.getPlayerOrException();
        final LandTeam team = LandManager.getTeam(player);
        if (!team.hasRankPerm(player.getUUID(), LandTeam.CLAIMPERM))
        {
            ChatHelper.sendSystemMessage(player,
                    Essentials.config.getMessage("thutessentials.claim.notallowed.teamperms"));
            return 1;
        }
        final KGobalPos start = CoordinateUtls.forMob(player);
        Claim.claimstarts.put(player.getUUID(), start);
        ChatHelper.sendSystemMessage(player,
                Essentials.config.getMessage("thutessentials.claim.start.set", player.blockPosition()));
        return 0;
    }

    private static int executeEnd(final CommandSourceStack source) throws CommandSyntaxException
    {
        final ServerPlayer player = source.getPlayerOrException();
        final LandTeam team = LandManager.getTeam(player);
        if (!team.hasRankPerm(player.getUUID(), LandTeam.CLAIMPERM))
        {
            ChatHelper.sendSystemMessage(player,
                    Essentials.config.getMessage("thutessentials.claim.notallowed.teamperms"));
            return 1;
        }
        final KGobalPos end = CoordinateUtls.forMob(player);
        final KGobalPos start = Claim.claimstarts.get(player.getUUID());
        if (start == null)
        {
            ChatHelper.sendSystemMessage(player, Essentials.config.getMessage("thutessentials.claim.start.not_set"));
            return 1;
        }
        if (end.getDimension() != start.getDimension())
        {
            ChatHelper.sendSystemMessage(player, Essentials.config.getMessage("thutessentials.claim.start.wrong_dim"));
            return 1;
        }
        player.getServer().execute(() -> { // easy way to sort the x, z
                                           // coordinates by min/max
            final AABB box = new AABB(start.getPos(), end.getPos());
            final boolean noLimit = PermNodes.getBooleanPerm(player, Claim.BYPASSLIMIT);
            final Level dim = player.getCommandSenderWorld();
            int n = 0;
            // Convert to chunk coordinates for the loop with the >> 4
            for (int x = Mth.floor(box.minX) >> 4; x <= Mth.floor(box.maxX) >> 4; x++)
                for (int z = Mth.floor(box.minZ) >> 4; z <= Mth.floor(box.maxZ) >> 4; z++)
                    for (int y = dim.getMinSection(); y < dim.getMaxSection(); y++)

                        n += Claim.claim(x, y, z, dim, player, team, false, noLimit);
            ChatHelper.sendSystemMessage(player,
                    Essentials.config.getMessage("thutessentials.claim.start.end", n, team.teamName));
        });
        return 0;
    }

    private static int executeAuto(final CommandSourceStack source) throws CommandSyntaxException
    {
        final ServerPlayer player = source.getPlayerOrException();
        if (Claim.autoclaimers.contains(player.getUUID()))
        {
            Claim.autoclaimers.remove(player.getUUID());
            Essentials.config.sendFeedback(source, "thutessentials.claim.autooff", true);
        }
        else
        {
            Claim.autoclaimers.add(player.getUUID());
            Essentials.config.sendFeedback(source, "thutessentials.claim.autoon", true);
        }
        return 0;
    }

    private static int execute(final CommandSourceStack source, final boolean up, final boolean down,
            final boolean here) throws CommandSyntaxException
    {
        final ServerPlayer player = source.getPlayerOrException();
        final LandTeam team = LandManager.getTeam(player);
        if (!team.hasRankPerm(player.getUUID(), LandTeam.CLAIMPERM))
        {
            ChatHelper.sendSystemMessage(player,
                    Essentials.config.getMessage("thutessentials.claim.notallowed.teamperms"));
            return 1;
        }
        final boolean noLimit = PermNodes.getBooleanPerm(player, Claim.BYPASSLIMIT);
        player.getServer().execute(() -> {

            final int x = player.blockPosition().getX() >> 4;
            final int y = player.blockPosition().getY() >> 4;
            final int z = player.blockPosition().getZ() >> 4;
            final Level dim = player.getCommandSenderWorld();

            if (here)
            {
                Claim.claim(x, y, z, dim, player, team, true, noLimit);
                return;
            }

            final int min = down ? dim.getMinSection() : y;
            final int max = up ? dim.getMaxSection() : y;

            boolean claimed = false;
            int claimnum = 0;
            int notclaimed = 0;
            for (int i = min; i < max; i++)
            {
                final int check = Claim.claim(x, i, z, dim, player, team, false, noLimit);
                if (check == 0)
                {
                    claimed = true;
                    claimnum++;
                }
                else notclaimed++;
                if (check == 3)
                {
                    ChatHelper.sendSystemMessage(player,
                            Essentials.config.getMessage("thutessentials.claim.notallowed.needmoreland"));
                    break;
                }
            }
            if (notclaimed > 0) ChatHelper.sendSystemMessage(player,
                    Essentials.config.getMessage("thutessentials.claim.warn.alreadyclaimed", notclaimed));
            if (claimed) ChatHelper.sendSystemMessage(player,
                    Essentials.config.getMessage("thutessentials.claim.claimed.num", claimnum, team.teamName));
            else ChatHelper.sendSystemMessage(player,
                    Essentials.config.getMessage("thutessentials.claim.claimed.failed", team.teamName));
            if (claimed) LandSaveHandler.saveTeam(team.teamName);
        });
        return 0;
    }

    public static int claim(final int x, final int y, final int z, final Level dim, final Player player,
            final LandTeam team, final boolean messages, final boolean noLimit)
    {
        return Claim.claim(dim, new BlockPos(x, y, z), player, team, messages, noLimit);
    }

    public static int claim(final Level world, final BlockPos chunkCoord, final Player player, final LandTeam team,
            final boolean messages, final boolean noLimit)
    {
        final LandTeam owner = LandManager.getInstance().getLandOwner(world, chunkCoord, true);
        if (!LandManager.isWild(owner))
        {
            if (messages) ChatHelper.sendSystemMessage(player,
                    Essentials.config.getMessage("thutessentials.claim.notallowed.alreadyclaimedby", owner.teamName));
            return 1;
        }
        final int teamCount = team.member.size();
        final int maxLand = team.maxLand < 0 ? teamCount * Essentials.config.teamLandPerPlayer : team.maxLand;
        final int count = LandManager.getInstance().countLand(team.teamName);
        if (count >= maxLand && !noLimit)
        {
            if (messages) ChatHelper.sendSystemMessage(player,
                    Essentials.config.getMessage("thutessentials.claim.notallowed.needmoreland"));
            return 3;
        }
        final KGobalPos pos = KGobalPos.getPosition(world.dimension(), chunkCoord);
        final ClaimLandEvent event = new ClaimLandEvent(pos, player, team.teamName);
        MinecraftForge.EVENT_BUS.post(event);
        LandManager.getInstance().claimLand(team.teamName, world, chunkCoord, true);
        if (messages) ChatHelper.sendSystemMessage(player,
                Essentials.config.getMessage("thutessentials.claim.claimed", team.teamName));
        return 0;
    }

}
