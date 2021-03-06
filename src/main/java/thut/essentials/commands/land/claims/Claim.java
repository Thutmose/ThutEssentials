package thut.essentials.commands.land.claims;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.Util;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.server.FMLServerStoppingEvent;
import net.minecraftforge.server.permission.DefaultPermissionLevel;
import net.minecraftforge.server.permission.PermissionAPI;
import thut.essentials.Essentials;
import thut.essentials.commands.CommandManager;
import thut.essentials.events.ClaimLandEvent;
import thut.essentials.land.LandManager;
import thut.essentials.land.LandManager.KGobalPos;
import thut.essentials.land.LandManager.LandTeam;
import thut.essentials.land.LandSaveHandler;
import thut.essentials.util.CoordinateUtls;

public class Claim
{
    private static final String BYPASSLIMIT = "thutessentials.land.claim.nolimit";
    private static final String AUTOCLAIM   = "thutessentials.land.claim.autoclaim";
    private static final String BULKCLAIM   = "thutessentials.land.claim.bulkclaim";

    private static final Set<UUID> autoclaimers = Sets.newHashSet();

    private static final Map<UUID, KGobalPos> claimstarts = Maps.newHashMap();

    public static void register(final CommandDispatcher<CommandSource> commandDispatcher)
    {
        final String name = "claim";
        if (Essentials.config.commandBlacklist.contains(name)) return;
        MinecraftForge.EVENT_BUS.register(Claim.class);
        String perm;
        PermissionAPI.registerNode(perm = "command." + name, DefaultPermissionLevel.ALL, "Can the player use /" + name);
        PermissionAPI.registerNode(Claim.BYPASSLIMIT, DefaultPermissionLevel.OP,
                "Permission to bypass the land per player limit for a team.");
        PermissionAPI.registerNode(Claim.AUTOCLAIM, DefaultPermissionLevel.OP,
                "Permission to use autoclaim to claim land as they walk around.");
        PermissionAPI.registerNode(Claim.BULKCLAIM, DefaultPermissionLevel.OP,
                "Permission to use /claim start and /claim end to bulk claim chunks.");

        // Setup with name and permission
        LiteralArgumentBuilder<CommandSource> command = Commands.literal(name).requires(cs -> CommandManager.hasPerm(cs,
                perm));

        // Entire chunk
        command = command.executes(ctx -> Claim.execute(ctx.getSource(), true, true, false));
        commandDispatcher.register(command);

        command = Commands.literal(name).requires(cs -> CommandManager.hasPerm(cs, perm));
        command = command.then(Commands.literal("up").executes(ctx -> Claim.execute(ctx.getSource(), true, false,
                false)));
        commandDispatcher.register(command);

        command = Commands.literal(name).requires(cs -> CommandManager.hasPerm(cs, perm));
        command = command.then(Commands.literal("down").executes(ctx -> Claim.execute(ctx.getSource(), false, true,
                false)));
        commandDispatcher.register(command);

        command = Commands.literal(name).requires(cs -> CommandManager.hasPerm(cs, perm));
        command = command.then(Commands.literal("here").executes(ctx -> Claim.execute(ctx.getSource(), false, false,
                true)));
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
    public static void livingUpdate(final LivingUpdateEvent evt)
    {
        if (!evt.getEntity().isAlive() || !Claim.autoclaimers.contains(evt.getEntity().getUUID()) || !(evt
                .getEntityLiving() instanceof ServerPlayerEntity)) return;
        final ServerPlayerEntity player = (ServerPlayerEntity) evt.getEntityLiving();
        final LandTeam team = LandManager.getTeam(player);

        BlockPos here;
        BlockPos old;
        here = new BlockPos(player.xCloak, player.yCloak, player.zCloak);
        old = new BlockPos(player.xCloakO, player.yCloakO, player.zCloakO);
        final KGobalPos newChunk = CoordinateUtls.chunkPos(KGobalPos.getPosition(player.getCommandSenderWorld()
                .dimension(), here));
        final KGobalPos oldChunk = CoordinateUtls.chunkPos(KGobalPos.getPosition(player.getCommandSenderWorld()
                .dimension(), old));
        if (newChunk.equals(oldChunk)) return;
        final World dim = player.getCommandSenderWorld();

        final int x = MathHelper.floor(player.blockPosition().getX() >> 4);
        final int z = MathHelper.floor(player.blockPosition().getZ() >> 4);
        final boolean noLimit = PermissionAPI.hasPermission(player, Claim.BYPASSLIMIT);
        for (int i = 0; i < 16; i++)
            Claim.claim(x, i, z, dim, player, team, false, noLimit);
    }

    @SubscribeEvent
    public static void serverUnload(final FMLServerStoppingEvent evt)
    {
        Claim.autoclaimers.clear();
        Claim.claimstarts.clear();
        MinecraftForge.EVENT_BUS.unregister(Claim.class);
    }

    private static int executeCheck(final CommandSource source) throws CommandSyntaxException
    {
        final PlayerEntity player = source.getPlayerOrException();
        final LandTeam team = LandManager.getTeam(player);
        final int count = LandManager.getInstance().countLand(team.teamName);
        final int teamCount = team.member.size();
        final int maxLand = team.maxLand < 0 ? teamCount * Essentials.config.teamLandPerPlayer : team.maxLand;
        player.sendMessage(Essentials.config.getMessage("thutessentials.claim.claimed.count", count, maxLand),
                Util.NIL_UUID);
        return 0;
    }

    private static int executeStart(final CommandSource source) throws CommandSyntaxException
    {
        final PlayerEntity player = source.getPlayerOrException();
        final LandTeam team = LandManager.getTeam(player);
        if (!team.hasRankPerm(player.getUUID(), LandTeam.CLAIMPERM))
        {
            player.sendMessage(Essentials.config.getMessage("thutessentials.claim.notallowed.teamperms"),
                    Util.NIL_UUID);
            return 1;
        }
        final KGobalPos start = CoordinateUtls.forMob(player);
        Claim.claimstarts.put(player.getUUID(), start);
        player.sendMessage(Essentials.config.getMessage("thutessentials.claim.start.set", player.blockPosition()),
                Util.NIL_UUID);
        return 0;
    }

    private static int executeEnd(final CommandSource source) throws CommandSyntaxException
    {
        final PlayerEntity player = source.getPlayerOrException();
        final LandTeam team = LandManager.getTeam(player);
        if (!team.hasRankPerm(player.getUUID(), LandTeam.CLAIMPERM))
        {
            player.sendMessage(Essentials.config.getMessage("thutessentials.claim.notallowed.teamperms"),
                    Util.NIL_UUID);
            return 1;
        }
        final KGobalPos end = CoordinateUtls.forMob(player);
        final KGobalPos start = Claim.claimstarts.get(player.getUUID());
        if (start == null)
        {
            player.sendMessage(Essentials.config.getMessage("thutessentials.claim.start.not_set"), Util.NIL_UUID);
            return 1;
        }
        if (end.getDimension() != start.getDimension())
        {
            player.sendMessage(Essentials.config.getMessage("thutessentials.claim.start.wrong_dim"), Util.NIL_UUID);
            return 1;
        }
        player.getServer().execute(() ->
        {        // easy way to sort the x, z coordinates by min/max
            final AxisAlignedBB box = new AxisAlignedBB(start.getPos(), end.getPos());
            final boolean noLimit = PermissionAPI.hasPermission(player, Claim.BYPASSLIMIT);
            final World dim = player.getCommandSenderWorld();
            int n = 0;
            // Convert to chunk coordinates for the loop with the >> 4
            for (int x = MathHelper.floor(box.minX) >> 4; x <= MathHelper.floor(box.maxX) >> 4; x++)
                for (int z = MathHelper.floor(box.minZ) >> 4; z <= MathHelper.floor(box.maxZ) >> 4; z++)
                    for (int y = 0; y < 16; y++)

                        n += Claim.claim(x, y, z, dim, player, team, false, noLimit);
            player.sendMessage(Essentials.config.getMessage("thutessentials.claim.start.end", n, team.teamName),
                    Util.NIL_UUID);
        });
        return 0;
    }

    private static int executeAuto(final CommandSource source) throws CommandSyntaxException
    {
        final ServerPlayerEntity player = source.getPlayerOrException();
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

    private static int execute(final CommandSource source, final boolean up, final boolean down, final boolean here)
            throws CommandSyntaxException
    {
        final PlayerEntity player = source.getPlayerOrException();
        final LandTeam team = LandManager.getTeam(player);
        if (!team.hasRankPerm(player.getUUID(), LandTeam.CLAIMPERM))
        {
            player.sendMessage(Essentials.config.getMessage("thutessentials.claim.notallowed.teamperms"),
                    Util.NIL_UUID);
            return 1;
        }
        final boolean noLimit = PermissionAPI.hasPermission(player, Claim.BYPASSLIMIT);
        player.getServer().execute(() ->
        {

            final int x = player.blockPosition().getX() >> 4;
            final int y = player.blockPosition().getY() >> 4;
            final int z = player.blockPosition().getZ() >> 4;
            final World dim = player.getCommandSenderWorld();

            if (here)
            {
                Claim.claim(x, y, z, dim, player, team, true, noLimit);
                return;
            }

            final int min = down ? 0 : y;
            final int max = up ? 16 : y;

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
                    player.sendMessage(Essentials.config.getMessage("thutessentials.claim.notallowed.needmoreland"),
                            Util.NIL_UUID);
                    break;
                }
            }
            if (notclaimed > 0) player.sendMessage(Essentials.config.getMessage(
                    "thutessentials.claim.warn.alreadyclaimed", notclaimed), Util.NIL_UUID);
            if (claimed) player.sendMessage(Essentials.config.getMessage("thutessentials.claim.claimed.num", claimnum,
                    team.teamName), Util.NIL_UUID);
            else player.sendMessage(Essentials.config.getMessage("thutessentials.claim.claimed.failed", team.teamName),
                    Util.NIL_UUID);
            if (claimed) LandSaveHandler.saveTeam(team.teamName);
        });
        return 0;
    }

    public static int claim(final int x, final int y, final int z, final World dim, final PlayerEntity player,
            final LandTeam team, final boolean messages, final boolean noLimit)
    {
        return Claim.claim(dim, new BlockPos(x, y, z), player, team, messages, noLimit);
    }

    public static int claim(final World world, final BlockPos chunkCoord, final PlayerEntity player,
            final LandTeam team, final boolean messages, final boolean noLimit)
    {
        final LandTeam owner = LandManager.getInstance().getLandOwner(world, chunkCoord, true);
        if (!LandManager.isWild(owner))
        {
            if (messages) player.sendMessage(Essentials.config.getMessage(
                    "thutessentials.claim.notallowed.alreadyclaimedby", owner.teamName), Util.NIL_UUID);
            return 1;
        }
        final int teamCount = team.member.size();
        final int maxLand = team.maxLand < 0 ? teamCount * Essentials.config.teamLandPerPlayer : team.maxLand;
        final int count = LandManager.getInstance().countLand(team.teamName);
        if (count >= maxLand && !noLimit)
        {
            if (messages) player.sendMessage(Essentials.config.getMessage(
                    "thutessentials.claim.notallowed.needmoreland"), Util.NIL_UUID);
            return 3;
        }
        final KGobalPos pos = KGobalPos.getPosition(world.dimension(), chunkCoord);
        final ClaimLandEvent event = new ClaimLandEvent(pos, player, team.teamName);
        MinecraftForge.EVENT_BUS.post(event);
        LandManager.getInstance().claimLand(team.teamName, world, chunkCoord, true);
        if (messages) player.sendMessage(Essentials.config.getMessage("thutessentials.claim.claimed", team.teamName),
                Util.NIL_UUID);
        return 0;
    }

}
