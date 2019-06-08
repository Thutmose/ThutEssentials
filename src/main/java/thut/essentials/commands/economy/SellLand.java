package thut.essentials.commands.economy;

import java.util.UUID;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.util.text.event.ClickEvent.Action;
import net.minecraftforge.server.permission.DefaultPermissionLevel;
import net.minecraftforge.server.permission.PermissionAPI;
import thut.essentials.economy.EconomyManager;
import thut.essentials.land.LandManager;
import thut.essentials.land.LandManager.LandTeam;
import thut.essentials.util.BaseCommand;
import thut.essentials.util.ConfigManager;
import thut.essentials.util.Coordinate;
import thut.essentials.util.PlayerDataHandler;

public class SellLand extends BaseCommand
{
    private static final String PERMSELLOTHERLAND = "thutessentials.economy.sell_land.other";

    public SellLand()
    {
        super("sell_team_land", 0);
        PermissionAPI.registerNode(PERMSELLOTHERLAND, DefaultPermissionLevel.OP,
                "Allowed to sell any land, regardless of owner");
    }

    @Override
    public String getUsage(ICommandSource sender)
    {
        return super.getUsage(sender) + " <player> <cost> <optional|4-coordinates x y z w> or " + super.getUsage(sender)
                + " !clear to clear your current sale offer.";
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
        if (!ConfigManager.INSTANCE.landEnabled) throw new CommandException("Land is not enabled on this server.");
        PlayerEntity player = getPlayerBySender(sender);
        // We use this to actually confirm the sale.
        if (args.length == 1)
        {
            // clear any sale offer, if not sale offer, just return.
            if (args[0].equals("!clear"))
            {
                if (PlayerDataHandler.getCustomDataTag(player).hasKey("land_sale"))
                {
                    PlayerDataHandler.getCustomDataTag(player).remove("land_sale");
                    PlayerDataHandler.saveCustomData(player);
                    player.sendMessage(new StringTextComponent(TextFormatting.RED + "Cleared land sale offer!"));
                }
                return;
            }
            // Confirm the sale offer.
            if (args[0].equals("!confim"))
            {
                LandTeam buyTeam = LandManager.getTeam(player);
                if (buyTeam == LandManager.getDefaultTeam())
                    throw new CommandException("You cannot buy land for the default team");
                if (buyTeam.land.land.size() >= ConfigManager.INSTANCE.maxLandViaSalesPerTeam)
                    throw new CommandException("Your team may not buy any more land.");
                CompoundNBT tag = PlayerDataHandler.getCustomDataTag(player).getCompound("land_sale");
                // check if we have any sales
                if (tag.hasNoTags()) throw new CommandException("You do not have any sale offers.");
                int cost = tag.getInteger("c");
                // Check if we have enough money for the sale
                int balance = EconomyManager.getBalance(player);
                if (balance < cost) throw new CommandException("Insufficient Funds, cost is " + cost);
                Coordinate coord = new Coordinate(tag.getInteger("x"), tag.getInteger("y"), tag.getInteger("z"),
                        tag.getInteger("w"));

                UUID seller = UUID.fromString(tag.getString("id"));

                EconomyManager.addBalance(seller, cost);
                EconomyManager.addBalance(player, -cost);

                ServerPlayerEntity sellPlayer = server.getPlayerList().getPlayerByUUID(seller);
                if (sellPlayer != null)
                {
                    sellPlayer.sendMessage(
                            new StringTextComponent(TextFormatting.GREEN + "Land Sale Complete, you received " + cost));
                }
                player.sendMessage(
                        new StringTextComponent(TextFormatting.GREEN + "Land Sale Complete, you spent " + cost));

                // Transfer the team land, this automatically removes old owner
                // of the land.
                LandManager.getInstance().addTeamLand(buyTeam.teamName, coord, true);
                // Remove sale tag from player.
                PlayerDataHandler.getCustomDataTag(player).remove("land_sale");
                PlayerDataHandler.saveCustomData(player);
                return;
            }
        }
        if (args.length < 1) throw new CommandException(getUsage(sender));
        PlayerEntity target = getPlayer(server, sender, args[0]);
        Coordinate loc = null;
        LandTeam sellTeam = LandManager.getTeam(player);
        LandTeam buyTeam = LandManager.getTeam(target);
        if (buyTeam == LandManager.getDefaultTeam()) throw new CommandException("You cannot sell land to default team");
        if (PlayerDataHandler.getCustomDataTag(target).hasKey("land_sale")) throw new CommandException(
                target.getName() + " already has an outstanding sell offer, they must complete or clear that first.");

        int cost = parseInt(args[1]);

        if (args.length == 6 && PermissionAPI.hasPermission(player, PERMSELLOTHERLAND))
        {
            BlockPos pos = parseBlockPos(sender, args, 2, true);
            loc = Coordinate.getChunkCoordFromWorldCoord(pos, parseInt(args[5]));
        }
        else if (args.length == 2)
        {
            if (sellTeam == buyTeam) throw new CommandException("You cannot sell land to your own team");
            if (sellTeam == LandManager.getDefaultTeam())
                throw new CommandException("You cannot sell land for default team");
            if (!sellTeam.isAdmin(player)) throw new CommandException("Only team admins may sell land");
            loc = Coordinate.getChunkCoordFromWorldCoord(player.getPosition(), player.dimension);
            if (!LandManager.getInstance().isTeamLand(loc, sellTeam.teamName))
                throw new CommandException("You cannot sell land your team does not own.");
        }
        if (loc == null) throw new CommandException(getUsage(sender));

        // Set the nbt tag that the player has offers.
        CompoundNBT tag = new CompoundNBT();
        tag.setInteger("c", cost);
        tag.setInteger("x", loc.x);
        tag.setInteger("y", loc.y);
        tag.setInteger("z", loc.z);
        tag.setInteger("w", loc.dim);
        tag.putString("id", player.getCachedUniqueIdString());
        PlayerDataHandler.getCustomDataTag(target).setTag("land_sale", tag);
        PlayerDataHandler.saveCustomData(target);

        // Make a message to send to the target.
        ITextComponent message = new StringTextComponent("==============================================\n")
                .appendSibling(player.getDisplayName())
                .appendText(TextFormatting.AQUA + " Wishes to sell you some land!\n\n");
        message.appendText(TextFormatting.AQUA + "It is sub chunk " + TextFormatting.GOLD + loc.x + " " + loc.y + " "
                + loc.z + "\n" + TextFormatting.AQUA + "Located in dimension " + TextFormatting.GOLD + loc.dim
                + "\n\n");
        message.appendText(TextFormatting.AQUA + "The offered price is " + TextFormatting.GOLD + cost + "\n\n");

        ITextComponent accept = new StringTextComponent(TextFormatting.GREEN + "Accept");
        accept.setStyle(new Style());
        accept.getStyle().setClickEvent(new ClickEvent(Action.RUN_COMMAND, "/" + getName() + " !confim"));

        ITextComponent deny = new StringTextComponent(TextFormatting.RED + "Deny");
        deny.setStyle(new Style());
        deny.getStyle().setClickEvent(new ClickEvent(Action.RUN_COMMAND, "/" + getName() + " !clear"));

        message.appendSibling(accept).appendText(" / ").appendSibling(deny)
                .appendText(("\n=============================================="));
        target.sendMessage(message);

        message = new StringTextComponent("==============================================\n")
                .appendText(TextFormatting.AQUA + "Sell offer sent to ").appendSibling(player.getDisplayName());
        message.appendText("\n\n" + TextFormatting.AQUA + "It is sub chunk " + TextFormatting.GOLD + loc.x + " " + loc.y
                + " " + loc.z + "\n" + TextFormatting.AQUA + "Located in dimension " + TextFormatting.GOLD + loc.dim)
                .appendText(("\n=============================================="));
        sender.sendMessage(message);
    }

}
