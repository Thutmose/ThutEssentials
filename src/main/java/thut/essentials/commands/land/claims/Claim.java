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
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
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
import thut.essentials.land.LandManager.LandTeam;
import thut.essentials.land.LandSaveHandler;
import thut.essentials.util.Coordinate;

public class Claim
{
    private static final String BYPASSLIMIT = "thutessentials.land.claim.nolimit";
    private static final String AUTOCLAIM   = "thutessentials.land.claim.autoclaim";

    private static final Set<UUID>           autoclaimers = Sets.newHashSet();
    private static final Map<UUID, BlockPos> claimstarts  = Maps.newHashMap();

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
    }

    @SubscribeEvent
    public static void livingUpdate(final LivingUpdateEvent evt)
    {
        if (!evt.getEntity().isAlive() || !Claim.autoclaimers.contains(evt.getEntity().getUniqueID()) || !(evt
                .getEntityLiving() instanceof ServerPlayerEntity)) return;
        final ServerPlayerEntity player = (ServerPlayerEntity) evt.getEntityLiving();
        final LandTeam team = LandManager.getTeam(player);

        BlockPos here;
        BlockPos old;
        here = new BlockPos(player.chasingPosX, player.chasingPosY, player.chasingPosZ);
        old = new BlockPos(player.prevChasingPosX, player.prevChasingPosY, player.prevChasingPosZ);
        final Coordinate newChunk = Coordinate.getChunkCoordFromWorldCoord(here, player.dimension.getId());
        final Coordinate oldChunk = Coordinate.getChunkCoordFromWorldCoord(old, player.getEntityWorld().getDimension());
        if (newChunk.equals(oldChunk)) return;

        final int x = MathHelper.floor(player.getPosition().getX() >> 4);
        final int z = MathHelper.floor(player.getPosition().getZ() >> 4);
        for (int i = 0; i < 16; i++)
            Claim.claim(x, i, z, player, team, false);
    }

    @SubscribeEvent
    public static void serverUnload(final FMLServerStoppingEvent evt)
    {
        Claim.autoclaimers.clear();
        Claim.claimstarts.clear();
        MinecraftForge.EVENT_BUS.unregister(Claim.class);
    }

    private static int executeAuto(final CommandSource source) throws CommandSyntaxException
    {
        final ServerPlayerEntity player = source.asPlayer();
        if (Claim.autoclaimers.contains(player.getUniqueID()))
        {
            Claim.autoclaimers.remove(player.getUniqueID());
            Essentials.config.sendFeedback(source, "thutessentials.claim.autooff", true);
        }
        else
        {
            Claim.autoclaimers.add(player.getUniqueID());
            Essentials.config.sendFeedback(source, "thutessentials.claim.autoon", true);
        }
        return 0;
    }

    private static int execute(final CommandSource source, final boolean up, final boolean down, final boolean here)
            throws CommandSyntaxException
    {
        final PlayerEntity player = source.asPlayer();
        final LandTeam team = LandManager.getTeam(player);
        if (!team.hasRankPerm(player.getUniqueID(), LandTeam.CLAIMPERM))
        {
            player.sendMessage(CommandManager.makeFormattedComponent("thutessentials.claim.notallowed.teamperms",
                    TextFormatting.RED));
            return 1;
        }

        final int x = MathHelper.floor(player.getPosition().getX() >> 4);
        final int y = MathHelper.floor(player.getPosition().getY() >> 4);
        final int z = MathHelper.floor(player.getPosition().getZ() >> 4);

        if (here) return Claim.claim(x, y, z, player, team, true);

        final int min = down ? 0 : y;
        final int max = up ? 16 : y;

        boolean claimed = false;
        int claimnum = 0;
        int notclaimed = 0;
        for (int i = min; i < max; i++)
        {
            final int check = Claim.claim(x, i, z, player, team, false);
            if (check == 0)
            {
                claimed = true;
                claimnum++;
            }
            else notclaimed++;
            if (check == 3)
            {
                player.sendMessage(CommandManager.makeFormattedComponent("thutessentials.claim.notallowed.needmoreland",
                        TextFormatting.RED));
                break;
            }
        }
        if (notclaimed > 0) player.sendMessage(new TranslationTextComponent("thutessentials.claim.warn.alreadyclaimed",
                notclaimed));
        if (claimed) player.sendMessage(new TranslationTextComponent("thutessentials.claim.claimed.num", claimnum,
                team.teamName));
        else player.sendMessage(new TranslationTextComponent("thutessentials.claim.claimed.failed", team.teamName));

        LandSaveHandler.saveTeam(team.teamName);
        return claimed ? 0 : 1;
    }

    private static int claim(final int x, final int y, final int z, final PlayerEntity player, final LandTeam team,
            final boolean messages)
    {
        // TODO better bounds check to support say cubic chunks.
        if (y < 0 || y > 15) return 1;
        final int dim = player.dimension.getId();
        final Coordinate chunk = new Coordinate(x, y, z, dim);
        final LandTeam owner = LandManager.getInstance().getLandOwner(chunk);
        if (owner != null)
        {
            if (messages) player.sendMessage(new TranslationTextComponent(
                    "thutessentials.claim.notallowed.alreadyclaimedby", owner.teamName));
            return 2;
        }
        final int teamCount = team.member.size();
        final int maxLand = team.maxLand < 0 ? teamCount * Essentials.config.teamLandPerPlayer : team.maxLand;
        final int count = LandManager.getInstance().countLand(team.teamName);
        if (count >= maxLand && !PermissionAPI.hasPermission(player, Claim.BYPASSLIMIT))
        {
            if (messages) player.sendMessage(CommandManager.makeFormattedComponent(
                    "thutessentials.claim.notallowed.needmoreland", TextFormatting.RED));
            return 3;
        }
        final ClaimLandEvent event = new ClaimLandEvent(new BlockPos(x, y, z), dim, player, team.teamName);
        MinecraftForge.EVENT_BUS.post(event);
        LandManager.getInstance().addTeamLand(team.teamName, chunk, true);
        if (messages) player.sendMessage(new TranslationTextComponent("thutessentials.claim.claimed", team.teamName));
        return 0;
    }

}
